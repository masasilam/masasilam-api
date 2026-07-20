package com.naskah.app.mapper;

import com.naskah.app.model.film.Company;
import com.naskah.app.model.film.Person;
import com.naskah.app.model.film.Film;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilmMapper {

    // ── Film CRUD ────────────────────────────────────────────────
    void         insert(Film film);
    void         update(Film film);
    void         delete(Long id);
    Film         findBySlug(String slug);
    List<Film>   findAll(@Param("limit") int limit, @Param("offset") int offset);
    int          count();
    List<Film>   search(@Param("query") String query,
                        @Param("limit") int limit,
                        @Param("offset") int offset);
    int          countSearch(@Param("query") String query);
    Film findById(Long id);

    // ── Genre ────────────────────────────────────────────────────
    void         insertGenre(@Param("filmId") Long filmId,
                             @Param("genre")  String genre);
    List<String> findGenresByFilmId(Long filmId);
    void         deleteGenresByFilmId(Long filmId);

    // ── Person ───────────────────────────────────────────────────
    void         insertPerson(Person person);
    void         updatePerson(Person person);          // update photo_url & description
    Person       findPersonBySlug(String slug);
    void         insertFilmPerson(@Param("filmId")    Long   filmId,
                                  @Param("personId")  Long   personId,
                                  @Param("role")      String role);
    List<Person> findPersonsByFilmIdAndRole(@Param("filmId") Long   filmId,
                                            @Param("role")   String role);
    void         deletePersonsByFilmIdAndRole(@Param("filmId") Long   filmId,
                                              @Param("role")   String role);
    void         deletePersonsByFilmId(Long filmId);

    // ── Company ──────────────────────────────────────────────────
    void          insertCompany(Company company);
    void          updateCompany(Company company);       // update logo_url & description
    Company       findCompanyBySlug(String slug);
    void          insertFilmProductionCompany(@Param("filmId")    Long filmId,
                                              @Param("companyId") Long companyId);
    void          insertFilmDistributor(@Param("filmId")    Long filmId,
                                        @Param("companyId") Long companyId);
    List<Company> findProductionCompaniesByFilmId(Long filmId);
    List<Company> findDistributorsByFilmId(Long filmId);
    void          deleteProductionCompaniesByFilmId(Long filmId);
    void          deleteDistributorsByFilmId(Long filmId);

    // ── Location ─────────────────────────────────────────────────
    void         insertLocation(@Param("filmId") Long   filmId,
                                @Param("type")   String type,
                                @Param("name")   String name);
    List<String> findLocationsByFilmIdAndType(@Param("filmId") Long   filmId,
                                              @Param("type")   String type);
    void         deleteLocationsByFilmId(Long filmId);

    // ── Alias ────────────────────────────────────────────────────
    void         insertAlias(@Param("filmId")   Long   filmId,
                             @Param("alias")    String alias,
                             @Param("language") String language);
    List<String> findAliasesByFilmIdAndLanguage(@Param("filmId")   Long   filmId,
                                                @Param("language") String language);
    void         deleteAliasesByFilmId(Long filmId);

    // ── Financial / Review / Rating (delete only) ────────────────
    void deleteBoxOfficeByFilmId(Long filmId);
    void deleteReviewsByFilmId(Long filmId);
    void deleteContentRatingsByFilmId(Long filmId);
}