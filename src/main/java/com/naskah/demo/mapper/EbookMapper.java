package com.naskah.demo.mapper;

import com.naskah.demo.model.pojo.EbookPojo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EbookMapper {
    void insertEbook(EbookPojo ebookPojo);
    int countEbook(@Param("title") String title, @Param("author") String author, @Param("year") int year );
    EbookPojo getDetailEbook(@Param("id") String id);
    List<EbookPojo> getListEbook(@Param("offset") int offset, @Param("limit") int limit,
                                 @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);
    void updateEbook(EbookPojo ebookPojo);
    void deleteEbook(@Param("id") String id);
}
