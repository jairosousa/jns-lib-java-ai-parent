package io.github.jairosousa.jns.ai.core.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

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
}
