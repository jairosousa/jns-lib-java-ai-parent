package io.github.jairosousa.jns.ai.sample.chat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    /*
     * Só descrevemos O QUE queremos em cada campo. O FORMATO (JSON com nota, problemas e sugestoes)
     * é acrescentado automaticamente pelo .entity(RevisaoCodigo.class), a partir do record.
     */
    private static final String PROMPT_REVISAO = """
            Revise o código Java abaixo como um desenvolvedor sênior.
            - nota: inteiro de 0 a 10 para a qualidade geral do código
            - problemas: bugs, riscos e más práticas encontrados (lista vazia se não houver)
            - sugestoes: melhorias concretas e acionáveis (lista vazia se não houver)

            <codigo>
            %s
            </codigo>
            """;

    private final ChatClient chatClient;
    private final String provedor;
    private final Validator validator;

    public ChatService(ChatClient chatClient, @Value("${spring.ai.model.chat}") String provedor, Validator validator) {
        this.chatClient = chatClient;
        this.provedor = provedor;
        this.validator = validator;
    }

    public ChatResposta perguntar(String pergunta) {
        long inicio = System.nanoTime();
        ChatResponse response = chatClient.prompt()
                .user(pergunta)
                .call()
                .chatResponse();
        long latenciaMs = (System.nanoTime() - inicio) / 1_000_000;

        var usage = response.getMetadata().getUsage();
        return new ChatResposta(
                provedor,
                response.getMetadata().getModel(),
                response.getResult().getOutput().getText(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                latenciaMs);
    }

    public RevisaoCodigo revisarCodigo(String codigo) {
        RevisaoCodigo revisao;
        try {
            revisao = chatClient.prompt()
                    .user(PROMPT_REVISAO.formatted(codigo))
                    .call()
                    .entity(RevisaoCodigo.class);
        } catch (JacksonException e) {
            // O LLM devolveu algo que não é o JSON esperado (cortado, texto livre...). No Jackson 3
            // essa exceção é unchecked: sem este catch ela escaparia como HTTP 500.
            throw new RespostaInvalidaDoLlmException("O LLM não devolveu um JSON válido: " + e.getOriginalMessage(), e);
        }
        return validar(revisao);
    }

    /**
     * Revisão em streaming. O .entity() não funciona com .stream(): ele precisa do JSON inteiro para
     * converter. Então fazemos à mão o que o .entity() faz: acrescentamos as instruções de formato,
     * repassamos cada pedaço ao cliente e convertemos só quando o stream termina.
     */
    public Flux<EventoRevisao> revisarCodigoEmStreaming(String codigo) {
        var converter = new BeanOutputConverter<>(RevisaoCodigo.class);
        // defer: um StringBuilder novo para cada requisição (o Flux só roda quando alguém assina)
        return Flux.defer(() -> {
            var jsonCompleto = new StringBuilder();

            Flux<EventoRevisao> parciais = chatClient.prompt()
                    .user(PROMPT_REVISAO.formatted(codigo) + "\n" + converter.getFormat())
                    .stream()
                    .content()
                    .doOnNext(jsonCompleto::append)
                    .map(EventoRevisao.Parcial::new);

            Mono<EventoRevisao> concluida = Mono.fromCallable(
                    () -> new EventoRevisao.Concluida(converterEValidar(converter, jsonCompleto.toString())));

            return parciais.concatWith(concluida);   // concluida só roda depois do último pedaço
        });
    }

    private RevisaoCodigo converterEValidar(BeanOutputConverter<RevisaoCodigo> converter, String json) {
        try {
            return validar(converter.convert(json));
        } catch (JacksonException e) {
            throw new RespostaInvalidaDoLlmException("O LLM não devolveu um JSON válido: " + e.getOriginalMessage(), e);
        }
    }

    /** O LLM não é confiável como um método Java: validamos a SAÍDA dele antes de devolver. */
    private RevisaoCodigo validar(RevisaoCodigo revisao) {
        Set<ConstraintViolation<RevisaoCodigo>> violacoes = validator.validate(revisao);
        if (!violacoes.isEmpty()) {
            String detalhes = violacoes.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new RespostaInvalidaDoLlmException("Revisão inválida retornada pelo LLM: " + detalhes);
        }
        return revisao;
    }

    /** Devolve a resposta em pedaços (tokens) à medida que o LLM gera. */
    public Flux<String> perguntarEmStreaming(String pergunta) {
        return chatClient.prompt()
                .user(pergunta)
                .stream()
                .content();
    }
}
