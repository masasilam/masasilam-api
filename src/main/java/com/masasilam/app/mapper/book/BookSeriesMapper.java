package com.masasilam.app.mapper.book;

import com.masasilam.app.model.entity.BookSeries;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookSeriesMapper {
    BookSeries findBySlug(String slug);
    BookSeries findById(Long id);
    void insertSeries(BookSeries series);
    void updateSeries(BookSeries series);
}