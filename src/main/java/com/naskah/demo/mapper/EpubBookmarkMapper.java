package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.EpubBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EpubBookmarkMapper {

    List<EpubBookmark> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    void insert(EpubBookmark bookmark);

    EpubBookmark findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);
}