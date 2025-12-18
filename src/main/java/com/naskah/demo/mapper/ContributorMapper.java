package com.naskah.demo.mapper;

import com.naskah.demo.model.entity.Contributor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContributorMapper {
    Contributor findByNameAndRole(@Param("name") String name, @Param("role") String role);
    void insertContributor(Contributor contributor);

    @Select("<script>" +
            "SELECT c.*, COUNT(bc.book_id) AS total_books " +
            "FROM contributors c " +
            "LEFT JOIN book_contributors bc ON c.id = bc.contributor_id " +
            "<where>" +
            "  <if test='role != null and role != \"\"'>" +
            "    AND c.role = #{role}" +
            "  </if>" +
            "  <if test='search != null and search != \"\"'>" +
            "    AND c.name LIKE CONCAT('%', #{search}, '%')" +
            "  </if>" +
            "</where>" +
            "GROUP BY c.id " +
            "ORDER BY c.name ASC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Contributor> findAllWithPagination(@Param("offset") int offset,
                                            @Param("limit") int limit,
                                            @Param("role") String role,
                                            @Param("search") String search);

    @Select("<script>" +
            "SELECT COUNT(*) FROM contributors " +
            "<where>" +
            "  <if test='role != null and role != \"\"'>" +
            "    role = #{role}" +
            "  </if>" +
            "  <if test='search != null and search != \"\"'>" +
            "    AND name LIKE CONCAT('%', #{search}, '%')" +
            "  </if>" +
            "</where>" +
            "</script>")
    int countAll(@Param("role") String role, @Param("search") String search);

    Contributor findBySlug(@Param("slug") String slug);
}
