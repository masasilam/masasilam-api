package com.naskah.demo.service.film.impl;

import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.*;
import com.naskah.demo.model.film.FilmDetail.*;
import com.naskah.demo.service.film.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * FilmServiceImpl - Implementation of FilmService
 */
@Service
public class FilmServiceImpl implements FilmService {

    @Autowired
    private FilmMapper filmMapper;

    // Slug generation patterns
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");

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

        // Validate Wikidata QID
        if (filmDetail.getWikidataQid() == null || filmDetail.getWikidataQid().trim().isEmpty()) {
            throw new IllegalArgumentException("Wikidata QID cannot be null or empty");
        }

        // Extract and validate judul with fallback logic
        String judul = filmDetail.getJudul();

        // Fallback logic for missing or invalid judul
        if (judul == null || judul.trim().isEmpty() || judul.equalsIgnoreCase("film")) {
            System.err.println("⚠️  WARNING: judul is null/empty/invalid for " + filmDetail.getWikidataQid());

            // Try to extract from description or use QID as fallback
            String deskripsi = filmDetail.getDeskripsi();
            if (deskripsi != null && !deskripsi.isEmpty()) {
                // Use description as title if available
                judul = deskripsi.length() > 100 ? deskripsi.substring(0, 97) + "..." : deskripsi;
                System.err.println("    → Using description as title: " + judul);
            } else {
                // Last resort: use Wikidata QID
                judul = "Film " + filmDetail.getWikidataQid();
                System.err.println("    → Using QID as title: " + judul);
            }
        }

        // Final validation
        if (judul == null || judul.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Film title (judul) cannot be determined. Wikidata QID: " + filmDetail.getWikidataQid()
            );
        }

        // Log untuk debugging
        System.out.println("📝 Saving film: " + judul + " (" + filmDetail.getWikidataQid() + ")");

        // ==================== FIND EXISTING FILM ====================

        Film existing = filmMapper.findByQid(filmDetail.getWikidataQid());

        // ==================== PREPARE FILM ENTITY ====================

        Film film = new Film();
        film.setWikidataQid(filmDetail.getWikidataQid());

        // Generate slug for film
        String filmSlug = generateUniqueSlug(
                judul,
                filmDetail.getTahunRilis() != null ? filmDetail.getTahunRilis().substring(0, 4) : null
        );
        film.setSlug(filmSlug);
        filmDetail.setSlug(filmSlug);

        // Set basic fields
        film.setJudul(judul);  // Use validated judul
        film.setTahunRilis(filmDetail.getTahunRilis());
        film.setJenis(filmDetail.getJenis());
        film.setDeskripsi(filmDetail.getDeskripsi());
        film.setDurasi(filmDetail.getDurasi());
        film.setNegaraAsal(filmDetail.getNegaraAsal());
        film.setPosterUrl(filmDetail.getPosterUrl());
        film.setVideoUrl(filmDetail.getVideoUrl());
        film.setTrailerUrl(filmDetail.getTrailerUrl());
        film.setSubtitleUrl(filmDetail.getSubtitleUrl());

        // Set technical fields
        film.setColor(filmDetail.getColor());
        film.setOriginalLanguage(filmDetail.getOriginalLanguage());

        // Set budget
        if (filmDetail.getBudget() != null) {
            film.setBudget(filmDetail.getBudget().getAmount());
            film.setBudgetDisplay(filmDetail.getBudget().getDisplayValue());
        }

        // Set relations
        film.setFollowedBy(filmDetail.getFollowedBy());
        film.setPartOfSeries(filmDetail.getPartOfSeries());

        // ==================== INSERT OR UPDATE FILM ====================

        if (existing == null) {
            // Insert new film
            filmMapper.insert(film);
            System.out.println("✅ Film inserted with ID: " + film.getId());
        } else {
            // Update existing film
            film.setId(existing.getId());

            // Delete all old relations FIRST, before any updates
            deleteAllRelations(film.getId());

            // Then update the film record
            filmMapper.update(film);
            System.out.println("✅ Film updated with ID: " + film.getId());
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
     * Build complete FilmDetail from Film entity
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

        // Load all relationships
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
     * Load box office and format display values
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
     * Delete all film relations before update
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
     * Save person list with role
     */
    private void savePersonList(Long filmId, List<Person> persons, String role) {
        if (persons == null) return;

        for (Person person : persons) {
            Long personId = saveOrGetPerson(person);
            if (personId != null) {
                filmMapper.insertFilmPerson(filmId, personId, role);
            }
        }
    }

    /**
     * Save or get existing person
     */
    private Long saveOrGetPerson(Person person) {
        if (person == null || person.getName() == null) {
            return null;
        }

        // Generate temp QID if missing
        if (person.getWikidataQid() == null || person.getWikidataQid().isEmpty()) {
            person.setWikidataQid("TEMP_" + person.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Generate slug
        if (person.getSlug() == null || person.getSlug().isEmpty()) {
            person.setSlug(generateSlug(person.getName()));
        }

        Person existing = filmMapper.findPersonByQid(person.getWikidataQid());

        if (existing == null) {
            filmMapper.insertPerson(person);
            return person.getId();
        } else {
            // Update if there are changes
            if (needsPersonUpdate(person, existing)) {
                updatePersonFields(existing, person);
                filmMapper.updatePerson(existing);
            }
            return existing.getId();
        }
    }

    /**
     * Check if person needs update
     */
    private boolean needsPersonUpdate(Person newPerson, Person existing) {
        return (newPerson.getName() != null && !newPerson.getName().equals(existing.getName())) ||
                (newPerson.getPhotoUrl() != null && !newPerson.getPhotoUrl().equals(existing.getPhotoUrl())) ||
                (newPerson.getDescription() != null && !newPerson.getDescription().equals(existing.getDescription()));
    }

    /**
     * Update person fields
     */
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

    /**
     * Save production companies
     */
    private void saveProductionCompanies(Long filmId, List<Company> companies) {
        if (companies == null) return;

        for (Company company : companies) {
            Long companyId = saveOrGetCompany(company);
            if (companyId != null) {
                filmMapper.insertFilmProductionCompany(filmId, companyId);
            }
        }
    }

    /**
     * Save distributors
     */
    private void saveDistributors(Long filmId, List<Company> companies) {
        if (companies == null) return;

        for (Company company : companies) {
            Long companyId = saveOrGetCompany(company);
            if (companyId != null) {
                filmMapper.insertFilmDistributor(filmId, companyId);
            }
        }
    }

    /**
     * Save or get existing company
     */
    private Long saveOrGetCompany(Company company) {
        if (company == null || company.getName() == null) {
            return null;
        }

        // Generate temp QID if missing
        if (company.getWikidataQid() == null || company.getWikidataQid().isEmpty()) {
            company.setWikidataQid("TEMP_" + company.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Generate slug
        if (company.getSlug() == null || company.getSlug().isEmpty()) {
            company.setSlug(generateSlug(company.getName()));
        }

        Company existing = filmMapper.findCompanyByQid(company.getWikidataQid());

        if (existing == null) {
            filmMapper.insertCompany(company);
            return company.getId();
        } else {
            // Update if there are changes
            if (needsCompanyUpdate(company, existing)) {
                updateCompanyFields(existing, company);
                filmMapper.updateCompany(existing);
            }
            return existing.getId();
        }
    }

    /**
     * Check if company needs update
     */
    private boolean needsCompanyUpdate(Company newCompany, Company existing) {
        return (newCompany.getName() != null && !newCompany.getName().equals(existing.getName())) ||
                (newCompany.getLogoUrl() != null && !newCompany.getLogoUrl().equals(existing.getLogoUrl())) ||
                (newCompany.getDescription() != null && !newCompany.getDescription().equals(existing.getDescription()));
    }

    /**
     * Update company fields
     */
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
        // Narrative locations
        if (filmDetail.getNarrativeLocation() != null) {
            for (String location : filmDetail.getNarrativeLocation()) {
                filmMapper.insertLocation(filmId, "narrative", location);
            }
        }

        // Filming locations
        if (filmDetail.getFilmingLocation() != null) {
            for (String location : filmDetail.getFilmingLocation()) {
                filmMapper.insertLocation(filmId, "filming", location);
            }
        }
    }

    /**
     * Save box office data
     */
    private void saveBoxOffice(Long filmId, List<BoxOfficeData> boxOfficeList) {
        if (boxOfficeList == null) return;

        for (BoxOfficeData bo : boxOfficeList) {
            filmMapper.insertBoxOffice(filmId, bo.getRegion(), bo.getAmount(), bo.getCurrency());
        }
    }

    /**
     * Save review scores
     * Skip reviews with missing required fields
     */
    private void saveReviews(Long filmId, List<ReviewScore> reviews) {
        if (reviews == null) return;

        for (ReviewScore review : reviews) {
            // Skip reviews without source or value - they're incomplete
            if (review.getSource() == null || review.getSource().isEmpty() ||
                    review.getValue() == null || review.getValue().isEmpty()) {
                System.out.println("⚠️  Skipping review: " +
                        (review.getSource() != null ? review.getSource() : "unknown") +
                        " (missing required fields)");
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
        }
    }

    /**
     * Save content ratings
     * Skip ratings with NULL values as they violate database constraints
     */
    private void saveContentRatings(Long filmId, List<ContentRating> ratings) {
        if (ratings == null) return;

        for (ContentRating rating : ratings) {
            // Skip ratings without a value - they're incomplete and violate NOT NULL constraint
            if (rating.getValue() == null || rating.getValue().isEmpty()) {
                System.out.println("⚠️  Skipping content rating: " + rating.getSystem() + " (NULL value)");
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
        }
    }

    /**
     * Save aliases
     */
    private void saveAliases(Long filmId, List<String> aliases) {
        if (aliases == null) return;

        for (String alias : aliases) {
            filmMapper.insertAlias(filmId, alias, "id");
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clean genre name (remove "film" suffix)
     */
    private String cleanGenreName(String genre) {
        if (genre == null || genre.isEmpty()) {
            return genre;
        }

        String cleaned = genre.trim()
                .replaceAll("(?i)\\s+films?$", "")
                .trim();

        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }

        return cleaned;
    }

    /**
     * Generate slug from text
     */
    private String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String slug = input.toLowerCase(Locale.ENGLISH);
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        slug = slug.replaceAll("-+", "-");

        return slug;
    }

    /**
     * Generate unique slug with suffix
     */
    private String generateUniqueSlug(String input, String suffix) {
        String baseSlug = generateSlug(input);
        if (suffix != null && !suffix.isEmpty()) {
            return baseSlug + "-" + suffix;
        }
        return baseSlug;
    }

    /**
     * Format currency amount
     */
    private String formatCurrency(long amountInCents, String currency) {
        double amount = amountInCents / 100.0;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

        if ("USD".equals(currency)) {
            return formatter.format(amount);
        }

        return currency + " " + String.format("%,.2f", amount);
    }
}