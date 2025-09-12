package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.CopyrightStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CopyrightStatusMapper {
    CopyrightStatus findByCopyrightStatusCode(@Param("code") String code);
    String findCopyrightStatusNameById(@Param("id") Integer id);
}
