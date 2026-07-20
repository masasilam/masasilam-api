package com.masasilam.app.model.dto.response;

import lombok.Data;

@Data
public class GenreBreakdownItem {
    private String  genreName;
    private Integer booksRead;
    private Integer minutesSpent;
    private Double  percentage;
    private Double  averageRating;
}