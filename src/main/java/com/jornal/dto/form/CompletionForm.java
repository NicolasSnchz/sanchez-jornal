package com.jornal.dto.form;

import com.jornal.entity.Assignment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Cierre de una asignacion: el maestro califica al trabajador. */
public class CompletionForm {

    @Min(Assignment.MIN_RATING)
    @Max(Assignment.MAX_RATING)
    private int rating = Assignment.MAX_RATING;

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
