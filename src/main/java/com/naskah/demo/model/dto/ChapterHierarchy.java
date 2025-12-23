package com.naskah.demo.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterHierarchy {
    String href;
    String title;
    int level;
    String parentHref;
}