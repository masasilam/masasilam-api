package com.masasilam.app.service.zine;

import com.masasilam.app.model.dto.request.ZineReadingProgressRequest;
import com.masasilam.app.model.dto.request.ZineReadingSessionRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.ZineReadingProgressResponse;
import com.masasilam.app.model.dto.response.ZineReadingSessionResponse;

public interface ZineReadingService {
    DataResponse<ZineReadingSessionResponse> saveReadingSession(String slug, ZineReadingSessionRequest request);
    DataResponse<ZineReadingProgressResponse> saveOrUpdateProgress(String slug, ZineReadingProgressRequest request);
    DataResponse<ZineReadingProgressResponse> getProgress(String slug);
}