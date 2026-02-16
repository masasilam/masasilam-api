package com.naskah.demo.mapper;

import com.naskah.demo.model.film.Company;
import com.naskah.demo.model.film.Film;
import com.naskah.demo.model.film.Person;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FilmMapper {

    // ==================== FILM OPERATIONS ====================

    @Select("SELECT * FROM films WHERE wikidata_qid = #{qid}")
    Film findByQid(String qid);

    @Select("SELECT * FROM films WHERE slug = #{slug}")
    Film findBySlug(String slug);

    @Insert("INSERT INTO films (wikidata_qid, slug, judul, tahun_rilis, jenis, deskripsi, " +
            "durasi, negara_asal, poster_url, video_url, subtitle_url) " +
            "VALUES (#{wikidataQid}, #{slug}, #{judul}, #{tahunRilis}, #{jenis}, #{deskripsi}, " +
            "#{durasi}, #{negaraAsal}, #{posterUrl}, #{videoUrl}, #{subtitleUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Film film);

    @Update("UPDATE films SET slug = #{slug}, judul = #{judul}, tahun_rilis = #{tahunRilis}, " +
            "jenis = #{jenis}, deskripsi = #{deskripsi}, durasi = #{durasi}, " +
            "negara_asal = #{negaraAsal}, poster_url = #{posterUrl}, " +
            "video_url = #{videoUrl}, subtitle_url = #{subtitleUrl}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void update(Film film);

    @Select("SELECT * FROM films ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Film> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM films")
    int count();

    @Select("SELECT * FROM films WHERE judul LIKE CONCAT('%', #{query}, '%') " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Film> search(@Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM films WHERE judul LIKE CONCAT('%', #{query}, '%')")
    int countSearch(@Param("query") String query);

    // ==================== GENRE OPERATIONS ====================

    @Insert("INSERT INTO film_genres (film_id, genre) VALUES (#{filmId}, #{genre})")
    void insertGenre(@Param("filmId") Long filmId, @Param("genre") String genre);

    @Select("SELECT genre FROM film_genres WHERE film_id = #{filmId}")
    List<String> findGenresByFilmId(Long filmId);

    @Delete("DELETE FROM film_genres WHERE film_id = #{filmId}")
    void deleteGenresByFilmId(Long filmId);

    // ==================== PERSON OPERATIONS ====================

    @Select("SELECT * FROM persons WHERE wikidata_qid = #{qid}")
    Person findPersonByQid(@Param("qid") String qid);

    @Select("SELECT * FROM persons WHERE slug = #{slug}")
    Person findPersonBySlug(@Param("slug") String slug);

    @Insert("INSERT INTO persons (wikidata_qid, slug, name, photo_url, description) " +
            "VALUES (#{wikidataQid}, #{slug}, #{name}, #{photoUrl}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPerson(Person person);

    @Update("UPDATE persons SET slug = #{slug}, name = #{name}, photo_url = #{photoUrl}, " +
            "description = #{description} WHERE id = #{id}")
    void updatePerson(Person person);

    // ==================== FILM-PERSON RELATION OPERATIONS ====================

    @Insert("INSERT INTO film_persons (film_id, person_id, role_type) " +
            "VALUES (#{filmId}, #{personId}, #{roleType})")
    void insertFilmPerson(@Param("filmId") Long filmId,
                          @Param("personId") Long personId,
                          @Param("roleType") String roleType);

    @Select("SELECT p.* FROM persons p " +
            "INNER JOIN film_persons fp ON p.id = fp.person_id " +
            "WHERE fp.film_id = #{filmId} AND fp.role_type = #{role}")
    List<Person> findPersonObjectsByFilmIdAndRole(@Param("filmId") Long filmId,
                                                  @Param("role") String role);

    @Delete("DELETE FROM film_persons WHERE film_id = #{filmId}")
    void deletePersonsByFilmId(Long filmId);

    // ==================== COMPANY OPERATIONS ====================

    @Select("SELECT * FROM companies WHERE wikidata_qid = #{qid}")
    Company findCompanyByQid(@Param("qid") String qid);

    @Select("SELECT * FROM companies WHERE slug = #{slug}")
    Company findCompanyBySlug(@Param("slug") String slug);

    @Insert("INSERT INTO companies (wikidata_qid, slug, name, logo_url, description) " +
            "VALUES (#{wikidataQid}, #{slug}, #{name}, #{logoUrl}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertCompany(Company company);

    @Update("UPDATE companies SET slug = #{slug}, name = #{name}, logo_url = #{logoUrl}, " +
            "description = #{description} WHERE id = #{id}")
    void updateCompany(Company company);

    // ==================== FILM-COMPANY RELATION OPERATIONS ====================

    @Insert("INSERT INTO film_production_companies (film_id, company_id) " +
            "VALUES (#{filmId}, #{companyId})")
    void insertFilmProductionCompany(@Param("filmId") Long filmId,
                                     @Param("companyId") Long companyId);

    @Select("SELECT c.* FROM companies c " +
            "INNER JOIN film_production_companies fpc ON c.id = fpc.company_id " +
            "WHERE fpc.film_id = #{filmId}")
    List<Company> findCompanyObjectsByFilmId(@Param("filmId") Long filmId);

    @Delete("DELETE FROM film_production_companies WHERE film_id = #{filmId}")
    void deleteProductionCompaniesByFilmId(Long filmId);

    // ==================== ALIAS OPERATIONS ====================

    @Insert("INSERT INTO film_aliases (film_id, alias, language) " +
            "VALUES (#{filmId}, #{alias}, #{language})")
    void insertAlias(@Param("filmId") Long filmId,
                     @Param("alias") String alias,
                     @Param("language") String language);

    @Select("SELECT alias FROM film_aliases WHERE film_id = #{filmId} AND language = #{language}")
    List<String> findAliasesByFilmIdAndLanguage(@Param("filmId") Long filmId,
                                                @Param("language") String language);

    @Delete("DELETE FROM film_aliases WHERE film_id = #{filmId}")
    void deleteAliasesByFilmId(Long filmId);
}