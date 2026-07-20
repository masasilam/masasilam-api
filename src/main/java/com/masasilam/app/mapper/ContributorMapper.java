package com.masasilam.app.mapper;

import com.masasilam.app.model.entity.Contributor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContributorMapper {

    Contributor findByNameAndRole(@Param("name") String name, @Param("role") String role);

    void insertContributor(Contributor contributor);

    List<Contributor> findAllWithPagination(@Param("offset") int offset,
                                            @Param("limit") int limit,
                                            @Param("role") String role,
                                            @Param("search") String search);

    int countAll(@Param("role") String role, @Param("search") String search);

    Contributor findBySlug(@Param("slug") String slug);

    // ── Tambahan untuk EpubRebuildService ───────────────────
    Contributor findByName(@Param("name") String name);

    int countBookContributor(@Param("bookId") Long bookId,
                             @Param("contributorId") Long contributorId,
                             @Param("role") String role);

    void insertBookContributor(@Param("bookId") Long bookId,
                               @Param("contributorId") Long contributorId,
                               @Param("role") String role);
}