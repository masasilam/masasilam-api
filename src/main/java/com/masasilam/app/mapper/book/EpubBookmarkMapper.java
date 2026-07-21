package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.EpubBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EpubBookmarkMapper {
    List<EpubBookmark> findByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    void insert(EpubBookmark bookmark);
    EpubBookmark findById(@Param("id") Long id);
    void deleteById(@Param("id") Long id);
    Integer countByUser(@Param("userId") Long userId);
    Integer countByUserAndBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
    List<EpubBookmark> findByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
    List<EpubBookmark> findByUserAndZine(@Param("userId") Long userId, @Param("zineId") Long zineId);
}