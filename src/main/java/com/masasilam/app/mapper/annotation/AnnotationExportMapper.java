package com.masasilam.app.mapper.annotation;

import com.masasilam.app.model.entity.AnnotationExport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnnotationExportMapper {
    void insertExport(AnnotationExport export);
    void updateExport(AnnotationExport export);
    AnnotationExport findById(@Param("id") Long id);
    List<AnnotationExport> findUserExports(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}