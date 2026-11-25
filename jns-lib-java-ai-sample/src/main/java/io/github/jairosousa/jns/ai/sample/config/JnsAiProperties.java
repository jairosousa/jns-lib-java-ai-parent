package io.github.jairosousa.jns.ai.sample.config;

import io.github.jairosousa.jns.ai.core.advisor.PrecoModelo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Propriedades {@code jns.ai.*} do application.yml.
 *
 * @param precos preço por modelo (US$ por 1M de tokens), usado pelo TokenUsageAdvisor para calcular o custo
 */
@ConfigurationProperties(prefix = "jns.ai")
public record JnsAiProperties(Map<String, PrecoModelo> precos) {

    public JnsAiProperties {
        precos = precos == null ? Map.of() : Map.copyOf(precos);
    }
}
