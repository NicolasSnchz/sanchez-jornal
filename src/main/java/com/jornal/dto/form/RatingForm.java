package com.jornal.dto.form;

import com.jornal.entity.Assignment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Calificacion que el trabajador da al maestro. */
public class RatingForm {

    @Min(Assignment.MIN_RATING)
    @Max(Assignment.MAX_RATING)
    private int rating;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
