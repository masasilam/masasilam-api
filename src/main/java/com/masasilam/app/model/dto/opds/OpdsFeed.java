package com.masasilam.app.model.dto.opds;

import lombok.Data;
import java.util.List;

@Data
public class OpdsFeed {
    private String id;
    private String title;
    private String updated;
    private Integer totalResults;
    private Integer itemsPerPage;
    private Integer startIndex;
    private List<OpdsLink> links;
    private List<OpdsEntry> entries;
}