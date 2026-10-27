package com.jaldishop.backend.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "La calificación es obligatoria.")
        @Min(value = 1, message = "La calificación mínima es 1 estrella.")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas.")
        Integer rating,

        @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres.")
        String comment
) {}
