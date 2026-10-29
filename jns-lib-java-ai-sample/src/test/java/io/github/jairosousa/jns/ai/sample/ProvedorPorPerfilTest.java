package io.github.jairosousa.jns.ai.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Garante que cada perfil ativa exatamente um ChatModel — sem chamar nenhuma API.
 * Chaves falsas bastam porque o cliente só é usado na primeira requisição.
 */
class ProvedorPorPerfilTest {

    @Nested
    @SpringBootTest(properties = "spring.ai.anthropic.api-key=chave-falsa")
    @ActiveProfiles("anthropic")
    class Anthropic {
        @Autowired ApplicationContext context;

        @Test
        void usaAnthropicChatModel() {
            assertThat(context.getBeansOfType(ChatModel.class).values())
                    .singleElement().isInstanceOf(AnthropicChatModel.class);
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.ai.openai.api-key=chave-falsa")
    @ActiveProfiles("openai")
    class OpenAi {
        @Autowired ApplicationContext context;

        @Test
        void usaOpenAiChatModel() {
            assertThat(context.getBeansOfType(ChatModel.class).values())
                    .singleElement().isInstanceOf(OpenAiChatModel.class);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("local")
    class Local {
        @Autowired ApplicationContext context;

        @Test
        void usaOllamaChatModel() {
            assertThat(context.getBeansOfType(ChatModel.class).values())
                    .singleElement().isInstanceOf(OllamaChatModel.class);
        }
    }
}
