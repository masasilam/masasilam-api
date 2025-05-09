package com.naskah.demo.mapper;

import com.naskah.demo.model.pojo.EbookPojo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface EbookMapper {
    List<EbookPojo> getListEbook(@Param("offset") int offset, @Param("limit") int limit,
                                 @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);
    EbookPojo getDetailEbook(@Param("id") String id);
    void deleteEbook(@Param("id") String id);
}
