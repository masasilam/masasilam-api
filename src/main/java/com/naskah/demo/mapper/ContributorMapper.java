package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Contributor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContributorMapper {
    Contributor findByNameAndRole(@Param("name") String name, @Param("role") String role);
    void insertContributor(Contributor contributor);
}
