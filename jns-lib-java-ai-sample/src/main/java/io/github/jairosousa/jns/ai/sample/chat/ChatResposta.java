package io.github.jairosousa.jns.ai.sample.chat;

public record ChatResposta(
        String provedor,
        String modelo,
        String resposta,
        Integer tokensEntrada,
        Integer tokensSaida,
        long latenciaMs) {
}
