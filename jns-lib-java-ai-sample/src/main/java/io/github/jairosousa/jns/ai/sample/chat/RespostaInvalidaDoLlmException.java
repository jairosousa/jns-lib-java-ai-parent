package io.github.jairosousa.jns.ai.sample.chat;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** O LLM respondeu, mas a resposta não respeita o contrato esperado (ex.: nota 15). */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RespostaInvalidaDoLlmException extends RuntimeException {

    public RespostaInvalidaDoLlmException(String mensagem) {
        super(mensagem);
    }
}
