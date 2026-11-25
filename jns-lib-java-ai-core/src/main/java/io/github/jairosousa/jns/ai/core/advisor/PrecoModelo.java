package io.github.jairosousa.jns.ai.core.advisor;

/**
 * Preço de um modelo em US$ por 1 milhão de tokens, como publicado pelos provedores.
 *
 * @param entrada preço por 1M de tokens de entrada
 * @param saida   preço por 1M de tokens de saída
 */
public record PrecoModelo(double entrada, double saida) {

    /** custo = (entrada × preço de entrada + saída × preço de saída) ÷ 1.000.000 */
    public double custo(int tokensEntrada, int tokensSaida) {
        return (tokensEntrada * entrada + tokensSaida * saida) / 1_000_000;
    }
}
