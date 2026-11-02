package io.github.jairosousa.jns.ai.sample.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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

    @PostMapping("/revisao")
    public RevisaoCodigo revisao(@Valid @RequestBody RevisaoRequest request) {
        return chatService.revisarCodigo(request.codigo());
    }

    /**
     * Server-Sent Events: o Spring MVC envia cada pedaço do Flux assim que ele chega do LLM,
     * em vez de esperar a resposta inteira.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String pergunta) {
        return chatService.perguntarEmStreaming(pergunta);
    }

    public record ChatPergunta(String pergunta) {
    }

    public record RevisaoRequest(@NotBlank String codigo) {
    }
}
