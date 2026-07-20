package com.masasilam.app.model.dto.opds;

import lombok.Data;
import java.util.List;

@Data
public class OpdsEntry {
    private String id;
    private String title;
    private String updated;
    private String summary;
    private String content;
    private List<OpdsAuthor> authors;
    private List<OpdsLink> links;
    // Untuk navigasi entry (kategori, dll)
    private boolean isNavigation = false;
}