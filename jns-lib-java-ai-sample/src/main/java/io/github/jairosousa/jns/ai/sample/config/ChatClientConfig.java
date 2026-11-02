package io.github.jairosousa.jns.ai.sample.config;

import io.github.jairosousa.jns.ai.core.advisor.TokenUsageAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    static final String SYSTEM_PROMPT = """
            Você é um assistente para desenvolvedores Java e Spring Boot.
            Responda sempre em português do Brasil, de forma objetiva e técnica.
            """;

    /**
     * O ChatClient.Builder é criado pelo Spring AI a partir do único ChatModel ativo
     * (definido pela propriedade spring.ai.model.chat de cada perfil). O código não sabe
     * qual provedor está por trás.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new TokenUsageAdvisor())   // loga modelo, tokens e latência de toda chamada
                .build();
    }
}
