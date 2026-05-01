package com.naskah.demo.service.zine;

import com.naskah.demo.model.dto.ZineSearchCriteria;
import com.naskah.demo.model.dto.request.ZineRequest;
import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.entity.Zine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ZineService {

    DataResponse<ZineResponse> createZine(ZineRequest request);

    DataResponse<ZineResponse> getZineDetailBySlug(String slug, HttpServletRequest request);

    DatatableResponse<ZineResponse> getPaginatedZines(int page, int limit, String sortField,
                                                      String sortOrder, ZineSearchCriteria criteria);

    DataResponse<Zine> update(Long id, Zine zine, MultipartFile file) throws IOException;

    DefaultResponse delete(Long id) throws IOException;

    ResponseEntity<?> getDownloadUrl(String slug, HttpServletRequest request);

    DataResponse<List<GenreResponse>> getAllGenres(boolean includeZineCount);

    DatatableResponse<AuthorResponse> getAllAuthors(int page, int limit, String search, String sortBy);

    DatatableResponse<ContributorResponse> getAllContributors(int page, int limit, String role, String search);

    List<Zine> getAllZinesForSitemap();

    List<String> getChapterPaths(String zineSlug);
}