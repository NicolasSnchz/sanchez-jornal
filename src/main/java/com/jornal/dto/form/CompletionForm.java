package com.jornal.dto.form;

import com.jornal.entity.Assignment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Cierre de una asignacion: el maestro califica al trabajador. */
@Schema(description = "Cierre de una asignaci\u00f3n con la calificaci\u00f3n del trabajador")
public class CompletionForm {

    @Schema(description = "Calificaci\u00f3n de 1 a 5", example = "5")
    @Min(Assignment.MIN_RATING)
    @Max(Assignment.MAX_RATING)
    private int rating = Assignment.MAX_RATING;

    @Schema(description = "Comentario (opcional)", example = "Puntual y ordenado")
    @Size(max = 300)
    private String comment;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
