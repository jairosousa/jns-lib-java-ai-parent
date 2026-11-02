package io.github.jairosousa.jns.ai.core.advisor;

import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

import reactor.core.publisher.Flux;

/**
 * Advisor que mede modelo, tokens e latência de toda chamada feita pelo {@code ChatClient},
 * síncrona ou por streaming.
 *
 * <p>Um advisor funciona como um filtro de servlet: recebe a requisição, chama o próximo elo da
 * cadeia ({@code chain.nextCall}) e recebe a resposta na volta. O último elo é o próprio LLM.
 *
 * <pre>{@code
 * ChatClient.builder(chatModel)
 *         .defaultAdvisors(new TokenUsageAdvisor())
 *         .build();
 * }</pre>
 *
 * <p>Por padrão o consumo vai para o log. Para enviar a outro destino (métricas, banco), passe um
 * {@link Consumer} no construtor.
 */
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    private final Consumer<UsoDeTokens> destino;

    public TokenUsageAdvisor() {
        this(TokenUsageAdvisor::registrarNoLog);
    }

    public TokenUsageAdvisor(Consumer<UsoDeTokens> destino) {
        this.destino = Objects.requireNonNull(destino, "destino");
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long inicio = System.nanoTime();
        ChatClientResponse response = chain.nextCall(request);   // ← aqui o LLM é chamado
        publicar(response, inicio, false);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long inicio = System.nanoTime();
        // No streaming a resposta chega em pedaços; o agregador junta tudo e avisa no final
        return new ChatClientMessageAggregator().aggregateChatClientResponse(
                chain.nextStream(request),
                completa -> publicar(completa, inicio, true));
    }

    private void publicar(ChatClientResponse response, long inicio, boolean streaming) {
        long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return;
        }
        var metadata = chatResponse.getMetadata();
        var usage = metadata.getUsage();
        destino.accept(new UsoDeTokens(
                metadata.getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                latenciaMs,
                streaming));
    }

    private static void registrarNoLog(UsoDeTokens uso) {
        log.info("LLM modelo={} entrada={} saida={} latenciaMs={} streaming={}",
                uso.modelo(), uso.tokensEntrada(), uso.tokensSaida(), uso.latenciaMs(), uso.streaming());
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    /** Fica perto do LLM (ordem alta) para medir só o tempo da chamada ao modelo. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1000;
    }
}
