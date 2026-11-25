package io.github.jairosousa.jns.ai.core.advisor;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
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
 * Advisor que mede modelo, tokens, latência e custo de toda chamada feita pelo {@code ChatClient},
 * síncrona ou por streaming.
 *
 * <p>Um advisor funciona como um filtro de servlet: recebe a requisição, chama o próximo elo da
 * cadeia ({@code chain.nextCall}) e recebe a resposta na volta. O último elo é o próprio LLM.
 *
 * <pre>{@code
 * ChatClient.builder(chatModel)
 *         .defaultAdvisors(new TokenUsageAdvisor(Map.of("claude-opus-5-5", new PrecoModelo(4.00, 20.00))))
 *         .build();
 * }</pre>
 *
 * <p>Por padrão o consumo vai para o log. Para enviar a outro destino (métricas, banco), passe um
 * {@link Consumer} no construtor.
 */
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    private final Map<String, PrecoModelo> precos;
    private final Consumer<UsoDeTokens> destino;

    public TokenUsageAdvisor() {
        this(Map.of());
    }

    public TokenUsageAdvisor(Consumer<UsoDeTokens> destino) {
        this(Map.of(), destino);
    }

    public TokenUsageAdvisor(Map<String, PrecoModelo> precos) {
        this(precos, TokenUsageAdvisor::registrarNoLog);
    }

    /**
     * @param precos  preço por modelo. A chave pode ser o nome "curto" do modelo: {@code gpt-5.5}
     *                também vale para a versão datada {@code gpt-5.5-2026-04-23} devolvida pela API.
     * @param destino para onde vai cada {@link UsoDeTokens}
     */
    public TokenUsageAdvisor(Map<String, PrecoModelo> precos, Consumer<UsoDeTokens> destino) {
        this.precos = Map.copyOf(Objects.requireNonNull(precos, "precos"));
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
                streaming,
                calcularCusto(metadata.getModel(), usage.getPromptTokens(), usage.getCompletionTokens())));
    }

    /** Usa o preço cuja chave é o prefixo mais longo do nome do modelo; null se não houver preço. */
    Double calcularCusto(String modelo, Integer tokensEntrada, Integer tokensSaida) {
        if (modelo == null) {
            return null;
        }
        return precos.entrySet().stream()
                .filter(preco -> modelo.startsWith(preco.getKey()))
                .max(Comparator.comparingInt(preco -> preco.getKey().length()))
                .map(preco -> preco.getValue().custo(valor(tokensEntrada), valor(tokensSaida)))
                .orElse(null);
    }

    private static int valor(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }

    private static void registrarNoLog(UsoDeTokens uso) {
        String custo = uso.custoUsd() == null ? "n/d" : String.format(Locale.US, "US$ %.4f", uso.custoUsd());
        log.info("LLM modelo={} entrada={} saida={} latenciaMs={} streaming={} custo={}",
                uso.modelo(), uso.tokensEntrada(), uso.tokensSaida(), uso.latenciaMs(), uso.streaming(), custo);
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
