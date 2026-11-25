package io.github.jairosousa.jns.ai.sample.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
     * Revisão em streaming, com eventos SSE nomeados:
     * <ul>
     *   <li>{@code event: parcial}: pedaços do JSON enquanto o LLM gera (para mostrar progresso)</li>
     *   <li>{@code event: resultado}: a RevisaoCodigo completa e validada, uma vez, no final</li>
     *   <li>{@code event: erro}: o JSON final era inválido. O status HTTP já foi enviado (200) no
     *       primeiro pedaço, então o erro precisa ir como evento</li>
     * </ul>
     */
    @PostMapping(path = "/revisao/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> revisaoEmStreaming(@Valid @RequestBody RevisaoRequest request) {
        return chatService.revisarCodigoEmStreaming(request.codigo())
                .map(evento -> switch (evento) {
                    case EventoRevisao.Parcial(String texto) -> sse("parcial", texto);
                    case EventoRevisao.Concluida(RevisaoCodigo revisao) -> sse("resultado", revisao);
                })
                .onErrorResume(RespostaInvalidaDoLlmException.class, e -> Flux.just(sse("erro", e.getMessage())));
    }

    private static ServerSentEvent<Object> sse(String evento, Object dados) {
        return ServerSentEvent.builder(dados).event(evento).build();
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
