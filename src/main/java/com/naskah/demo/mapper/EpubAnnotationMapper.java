package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.EpubAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EpubAnnotationMapper {

    /**
     * Ambil semua anotasi milik user untuk buku tertentu,
     * diurutkan dari yang terbaru.
     */
    List<EpubAnnotation> findByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    /** Insert baru, id di-generate oleh DB (useGeneratedKeys). */
    void insert(EpubAnnotation annotation);

    /**
     * Cari berdasarkan id — dipakai untuk validasi ownership sebelum delete.
     */
    EpubAnnotation findById(@Param("id") Long id);

    void deleteById(@Param("id") Long id);
}
