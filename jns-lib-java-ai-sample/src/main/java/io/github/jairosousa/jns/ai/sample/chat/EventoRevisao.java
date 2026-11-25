package io.github.jairosousa.jns.ai.sample.chat;

/**
 * Eventos da revisão em streaming: vários {@link Parcial} enquanto o LLM gera o JSON e um
 * {@link Concluida} no final, com o JSON já convertido e validado.
 *
 * <p>{@code sealed}: só estes dois tipos existem, e o {@code switch} do controller é verificado pelo compilador.
 */
public sealed interface EventoRevisao {

    /** Um pedaço do JSON, exatamente como chegou do LLM (ainda incompleto, não dá para converter). */
    record Parcial(String texto) implements EventoRevisao {
    }

    /** O JSON completo, convertido em record e validado. */
    record Concluida(RevisaoCodigo revisao) implements EventoRevisao {
    }
}
