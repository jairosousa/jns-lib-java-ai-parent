package io.github.jairosousa.jns.ai.core.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

class TokenUsageAdvisorTest {

    /** LLM falso: responde sempre o mesmo texto e informa 10 tokens de entrada e 5 de saída. */
    static class ChatModelFalso implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return resposta("Olá!", new DefaultUsage(10, 5));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            // Como nos provedores reais: os tokens chegam só no último pedaço
            return Flux.just(
                    resposta("Ol", new DefaultUsage(0, 0)),
                    resposta("á!", new DefaultUsage(10, 5)));
        }

        private static ChatResponse resposta(String texto, DefaultUsage usage) {
            var metadata = ChatResponseMetadata.builder().model("modelo-falso").usage(usage).build();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))), metadata);
        }
    }

    private final List<UsoDeTokens> registros = new ArrayList<>();
    private final ChatClient chatClient = ChatClient.builder(new ChatModelFalso())
            .defaultAdvisors(new TokenUsageAdvisor(registros::add))
            .build();

    @Test
    void registraTokensDeChamadaSincrona() {
        String texto = chatClient.prompt().user("oi").call().content();

        assertThat(texto).isEqualTo("Olá!");
        assertThat(registros).singleElement().satisfies(uso -> {
            assertThat(uso.modelo()).isEqualTo("modelo-falso");
            assertThat(uso.tokensEntrada()).isEqualTo(10);
            assertThat(uso.tokensSaida()).isEqualTo(5);
            assertThat(uso.streaming()).isFalse();
        });
    }

    @Test
    void registraTokensUmaVezAoFinalDoStreaming() {
        List<String> pedacos = chatClient.prompt().user("oi").stream().content().collectList().block();

        assertThat(pedacos).containsExactly("Ol", "á!");
        assertThat(registros).singleElement().satisfies(uso -> {
            assertThat(uso.tokensEntrada()).isEqualTo(10);
            assertThat(uso.tokensSaida()).isEqualTo(5);
            assertThat(uso.streaming()).isTrue();
        });
    }

    @Test
    void semTabelaDePrecosOCustoFicaNulo() {
        chatClient.prompt().user("oi").call().content();

        assertThat(registros).singleElement().extracting(UsoDeTokens::custoUsd).isNull();
    }

    @Test
    void calculaCustoComATabelaDePrecos() {
        var comPreco = ChatClient.builder(new ChatModelFalso())
                .defaultAdvisors(new TokenUsageAdvisor(Map.of("modelo-falso", new PrecoModelo(4.00, 20.00)), registros::add))
                .build();

        comPreco.prompt().user("oi").call().content();

        // (10 × 4,00 + 5 × 20,00) / 1.000.000 = 0,00014
        assertThat(registros).singleElement().extracting(UsoDeTokens::custoUsd)
                .asInstanceOf(InstanceOfAssertFactories.DOUBLE).isCloseTo(0.00014, within(1e-9));
    }

    @Test
    void escolheOPrecoDoPrefixoMaisLongoParaModelosDatados() {
        var advisor = new TokenUsageAdvisor(Map.of(
                "gpt-5", new PrecoModelo(1.25, 10.00),
                "gpt-5.5", new PrecoModelo(5.00, 30.00)));

        // a API devolve o nome datado; "gpt-5.5" é o prefixo mais específico
        assertThat(advisor.calcularCusto("gpt-5.5-2026-04-23", 1_000_000, 0)).isEqualTo(5.00);
        assertThat(advisor.calcularCusto("gpt-5-mini", 1_000_000, 0)).isEqualTo(1.25);
        assertThat(advisor.calcularCusto("claude-opus-5-5", 1_000_000, 0)).isNull();
    }
}
