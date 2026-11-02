package io.github.jairosousa.jns.ai.sample.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
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

/**
 * Testa o ChatService sem rede e sem custo: o LLM é substituído por um ChatModel falso
 * que devolve um texto fixo. O ChatClient real (com toda a lógica do Spring AI) é usado.
 */
class ChatServiceTest {

    /** LLM falso: devolve sempre o mesmo texto e guarda o último prompt recebido. */
    static class ChatModelFalso implements ChatModel {
        private final String resposta;
        Prompt ultimoPrompt;

        ChatModelFalso(String resposta) {
            this.resposta = resposta;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.ultimoPrompt = prompt;
            return chatResponse(resposta);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            this.ultimoPrompt = prompt;
            // simula o streaming: uma palavra por vez
            return Flux.fromArray(resposta.split("(?<= )")).map(ChatModelFalso::chatResponse);
        }

        private static ChatResponse chatResponse(String texto) {
            var metadata = ChatResponseMetadata.builder()
                    .model("modelo-falso")
                    .usage(new DefaultUsage(100, 20))
                    .build();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))), metadata);
        }
    }

    private ChatService service(ChatModelFalso modelo) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new ChatService(ChatClient.builder(modelo).build(), "falso", validator);
    }

    @Test
    void perguntarDevolveTextoTokensEProvedor() {
        var resposta = service(new ChatModelFalso("Use record para DTOs.")).perguntar("record?");

        assertThat(resposta.resposta()).isEqualTo("Use record para DTOs.");
        assertThat(resposta.modelo()).isEqualTo("modelo-falso");
        assertThat(resposta.provedor()).isEqualTo("falso");
        assertThat(resposta.tokensEntrada()).isEqualTo(100);
        assertThat(resposta.tokensSaida()).isEqualTo(20);
    }

    @Test
    void revisarCodigoConverteJsonDoLlmEmRecord() {
        var modelo = new ChatModelFalso("""
                {"nota": 7, "problemas": ["sem validação"], "sugestoes": ["usar @Valid"]}
                """);

        RevisaoCodigo revisao = service(modelo).revisarCodigo("class A {}");

        assertThat(revisao.nota()).isEqualTo(7);
        assertThat(revisao.problemas()).containsExactly("sem validação");
        assertThat(revisao.sugestoes()).containsExactly("usar @Valid");
        // o .entity() acrescentou ao prompt as instruções de formato JSON
        assertThat(modelo.ultimoPrompt.getContents()).contains("class A {}").containsIgnoringCase("json");
    }

    @Test
    void revisarCodigoRejeitaNotaForaDoIntervalo() {
        var modelo = new ChatModelFalso("""
                {"nota": 15, "problemas": [], "sugestoes": []}
                """);

        assertThatThrownBy(() -> service(modelo).revisarCodigo("class A {}"))
                .isInstanceOf(RespostaInvalidaDoLlmException.class)
                .hasMessageContaining("nota");
    }

    @Test
    void streamingDevolveARespostaEmPedacos() {
        List<String> pedacos = service(new ChatModelFalso("Use record para DTOs."))
                .perguntarEmStreaming("record?")
                .collectList()
                .block();

        assertThat(pedacos).containsExactly("Use ", "record ", "para ", "DTOs.");
    }
}
