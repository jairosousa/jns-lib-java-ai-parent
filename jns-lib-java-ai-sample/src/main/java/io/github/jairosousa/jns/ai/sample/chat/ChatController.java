package io.github.jairosousa.jns.ai.sample.chat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResposta perguntar(@RequestBody ChatPergunta pergunta) {
        return chatService.perguntar(pergunta.pergunta());
    }

    public record ChatPergunta(String pergunta) {
    }
}
