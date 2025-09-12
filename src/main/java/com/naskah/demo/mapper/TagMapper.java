package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {
    List<Tag> findTagsByBookId(@Param("bookId") Long bookId);
}
