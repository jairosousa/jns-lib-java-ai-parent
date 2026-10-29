package io.github.jairosousa.jns.ai.sample.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final String provedor;

    public ChatService(ChatClient chatClient, @Value("${spring.ai.model.chat}") String provedor) {
        this.chatClient = chatClient;
        this.provedor = provedor;
    }

    public ChatResposta perguntar(String pergunta) {
        long inicio = System.nanoTime();
        ChatResponse response = chatClient.prompt()
                .user(pergunta)
                .call()
                .chatResponse();
        long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;

        var usage = response.getMetadata().getUsage();
        return new ChatResposta(
                provedor,
                response.getMetadata().getModel(),
                response.getResult().getOutput().getText(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                latenciaMs);
    }
}
