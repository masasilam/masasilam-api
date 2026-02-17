package com.naskah.demo.service.film.impl;

import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.*;
import com.naskah.demo.model.film.FilmDetail.*;
import com.naskah.demo.service.film.FilmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FilmServiceImpl implements FilmService {

    @Autowired
    private FilmMapper filmMapper;

    // Slug generation patterns
    private static final Pattern NON_LATIN    = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE   = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    private static final String IMAGE_URL_DELIMITER   = "||";
    private static final String IMAGE_URL_SPLIT_REGEX = "\\|\\|";

    // ==================== PUBLIC METHODS ====================

    @Override
    public FilmDetail getFilmDetailBySlug(String slug) {
        Film film = filmMapper.findBySlug(slug);
        if (film == null) return null;
        return buildFilmDetail(film);
    }

    @Override
    public FilmDetail getFilmDetailByQid(String qid) {
        Film film = filmMapper.findByQid(qid);
        if (film == null) return null;
        return buildFilmDetail(film);
    }

    @Transactional
    @Override
    public Film saveFilm(FilmDetail filmDetail) {

        // ==================== VALIDATION & SANITIZATION ====================

        if (filmDetail.getWikidataQid() == null || filmDetail.getWikidataQid().trim().isEmpty()) {
            throw new IllegalArgumentException("Wikidata QID cannot be null or empty");
        }

        String judul = filmDetail.getJudul();

        if (judul == null || judul.trim().isEmpty() || judul.equalsIgnoreCase("film")) {
            log.warn("judul is null/empty/invalid for QID: {}", filmDetail.getWikidataQid());

            String deskripsi = filmDetail.getDeskripsi();
            if (deskripsi != null && !deskripsi.isEmpty()) {
                judul = deskripsi.length() > 100 ? deskripsi.substring(0, 97) + "..." : deskripsi;
                log.warn("Using description as title fallback: {}", judul);
            } else {
                judul = "Film " + filmDetail.getWikidataQid();
                log.warn("Using QID as title fallback: {}", judul);
            }
        }

        if (judul == null || judul.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Film title (judul) cannot be determined. Wikidata QID: " + filmDetail.getWikidataQid()
            );
        }

        log.info("Saving film: {} ({})", judul, filmDetail.getWikidataQid());

        // ==================== FIND EXISTING FILM ====================

        Film existing = filmMapper.findByQid(filmDetail.getWikidataQid());

        // ==================== PREPARE FILM ENTITY ====================

        Film film = new Film();
        film.setWikidataQid(filmDetail.getWikidataQid());

        String filmSlug = generateUniqueSlug(
                judul,
                filmDetail.getTahunRilis() != null ? filmDetail.getTahunRilis().substring(0, 4) : null
        );
        film.setSlug(filmSlug);
        filmDetail.setSlug(filmSlug);

        // Basic fields
        film.setJudul(judul);
        film.setTahunRilis(filmDetail.getTahunRilis());
        film.setJenis(filmDetail.getJenis());
        film.setDeskripsi(filmDetail.getDeskripsi());
        film.setDurasi(filmDetail.getDurasi());
        film.setNegaraAsal(filmDetail.getNegaraAsal());

        // Media fields
        film.setPosterUrl(filmDetail.getPosterUrl());
        film.setImageUrls(serializeImageUrls(filmDetail.getImageUrls()));
        film.setVideoUrl(filmDetail.getVideoUrl());
        film.setTrailerUrl(filmDetail.getTrailerUrl());
        film.setSubtitleUrl(filmDetail.getSubtitleUrl());

        // Technical fields
        film.setColor(filmDetail.getColor());
        film.setOriginalLanguage(filmDetail.getOriginalLanguage());

        // Budget
        if (filmDetail.getBudget() != null) {
            film.setBudget(filmDetail.getBudget().getAmount());
            film.setBudgetDisplay(filmDetail.getBudget().getDisplayValue());
        }

        // Relations
        film.setFollowedBy(filmDetail.getFollowedBy());
        film.setPartOfSeries(filmDetail.getPartOfSeries());

        // ==================== INSERT OR UPDATE FILM ====================

        if (existing == null) {
            filmMapper.insert(film);
            log.info("Film inserted with ID: {}", film.getId());
        } else {
            film.setId(existing.getId());
            deleteAllRelations(film.getId());
            filmMapper.update(film);
            log.info("Film updated with ID: {}", film.getId());
        }

        // ==================== SAVE ALL RELATIONS ====================

        saveGenres(film.getId(), filmDetail.getGenre());
        savePersons(film.getId(), filmDetail);
        saveCompanies(film.getId(), filmDetail);
        saveLocations(film.getId(), filmDetail);
        saveBoxOffice(film.getId(), filmDetail.getBoxOffice());
        saveReviews(film.getId(), filmDetail.getReviewScores());
        saveContentRatings(film.getId(), filmDetail.getContentRatings());
        saveAliases(film.getId(), filmDetail.getAliasIndonesia());

        return film;
    }

    @Override
    public List<Film> getAllFilms(int page, int size) {
        int offset = page * size;
        return filmMapper.findAll(size, offset);
    }

    @Override
    public int getTotalFilms() {
        return filmMapper.count();
    }

    @Override
    public List<Film> searchFilms(String query, int page, int size) {
        int offset = page * size;
        return filmMapper.search(query, size, offset);
    }

    @Override
    public int getTotalSearchResults(String query) {
        return filmMapper.countSearch(query);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Build complete FilmDetail dari Film entity
     */
    private FilmDetail buildFilmDetail(Film film) {
        FilmDetail detail = new FilmDetail();

        // Basic info
        detail.setId(film.getId());
        detail.setWikidataQid(film.getWikidataQid());
        detail.setSlug(film.getSlug());
        detail.setJudul(film.getJudul());
        detail.setTahunRilis(film.getTahunRilis());
        detail.setJenis(film.getJenis());
        detail.setDeskripsi(film.getDeskripsi());
        detail.setDurasi(film.getDurasi());
        detail.setNegaraAsal(film.getNegaraAsal());

        // Media
        detail.setPosterUrl(film.getPosterUrl());
        detail.setImageUrls(deserializeImageUrls(film.getImageUrls()));
        detail.setVideoUrl(film.getVideoUrl());
        detail.setTrailerUrl(film.getTrailerUrl());
        detail.setSubtitleUrl(film.getSubtitleUrl());

        // Technical
        detail.setColor(film.getColor());
        detail.setOriginalLanguage(film.getOriginalLanguage());

        // Budget
        if (film.getBudget() != null) {
            BudgetData budget = new BudgetData();
            budget.setAmount(film.getBudget());
            budget.setCurrency("USD");
            budget.setDisplayValue(film.getBudgetDisplay());
            detail.setBudget(budget);
        }

        // Relations
        detail.setFollowedBy(film.getFollowedBy());
        detail.setPartOfSeries(film.getPartOfSeries());

        // Genres
        detail.setGenre(filmMapper.findGenresByFilmId(film.getId()));

        // People
        detail.setSutradara(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "director"));
        detail.setPenulisSkenario(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "writer"));
        detail.setPemeran(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "actor"));
        detail.setProduser(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "producer"));
        detail.setFilmEditor(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "editor"));
        detail.setCinematographer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "cinematographer"));
        detail.setComposer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "composer"));

        // Companies
        detail.setPerusahaanProduksi(filmMapper.findProductionCompaniesByFilmId(film.getId()));
        detail.setDistributor(filmMapper.findDistributorsByFilmId(film.getId()));

        // Locations
        detail.setNarrativeLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "narrative"));
        detail.setFilmingLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "filming"));

        // Financial & ratings
        detail.setBoxOffice(loadBoxOffice(filmMapper.findBoxOfficeByFilmId(film.getId())));
        detail.setReviewScores(filmMapper.findReviewsByFilmId(film.getId()));
        detail.setContentRatings(filmMapper.findContentRatingsByFilmId(film.getId()));

        // Aliases
        detail.setAliasIndonesia(filmMapper.findAliasesByFilmIdAndLanguage(film.getId(), "id"));

        return detail;
    }

    /**
     * Serialize List<String> imageUrls ke TEXT column dengan delimiter "||"
     */
    private String serializeImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return null;

        List<String> filtered = new ArrayList<>();
        for (String url : imageUrls) {
            if (url != null && !url.trim().isEmpty()) {
                filtered.add(url.trim());
            }
        }

        return filtered.isEmpty() ? null : String.join(IMAGE_URL_DELIMITER, filtered);
    }

    /**
     * Deserialize TEXT column kembali ke List<String> imageUrls
     */
    private List<String> deserializeImageUrls(String imageUrlsRaw) {
        if (imageUrlsRaw == null || imageUrlsRaw.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] parts = imageUrlsRaw.split(IMAGE_URL_SPLIT_REGEX);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Load box office dan format display values
     */
    private List<BoxOfficeData> loadBoxOffice(List<BoxOfficeData> boxOfficeList) {
        if (boxOfficeList == null) return null;

        for (BoxOfficeData bo : boxOfficeList) {
            if (bo.getDisplayValue() == null && bo.getAmount() != null) {
                bo.setDisplayValue(formatCurrency(bo.getAmount(), bo.getCurrency()));
            }
        }

        return boxOfficeList;
    }

    /**
     * Delete all film relations sebelum update
     */
    private void deleteAllRelations(Long filmId) {
        filmMapper.deleteGenresByFilmId(filmId);
        filmMapper.deletePersonsByFilmId(filmId);
        filmMapper.deleteProductionCompaniesByFilmId(filmId);
        filmMapper.deleteDistributorsByFilmId(filmId);
        filmMapper.deleteLocationsByFilmId(filmId);
        filmMapper.deleteBoxOfficeByFilmId(filmId);
        filmMapper.deleteReviewsByFilmId(filmId);
        filmMapper.deleteContentRatingsByFilmId(filmId);
        filmMapper.deleteAliasesByFilmId(filmId);
        log.debug("All relations deleted for filmId: {}", filmId);
    }

    /**
     * Save genres
     */
    private void saveGenres(Long filmId, List<String> genres) {
        if (genres == null) return;

        for (String genre : genres) {
            String cleanGenre = cleanGenreName(genre);
            filmMapper.insertGenre(filmId, cleanGenre);
        }
        log.debug("Saved {} genres for filmId: {}", genres.size(), filmId);
    }

    /**
     * Save all person relations
     */
    private void savePersons(Long filmId, FilmDetail filmDetail) {
        savePersonList(filmId, filmDetail.getSutradara(), "director");
        savePersonList(filmId, filmDetail.getPenulisSkenario(), "writer");
        savePersonList(filmId, filmDetail.getPemeran(), "actor");
        savePersonList(filmId, filmDetail.getProduser(), "producer");
        savePersonList(filmId, filmDetail.getFilmEditor(), "editor");
        savePersonList(filmId, filmDetail.getCinematographer(), "cinematographer");
        savePersonList(filmId, filmDetail.getComposer(), "composer");
    }

    /**
     * Save person list dengan role
     */
    private void savePersonList(Long filmId, List<Person> persons, String role) {
        if (persons == null) return;

        int saved = 0;
        for (Person person : persons) {
            Long personId = saveOrGetPerson(person);
            if (personId != null) {
                filmMapper.insertFilmPerson(filmId, personId, role);
                saved++;
            }
        }
        log.debug("Saved {} {} for filmId: {}", saved, role, filmId);
    }

    /**
     * Save or get existing person
     */
    private Long saveOrGetPerson(Person person) {
        if (person == null || person.getName() == null) {
            return null;
        }

        if (person.getWikidataQid() == null || person.getWikidataQid().isEmpty()) {
            person.setWikidataQid("TEMP_" + person.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        if (person.getSlug() == null || person.getSlug().isEmpty()) {
            person.setSlug(generateSlug(person.getName()));
        }

        Person existing = filmMapper.findPersonByQid(person.getWikidataQid());

        if (existing == null) {
            filmMapper.insertPerson(person);
            log.debug("Person inserted: {} ({})", person.getName(), person.getWikidataQid());
            return person.getId();
        } else {
            if (needsPersonUpdate(person, existing)) {
                updatePersonFields(existing, person);
                filmMapper.updatePerson(existing);
                log.debug("Person updated: {} ({})", person.getName(), person.getWikidataQid());
            }
            return existing.getId();
        }
    }

    private boolean needsPersonUpdate(Person newPerson, Person existing) {
        return (newPerson.getName() != null && !newPerson.getName().equals(existing.getName())) ||
                (newPerson.getPhotoUrl() != null && !newPerson.getPhotoUrl().equals(existing.getPhotoUrl())) ||
                (newPerson.getDescription() != null && !newPerson.getDescription().equals(existing.getDescription()));
    }

    private void updatePersonFields(Person existing, Person newPerson) {
        if (newPerson.getName() != null) existing.setName(newPerson.getName());
        if (newPerson.getSlug() != null) existing.setSlug(newPerson.getSlug());
        if (newPerson.getPhotoUrl() != null) existing.setPhotoUrl(newPerson.getPhotoUrl());
        if (newPerson.getDescription() != null) existing.setDescription(newPerson.getDescription());
    }

    /**
     * Save all company relations
     */
    private void saveCompanies(Long filmId, FilmDetail filmDetail) {
        saveProductionCompanies(filmId, filmDetail.getPerusahaanProduksi());
        saveDistributors(filmId, filmDetail.getDistributor());
    }

    private void saveProductionCompanies(Long filmId, List<Company> companies) {
        if (companies == null) return;

        int saved = 0;
        for (Company company : companies) {
            Long companyId = saveOrGetCompany(company);
            if (companyId != null) {
                filmMapper.insertFilmProductionCompany(filmId, companyId);
                saved++;
            }
        }
        log.debug("Saved {} production companies for filmId: {}", saved, filmId);
    }

    private void saveDistributors(Long filmId, List<Company> companies) {
        if (companies == null) return;

        int saved = 0;
        for (Company company : companies) {
            Long companyId = saveOrGetCompany(company);
            if (companyId != null) {
                filmMapper.insertFilmDistributor(filmId, companyId);
                saved++;
            }
        }
        log.debug("Saved {} distributors for filmId: {}", saved, filmId);
    }

    private Long saveOrGetCompany(Company company) {
        if (company == null || company.getName() == null) {
            return null;
        }

        if (company.getWikidataQid() == null || company.getWikidataQid().isEmpty()) {
            company.setWikidataQid("TEMP_" + company.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        if (company.getSlug() == null || company.getSlug().isEmpty()) {
            company.setSlug(generateSlug(company.getName()));
        }

        Company existing = filmMapper.findCompanyByQid(company.getWikidataQid());

        if (existing == null) {
            filmMapper.insertCompany(company);
            log.debug("Company inserted: {} ({})", company.getName(), company.getWikidataQid());
            return company.getId();
        } else {
            if (needsCompanyUpdate(company, existing)) {
                updateCompanyFields(existing, company);
                filmMapper.updateCompany(existing);
                log.debug("Company updated: {} ({})", company.getName(), company.getWikidataQid());
            }
            return existing.getId();
        }
    }

    private boolean needsCompanyUpdate(Company newCompany, Company existing) {
        return (newCompany.getName() != null && !newCompany.getName().equals(existing.getName())) ||
                (newCompany.getLogoUrl() != null && !newCompany.getLogoUrl().equals(existing.getLogoUrl())) ||
                (newCompany.getDescription() != null && !newCompany.getDescription().equals(existing.getDescription()));
    }

    private void updateCompanyFields(Company existing, Company newCompany) {
        if (newCompany.getName() != null) existing.setName(newCompany.getName());
        if (newCompany.getSlug() != null) existing.setSlug(newCompany.getSlug());
        if (newCompany.getLogoUrl() != null) existing.setLogoUrl(newCompany.getLogoUrl());
        if (newCompany.getDescription() != null) existing.setDescription(newCompany.getDescription());
    }

    /**
     * Save locations
     */
    private void saveLocations(Long filmId, FilmDetail filmDetail) {
        int count = 0;
        if (filmDetail.getNarrativeLocation() != null) {
            for (String location : filmDetail.getNarrativeLocation()) {
                filmMapper.insertLocation(filmId, "narrative", location);
                count++;
            }
        }
        if (filmDetail.getFilmingLocation() != null) {
            for (String location : filmDetail.getFilmingLocation()) {
                filmMapper.insertLocation(filmId, "filming", location);
                count++;
            }
        }
        log.debug("Saved {} locations for filmId: {}", count, filmId);
    }

    /**
     * Save box office data
     */
    private void saveBoxOffice(Long filmId, List<BoxOfficeData> boxOfficeList) {
        if (boxOfficeList == null) return;

        for (BoxOfficeData bo : boxOfficeList) {
            filmMapper.insertBoxOffice(filmId, bo.getRegion(), bo.getAmount(), bo.getCurrency());
        }
        log.debug("Saved {} box office entries for filmId: {}", boxOfficeList.size(), filmId);
    }

    /**
     * Save review scores — skip jika source atau value kosong
     */
    private void saveReviews(Long filmId, List<ReviewScore> reviews) {
        if (reviews == null) return;

        int saved = 0;
        int skipped = 0;
        for (ReviewScore review : reviews) {
            if (review.getSource() == null || review.getSource().isEmpty() ||
                    review.getValue() == null || review.getValue().isEmpty()) {
                log.warn("Skipping review with missing required fields (source: {})",
                        review.getSource() != null ? review.getSource() : "unknown");
                skipped++;
                continue;
            }

            filmMapper.insertReview(
                    filmId,
                    review.getSource(),
                    review.getScoreType(),
                    review.getValue(),
                    review.getNumReviews(),
                    review.getReviewDate()
            );
            saved++;
        }
        log.debug("Saved {} reviews, skipped {} for filmId: {}", saved, skipped, filmId);
    }

    /**
     * Save content ratings — skip jika value null (violate NOT NULL constraint)
     */
    private void saveContentRatings(Long filmId, List<ContentRating> ratings) {
        if (ratings == null) return;

        int saved = 0;
        int skipped = 0;
        for (ContentRating rating : ratings) {
            if (rating.getValue() == null || rating.getValue().isEmpty()) {
                log.warn("Skipping content rating: {} (NULL value)", rating.getSystem());
                skipped++;
                continue;
            }

            filmMapper.insertContentRating(
                    filmId,
                    rating.getSystem(),
                    rating.getValue(),
                    rating.getContentDescriptors(),
                    rating.getStartDate(),
                    rating.getDistributionFormat()
            );
            saved++;
        }
        log.debug("Saved {} ratings, skipped {} for filmId: {}", saved, skipped, filmId);
    }

    /**
     * Save aliases
     */
    private void saveAliases(Long filmId, List<String> aliases) {
        if (aliases == null) return;

        for (String alias : aliases) {
            filmMapper.insertAlias(filmId, alias, "id");
        }
        log.debug("Saved {} aliases for filmId: {}", aliases.size(), filmId);
    }

    // ==================== UTILITY METHODS ====================

    private String cleanGenreName(String genre) {
        if (genre == null || genre.isEmpty()) return genre;

        String cleaned = genre.trim()
                .replaceAll("(?i)\\s+films?$", "")
                .trim();

        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }

        return cleaned;
    }

    private String generateSlug(String input) {
        if (input == null || input.isEmpty()) return "";

        String slug = input.toLowerCase(Locale.ENGLISH);
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-+", "-");

        return slug;
    }

    private String generateUniqueSlug(String input, String suffix) {
        String baseSlug = generateSlug(input);
        if (suffix != null && !suffix.isEmpty()) {
            return baseSlug + "-" + suffix;
        }
        return baseSlug;
    }

    private String formatCurrency(long amountInCents, String currency) {
        double amount = amountInCents / 100.0;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        if ("USD".equals(currency)) {
            return formatter.format(amount);
        }

        return currency + " " + String.format("%,.2f", amount);
    }
}