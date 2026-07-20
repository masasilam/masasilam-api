package com.masasilam.app.service.common;

import com.masasilam.app.model.dto.request.SubmitCorrectionRequest;
import com.masasilam.app.model.dto.response.CorrectionResponse;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.DatatableResponse;

import java.util.List;

public interface CorrectionService {
    DataResponse<CorrectionResponse> submitCorrection(String bookSlug, Integer chapterNumber, SubmitCorrectionRequest request);
    DataResponse<List<Integer>> getPendingPositions(String bookSlug, Integer chapterNumber);
    DatatableResponse<CorrectionResponse> getCorrections(String status, int page, int limit);
    DataResponse<Void> approveCorrection(Long correctionId);
    DataResponse<Void> rejectCorrection(Long correctionId, String note);
    DataResponse<List<CorrectionResponse>> getMyPendingForBook(String slug);
}