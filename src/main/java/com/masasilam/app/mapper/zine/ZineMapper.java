package com.masasilam.app.mapper.zine;

import com.masasilam.app.model.dto.ZineSearchCriteria;
import com.masasilam.app.model.dto.response.BookRecommendationResponse;
import com.masasilam.app.model.dto.response.ZineResponse;
import com.masasilam.app.model.entity.Author;
import com.masasilam.app.model.entity.ZineView;
import com.masasilam.app.model.entity.Zine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ZineMapper {
    void insertZine(Zine zine);
    void updateZine(Zine zine);
    void deleteZine(Long id);
    Zine findById(Long id);
    Zine findZineBySlug(String slug);
    Zine findBySlug(String slug);
    Long getZineIdBySlug(String slug);
    int countBySlug(String slug);
    ZineResponse getZineDetailBySlug(String slug);
    List<ZineResponse> getZineListWithAdvancedFilters(@Param("criteria") ZineSearchCriteria criteria, @Param("offset") int offset, @Param("limit") int limit, @Param("sortColumn") String sortColumn, @Param("sortType") String sortType);
    int countZinesWithAdvancedFilters(@Param("criteria") ZineSearchCriteria criteria);
    void incrementViewCountBySlug(String slug);
    void incrementDownloadCount(Long zineId);
    boolean hasActionByHash(@Param("viewerHash") String viewerHash, @Param("actionType") String actionType);
    void insertAction(ZineView zineView);
    List<Author> findAuthorsByZineId(Long zineId);
    List<String> findAuthorNamesByZineId(Long zineId);
    void insertZineGenre(@Param("zineId") Long zineId, @Param("genreId") Long genreId);
    void insertZineAuthor(@Param("zineId") Long zineId, @Param("authorId") Long authorId);
    void insertZineContributor(@Param("zineId") Long zineId, @Param("contributorId") Long contributorId, @Param("role") String role);
    void deleteZineGenres(Long zineId);
    void deleteZineContributors(Long zineId);
    List<Zine> findAllZinesForSitemap();
    List<String> getChapterPathsForSitemap(String zineSlug);
    List<BookRecommendationResponse> getZineRecommendations(@Param("userId") Long userId, @Param("favoriteGenres") List<String> favoriteGenres, @Param("limit") int limit);
    boolean hasActionByUserAndZine(@Param("zineId") Long zineId, @Param("userId") Long userId, @Param("actionType") String actionType);
    int countUserReadSessions(@Param("zineId") Long zineId, @Param("userId") Long userId);
    void incrementReadCount(Long zineId);
    Integer findEarliestPublicationYearByCollection(String collectionName);
}