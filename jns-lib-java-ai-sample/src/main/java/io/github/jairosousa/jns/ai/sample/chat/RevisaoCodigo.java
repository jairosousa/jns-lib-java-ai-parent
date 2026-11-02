package io.github.jairosousa.jns.ai.sample.chat;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @Autor Jairo Nascimento
 * @Created 02/11/2026 - 11:20
 */
public record RevisaoCodigo(
        @Min(0) @Max(10) int nota,
        @NotNull List<String> problemas,
        @NotNull List<String> sugestoes) {
}
