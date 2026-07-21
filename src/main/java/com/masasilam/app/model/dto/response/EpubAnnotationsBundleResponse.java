package com.masasilam.app.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class EpubAnnotationsBundleResponse {
    private List<EpubAnnotationResponse> annotations;
    private List<EpubBookmarkResponse> bookmarks;
}