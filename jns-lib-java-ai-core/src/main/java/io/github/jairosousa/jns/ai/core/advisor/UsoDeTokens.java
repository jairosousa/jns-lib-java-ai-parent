package io.github.jairosousa.jns.ai.core.advisor;

/**
 * Consumo de uma chamada ao LLM, registrado pelo {@link TokenUsageAdvisor}.
 *
 * @param modelo        modelo que respondeu (ex.: claude-opus-5-5)
 * @param tokensEntrada tokens enviados (system + histórico + pergunta)
 * @param tokensSaida   tokens gerados (inclui raciocínio, quando houver)
 * @param latenciaMs    tempo total da chamada em milissegundos
 * @param streaming     true quando a resposta veio por streaming
 */
public record UsoDeTokens(String modelo, Integer tokensEntrada, Integer tokensSaida, long latenciaMs, boolean streaming) {
}
