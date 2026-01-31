package com.naskah.demo.model.dto.newspaper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveArticleRequest {
    private String collectionName; // Optional: default, favorites, read-later
    private String notes; // Personal notes
}
