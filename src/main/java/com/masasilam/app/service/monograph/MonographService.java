package com.masasilam.app.service.monograph;

import com.masasilam.app.model.dto.monograph.*;
import com.masasilam.app.model.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface MonographService {
    DataResponse<MonographDetailResponse> createMonograph(CreateMonographRequest request);
    DataResponse<MonographDetailResponse> updateMonograph(Long id, UpdateMonographRequest request);
    DataResponse<MonographDetailResponse> saveStructure(Long id, MonographStructureRequest request);
    DataResponse<MonographDetailResponse> getMonographDetailBySlug(String slug);
    DatatableResponse<MonographResponse> getMonographs(int page, int limit, String sortField, String sortOrder, MonographSearchCriteria criteria);
    DataResponse<List<MonographResponse>> getLatestMonographs(int limit);
    DataResponse<MonographValidationResponse> validateMonograph(Long id);
    DataResponse<MonographDetailResponse> updateStatus(Long id, UpdateMonographStatusRequest request);
    DataResponse<Void> deleteMonograph(Long id);
    void trackView(String slug, HttpServletRequest request);
    List<MonographSitemapItemResponse> getMonographsForSitemap();
    DataResponse<MonographScaffoldResponse> getScaffold(Long sourceId, LocalDate fromDate, LocalDate toDate);
    DataResponse<MonographAdjacentResponse> getAdjacentMonographs(Long id);
}