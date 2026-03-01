package com.naskah.demo.mapper;

import com.naskah.demo.model.film.*;
import com.naskah.demo.model.film.FilmDetail.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * FilmMapper - MyBatis mapper for film database operations
 * PostgreSQL Compatible Version
 *
 * Perubahan dari versi sebelumnya:
 * 1. INSERT/UPDATE films sekarang menyertakan kolom title_eng
 * 2. insertFilmPerson, insertFilmProductionCompany, insertFilmDistributor
 *    menggunakan ON CONFLICT DO NOTHING untuk mencegah duplicate key error
 *    ketika Wikidata memiliki claim duplikat untuk entitas yang sama
 */
@Mapper
public interface FilmMapper {

    // ==================== FILM OPERATIONS ====================

    @Select("SELECT * FROM films WHERE wikidata_qid = #{qid}")
    Film findByQid(String qid);

    @Select("SELECT * FROM films WHERE slug = #{slug}")
    Film findBySlug(String slug);

    @Insert("INSERT INTO films (wikidata_qid, slug, judul, title_eng, tahun_rilis, jenis, deskripsi, " +
            "durasi, negara_asal, poster_url, image_urls, video_url, trailer_url, subtitle_url, " +
            "color, original_language, budget, budget_display, followed_by, part_of_series) " +
            "VALUES (#{wikidataQid}, #{slug}, #{judul}, #{titleEng}, #{tahunRilis}, #{jenis}, #{deskripsi}, " +
            "#{durasi}, #{negaraAsal}, #{posterUrl}, #{imageUrls}, #{videoUrl}, #{trailerUrl}, #{subtitleUrl}, " +
            "#{color}, #{originalLanguage}, #{budget}, #{budgetDisplay}, #{followedBy}, #{partOfSeries})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Film film);

    @Update("UPDATE films SET slug = #{slug}, judul = #{judul}, title_eng = #{titleEng}, " +
            "tahun_rilis = #{tahunRilis}, jenis = #{jenis}, deskripsi = #{deskripsi}, durasi = #{durasi}, " +
            "negara_asal = #{negaraAsal}, poster_url = #{posterUrl}, image_urls = #{imageUrls}, " +
            "video_url = #{videoUrl}, trailer_url = #{trailerUrl}, subtitle_url = #{subtitleUrl}, " +
            "color = #{color}, original_language = #{originalLanguage}, " +
            "budget = #{budget}, budget_display = #{budgetDisplay}, " +
            "followed_by = #{followedBy}, part_of_series = #{partOfSeries}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void update(Film film);

    @Select("SELECT * FROM films ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Film> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM films")
    int count();

    @Select("SELECT * FROM films WHERE judul ILIKE CONCAT('%', #{query}, '%') OR title_eng ILIKE CONCAT('%', #{query}, '%') " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Film> search(@Param("query") String query, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM films WHERE judul ILIKE CONCAT('%', #{query}, '%') OR title_eng ILIKE CONCAT('%', #{query}, '%')")
    int countSearch(@Param("query") String query);

    // ==================== GENRE OPERATIONS ====================

    @Insert("INSERT INTO film_genres (film_id, genre) VALUES (#{filmId}, #{genre}) " +
            "ON CONFLICT DO NOTHING")
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
            "description = #{description}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updatePerson(Person person);

    // ==================== FILM-PERSON RELATION OPERATIONS ====================

    /**
     * ON CONFLICT DO NOTHING mencegah duplicate key error ketika Wikidata
     * memiliki claim P57/P58/P161 dst yang sama lebih dari satu kali untuk entitas yang sama.
     * Membutuhkan unique constraint: UNIQUE (film_id, person_id, role_type)
     */
    @Insert("INSERT INTO film_persons (film_id, person_id, role_type) " +
            "VALUES (#{filmId}, #{personId}, #{roleType}) " +
            "ON CONFLICT (film_id, person_id, role_type) DO NOTHING")
    void insertFilmPerson(@Param("filmId") Long filmId,
                          @Param("personId") Long personId,
                          @Param("roleType") String roleType);

    @Select("SELECT p.* FROM persons p " +
            "INNER JOIN film_persons fp ON p.id = fp.person_id " +
            "WHERE fp.film_id = #{filmId} AND fp.role_type = #{role}")
    List<Person> findPersonsByFilmIdAndRole(@Param("filmId") Long filmId,
                                            @Param("role") String role);

    @Delete("DELETE FROM film_persons WHERE film_id = #{filmId}")
    void deletePersonsByFilmId(Long filmId);

    @Delete("DELETE FROM film_persons WHERE film_id = #{filmId} AND role_type = #{roleType}")
    void deletePersonsByFilmIdAndRole(@Param("filmId") Long filmId, @Param("roleType") String roleType);

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
            "description = #{description}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateCompany(Company company);

    // ==================== FILM-COMPANY RELATION OPERATIONS ====================

    /**
     * ON CONFLICT DO NOTHING mencegah duplicate jika Wikidata claim P272 memiliki
     * entitas perusahaan yang sama lebih dari sekali.
     * Membutuhkan unique constraint: UNIQUE (film_id, company_id) pada tabel masing-masing.
     */
    @Insert("INSERT INTO film_production_companies (film_id, company_id) " +
            "VALUES (#{filmId}, #{companyId}) " +
            "ON CONFLICT (film_id, company_id) DO NOTHING")
    void insertFilmProductionCompany(@Param("filmId") Long filmId,
                                     @Param("companyId") Long companyId);

    @Insert("INSERT INTO film_distributors (film_id, company_id) " +
            "VALUES (#{filmId}, #{companyId}) " +
            "ON CONFLICT (film_id, company_id) DO NOTHING")
    void insertFilmDistributor(@Param("filmId") Long filmId,
                               @Param("companyId") Long companyId);

    @Select("SELECT c.* FROM companies c " +
            "INNER JOIN film_production_companies fpc ON c.id = fpc.company_id " +
            "WHERE fpc.film_id = #{filmId}")
    List<Company> findProductionCompaniesByFilmId(@Param("filmId") Long filmId);

    @Select("SELECT c.* FROM companies c " +
            "INNER JOIN film_distributors fd ON c.id = fd.company_id " +
            "WHERE fd.film_id = #{filmId}")
    List<Company> findDistributorsByFilmId(@Param("filmId") Long filmId);

    @Delete("DELETE FROM film_production_companies WHERE film_id = #{filmId}")
    void deleteProductionCompaniesByFilmId(Long filmId);

    @Delete("DELETE FROM film_distributors WHERE film_id = #{filmId}")
    void deleteDistributorsByFilmId(Long filmId);

    // ==================== LOCATION OPERATIONS ====================

    @Insert("INSERT INTO film_locations (film_id, location_type, location_name) " +
            "VALUES (#{filmId}, #{locationType}, #{locationName}) " +
            "ON CONFLICT DO NOTHING")
    void insertLocation(@Param("filmId") Long filmId,
                        @Param("locationType") String locationType,
                        @Param("locationName") String locationName);

    @Select("SELECT location_name FROM film_locations " +
            "WHERE film_id = #{filmId} AND location_type = #{locationType}")
    List<String> findLocationsByFilmIdAndType(@Param("filmId") Long filmId,
                                              @Param("locationType") String locationType);

    @Delete("DELETE FROM film_locations WHERE film_id = #{filmId}")
    void deleteLocationsByFilmId(Long filmId);

    // ==================== BOX OFFICE OPERATIONS ====================

    @Insert("INSERT INTO film_box_office (film_id, region, amount, currency) " +
            "VALUES (#{filmId}, #{region}, #{amount}, #{currency}) " +
            "ON CONFLICT (film_id, region) " +
            "DO UPDATE SET amount = EXCLUDED.amount, currency = EXCLUDED.currency")
    void insertBoxOffice(@Param("filmId") Long filmId,
                         @Param("region") String region,
                         @Param("amount") Long amount,
                         @Param("currency") String currency);

    @Select("SELECT region, amount, currency FROM film_box_office WHERE film_id = #{filmId}")
    @Results({
            @Result(property = "region", column = "region"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "currency", column = "currency")
    })
    List<BoxOfficeData> findBoxOfficeByFilmId(Long filmId);

    @Delete("DELETE FROM film_box_office WHERE film_id = #{filmId}")
    void deleteBoxOfficeByFilmId(Long filmId);

    // ==================== REVIEW OPERATIONS ====================

    @Insert("INSERT INTO film_reviews (film_id, review_source, score_type, score_value, " +
            "num_reviews, review_date) " +
            "VALUES (#{filmId}, #{source}, #{scoreType}, #{value}, #{numReviews}, " +
            "CAST(#{reviewDate} AS DATE)) " +
            "ON CONFLICT DO NOTHING")
    void insertReview(@Param("filmId") Long filmId,
                      @Param("source") String source,
                      @Param("scoreType") String scoreType,
                      @Param("value") String value,
                      @Param("numReviews") Integer numReviews,
                      @Param("reviewDate") String reviewDate);

    @Select("SELECT review_source as source, score_type as scoreType, score_value as value, " +
            "num_reviews as numReviews, review_date::text as reviewDate " +
            "FROM film_reviews WHERE film_id = #{filmId}")
    List<ReviewScore> findReviewsByFilmId(Long filmId);

    @Delete("DELETE FROM film_reviews WHERE film_id = #{filmId}")
    void deleteReviewsByFilmId(Long filmId);

    // ==================== CONTENT RATING OPERATIONS ====================

    @Insert("INSERT INTO film_content_ratings (film_id, rating_system, rating_value, " +
            "content_descriptors, start_date, distribution_format) " +
            "VALUES (#{filmId}, #{system}, #{value}, #{descriptors}, " +
            "CAST(#{startDate} AS DATE), #{format}) " +
            "ON CONFLICT DO NOTHING")
    void insertContentRating(@Param("filmId") Long filmId,
                             @Param("system") String system,
                             @Param("value") String value,
                             @Param("descriptors") String descriptors,
                             @Param("startDate") String startDate,
                             @Param("format") String format);

    @Select("SELECT rating_system as system, rating_value as value, " +
            "content_descriptors as contentDescriptors, start_date::text as startDate, " +
            "distribution_format as distributionFormat " +
            "FROM film_content_ratings WHERE film_id = #{filmId}")
    List<ContentRating> findContentRatingsByFilmId(Long filmId);

    @Delete("DELETE FROM film_content_ratings WHERE film_id = #{filmId}")
    void deleteContentRatingsByFilmId(Long filmId);

    // ==================== ALIAS OPERATIONS ====================

    @Insert("INSERT INTO film_aliases (film_id, alias, language) " +
            "VALUES (#{filmId}, #{alias}, #{language}) " +
            "ON CONFLICT DO NOTHING")
    void insertAlias(@Param("filmId") Long filmId,
                     @Param("alias") String alias,
                     @Param("language") String language);

    @Select("SELECT alias FROM film_aliases WHERE film_id = #{filmId} AND language = #{language}")
    List<String> findAliasesByFilmIdAndLanguage(@Param("filmId") Long filmId,
                                                @Param("language") String language);

    @Delete("DELETE FROM film_aliases WHERE film_id = #{filmId}")
    void deleteAliasesByFilmId(Long filmId);
}