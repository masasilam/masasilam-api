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
            throw new IllegalArgumentException("Film title (judul) cannot be determined. Wikidata QID: " + filmDetail.getWikidataQid());
        }

        log.info("Saving film: {} ({})", judul, filmDetail.getWikidataQid());

        // ==================== FIND EXISTING FILM ====================

        Film existing = filmMapper.findByQid(filmDetail.getWikidataQid());

        // ==================== PREPARE FILM ENTITY ====================

        Film film = new Film();
        film.setWikidataQid(filmDetail.getWikidataQid());
        String slugSource = (filmDetail.getJudulSlug() != null && !filmDetail.getJudulSlug().isBlank()) ? filmDetail.getJudulSlug() : judul;
        String tahunSuffix = filmDetail.getTahunRilis() != null ? filmDetail.getTahunRilis().substring(0, 4) : null;

        String filmSlug;
        if (existing != null) {
            filmSlug = existing.getSlug();
        } else {
            filmSlug = generateUniqueSlugSafe(slugSource, tahunSuffix, filmDetail.getWikidataQid());
        }

        film.setSlug(filmSlug);
        filmDetail.setSlug(filmSlug);

        // Basic fields
        film.setJudul(judul);
        film.setTahunRilis(filmDetail.getTahunRilis());
        film.setJenis(filmDetail.getJenis());
        film.setDeskripsi(filmDetail.getDeskripsi());
        film.setDurasi(filmDetail.getDurasi());
        film.setNegaraAsal(filmDetail.getNegaraAsal());
        film.setTitleEng(filmDetail.getJudulSlug());
        film.setPosterUrl(filmDetail.getPosterUrl());
        film.setImageUrls(serializeImageUrls(filmDetail.getImageUrls()));
        film.setVideoUrl(filmDetail.getVideoUrl());
        film.setTrailerUrl(filmDetail.getTrailerUrl());
        film.setSubtitleUrl(filmDetail.getSubtitleUrl());
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
            log.info("Film inserted with ID: {}, slug: {}", film.getId(), film.getSlug());
        } else {
            film.setId(existing.getId());
            deleteAllRelations(film.getId());
            filmMapper.update(film);
            log.info("Film updated with ID: {}, slug: {}", film.getId(), film.getSlug());
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

    @Override
    public boolean existsByQid(String qid) {
        return filmMapper.findByQid(qid) != null;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private FilmDetail buildFilmDetail(Film film) {
        FilmDetail detail = new FilmDetail();

        detail.setId(film.getId());
        detail.setWikidataQid(film.getWikidataQid());
        detail.setSlug(film.getSlug());
        detail.setJudul(film.getJudul());

        // Kembalikan titleEng ke judulSlug agar frontend bisa menggunakannya
        detail.setJudulSlug(film.getTitleEng());

        detail.setTahunRilis(film.getTahunRilis());
        detail.setJenis(film.getJenis());
        detail.setDeskripsi(film.getDeskripsi());
        detail.setDurasi(film.getDurasi());
        detail.setNegaraAsal(film.getNegaraAsal());

        detail.setPosterUrl(film.getPosterUrl());
        detail.setImageUrls(deserializeImageUrls(film.getImageUrls()));
        detail.setVideoUrl(film.getVideoUrl());
        detail.setTrailerUrl(film.getTrailerUrl());
        detail.setSubtitleUrl(film.getSubtitleUrl());

        detail.setColor(film.getColor());
        detail.setOriginalLanguage(film.getOriginalLanguage());

        if (film.getBudget() != null) {
            BudgetData budget = new BudgetData();
            budget.setAmount(film.getBudget());
            budget.setCurrency("USD");
            budget.setDisplayValue(film.getBudgetDisplay());
            detail.setBudget(budget);
        }

        detail.setFollowedBy(film.getFollowedBy());
        detail.setPartOfSeries(film.getPartOfSeries());

        detail.setGenre(filmMapper.findGenresByFilmId(film.getId()));

        detail.setSutradara(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "director"));
        detail.setPenulisSkenario(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "writer"));
        detail.setPemeran(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "actor"));
        detail.setProduser(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "producer"));
        detail.setFilmEditor(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "editor"));
        detail.setCinematographer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "cinematographer"));
        detail.setComposer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "composer"));

        detail.setPerusahaanProduksi(filmMapper.findProductionCompaniesByFilmId(film.getId()));
        detail.setDistributor(filmMapper.findDistributorsByFilmId(film.getId()));

        detail.setNarrativeLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "narrative"));
        detail.setFilmingLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "filming"));

        detail.setBoxOffice(loadBoxOffice(filmMapper.findBoxOfficeByFilmId(film.getId())));
        detail.setReviewScores(filmMapper.findReviewsByFilmId(film.getId()));
        detail.setContentRatings(filmMapper.findContentRatingsByFilmId(film.getId()));

        detail.setAliasIndonesia(filmMapper.findAliasesByFilmIdAndLanguage(film.getId(), "id"));

        return detail;
    }

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

    private List<BoxOfficeData> loadBoxOffice(List<BoxOfficeData> boxOfficeList) {
        if (boxOfficeList == null) return null;

        for (BoxOfficeData bo : boxOfficeList) {
            if (bo.getDisplayValue() == null && bo.getAmount() != null) {
                bo.setDisplayValue(formatCurrency(bo.getAmount(), bo.getCurrency()));
            }
        }

        return boxOfficeList;
    }

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

    private void saveGenres(Long filmId, List<String> genres) {
        if (genres == null) return;

        int saved = 0;
        for (String genre : genres) {
            if (genre == null || genre.trim().isEmpty()) continue;
            String cleanGenre = cleanGenreName(genre);
            if (!cleanGenre.isEmpty()) {
                filmMapper.insertGenre(filmId, cleanGenre);
                saved++;
            }
        }
        log.debug("Saved {} genres for filmId: {}", saved, filmId);
    }

    private void savePersons(Long filmId, FilmDetail filmDetail) {
        savePersonList(filmId, filmDetail.getSutradara(), "director");
        savePersonList(filmId, filmDetail.getPenulisSkenario(), "writer");
        savePersonList(filmId, filmDetail.getPemeran(), "actor");
        savePersonList(filmId, filmDetail.getProduser(), "producer");
        savePersonList(filmId, filmDetail.getFilmEditor(), "editor");
        savePersonList(filmId, filmDetail.getCinematographer(), "cinematographer");
        savePersonList(filmId, filmDetail.getComposer(), "composer");
    }

    private void savePersonList(Long filmId, List<Person> persons, String role) {
        if (persons == null) return;

        int saved = 0;
        for (Person person : persons) {
            // Skip null person (terjadi ketika fetchPersonDetails gagal dan kembalikan null)
            if (person == null) continue;

            Long personId = saveOrGetPerson(person);
            if (personId != null) {
                filmMapper.insertFilmPerson(filmId, personId, role);
                saved++;
            }
        }
        log.debug("Saved {} {} for filmId: {}", saved, role, filmId);
    }

    /**
     * Save or get existing person.
     *
     * Urutan lookup:
     *  1. Cari by QID → jika ketemu, pakai yang ada (update jika perlu)
     *  2. Cari by slug → jika slug sudah dipakai person lain, PAKAI YANG ADA (jangan insert baru)
     *  3. Jika benar-benar baru → insert
     *
     * PENTING: Person dengan nama berbentuk QID (misal "Q4096812") tidak di-insert,
     * karena itu menandakan fetchPersonDetails gagal dari Wikidata.
     */
    private Long saveOrGetPerson(Person person) {
        if (person == null || person.getName() == null || person.getName().trim().isEmpty()) {
            return null;
        }

        // Guard: jika nama masih berbentuk QID (fetch gagal), jangan insert ke DB
        // karena akan menghasilkan data sampah seperti nama = "Q4096812"
        if (person.getName().matches("Q\\d+")) {
            log.warn("Skipping person with QID-as-name: {}", person.getName());
            return null;
        }

        if (person.getWikidataQid() == null || person.getWikidataQid().isEmpty()) {
            person.setWikidataQid("TEMP_" + person.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Lookup 1: by QID
        Person existingByQid = filmMapper.findPersonByQid(person.getWikidataQid());
        if (existingByQid != null) {
            if (needsPersonUpdate(person, existingByQid)) {
                updatePersonFields(existingByQid, person);
                filmMapper.updatePerson(existingByQid);
                log.debug("Person updated: {} ({})", person.getName(), person.getWikidataQid());
            }
            return existingByQid.getId();
        }

        // Lookup 2: by slug — jika sudah ada, pakai yang ada tanpa insert baru
        String candidateSlug = generateSlug(person.getName());
        Person existingBySlug = filmMapper.findPersonBySlug(candidateSlug);
        if (existingBySlug != null) {
            log.debug("Slug '{}' already exists, reusing person id={} for: {} ({})",
                    candidateSlug, existingBySlug.getId(), person.getName(), person.getWikidataQid());
            return existingBySlug.getId();
        }

        // Insert baru
        person.setSlug(candidateSlug);
        filmMapper.insertPerson(person);
        log.debug("Person inserted: {} ({})", person.getName(), person.getWikidataQid());
        return person.getId();
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

    private void saveCompanies(Long filmId, FilmDetail filmDetail) {
        saveProductionCompanies(filmId, filmDetail.getPerusahaanProduksi());
        saveDistributors(filmId, filmDetail.getDistributor());
    }

    private void saveProductionCompanies(Long filmId, List<Company> companies) {
        if (companies == null) return;

        int saved = 0;
        for (Company company : companies) {
            if (company == null) continue;
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
            if (company == null) continue;
            Long companyId = saveOrGetCompany(company);
            if (companyId != null) {
                filmMapper.insertFilmDistributor(filmId, companyId);
                saved++;
            }
        }
        log.debug("Saved {} distributors for filmId: {}", saved, filmId);
    }

    /**
     * Save or get existing company.
     * Logika identik dengan saveOrGetPerson — lookup by QID lalu by slug.
     * Company dengan nama berbentuk QID juga di-skip.
     */
    private Long saveOrGetCompany(Company company) {
        if (company == null || company.getName() == null || company.getName().trim().isEmpty()) {
            return null;
        }

        // Guard: jika nama masih berbentuk QID (fetch gagal), jangan insert ke DB
        if (company.getName().matches("Q\\d+")) {
            log.warn("Skipping company with QID-as-name: {}", company.getName());
            return null;
        }

        if (company.getWikidataQid() == null || company.getWikidataQid().isEmpty()) {
            company.setWikidataQid("TEMP_" + company.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Lookup 1: by QID
        Company existingByQid = filmMapper.findCompanyByQid(company.getWikidataQid());
        if (existingByQid != null) {
            if (needsCompanyUpdate(company, existingByQid)) {
                updateCompanyFields(existingByQid, company);
                filmMapper.updateCompany(existingByQid);
                log.debug("Company updated: {} ({})", company.getName(), company.getWikidataQid());
            }
            return existingByQid.getId();
        }

        // Lookup 2: by slug — jika sudah ada, pakai yang ada tanpa insert baru
        String candidateSlug = generateSlug(company.getName());
        Company existingBySlug = filmMapper.findCompanyBySlug(candidateSlug);
        if (existingBySlug != null) {
            log.debug("Slug '{}' already exists, reusing company id={} for: {} ({})",
                    candidateSlug, existingBySlug.getId(), company.getName(), company.getWikidataQid());
            return existingBySlug.getId();
        }

        // Insert baru
        company.setSlug(candidateSlug);
        filmMapper.insertCompany(company);
        log.debug("Company inserted: {} ({})", company.getName(), company.getWikidataQid());
        return company.getId();
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

    private void saveLocations(Long filmId, FilmDetail filmDetail) {
        int count = 0;
        if (filmDetail.getNarrativeLocation() != null) {
            for (String location : filmDetail.getNarrativeLocation()) {
                if (location == null || location.trim().isEmpty()) continue;
                filmMapper.insertLocation(filmId, "narrative", location);
                count++;
            }
        }
        if (filmDetail.getFilmingLocation() != null) {
            for (String location : filmDetail.getFilmingLocation()) {
                if (location == null || location.trim().isEmpty()) continue;
                filmMapper.insertLocation(filmId, "filming", location);
                count++;
            }
        }
        log.debug("Saved {} locations for filmId: {}", count, filmId);
    }

    private void saveBoxOffice(Long filmId, List<BoxOfficeData> boxOfficeList) {
        if (boxOfficeList == null) return;

        for (BoxOfficeData bo : boxOfficeList) {
            if (bo == null) continue;
            String region = (bo.getRegion() != null && !bo.getRegion().isBlank()) ? bo.getRegion() : "worldwide";
            filmMapper.insertBoxOffice(filmId, region, bo.getAmount(), bo.getCurrency());
        }
        log.debug("Saved {} box office entries for filmId: {}", boxOfficeList.size(), filmId);
    }

    private void saveReviews(Long filmId, List<ReviewScore> reviews) {
        if (reviews == null) return;

        int saved = 0;
        int skipped = 0;
        for (ReviewScore review : reviews) {
            if (review == null) continue;
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

    private void saveContentRatings(Long filmId, List<ContentRating> ratings) {
        if (ratings == null) return;

        int saved = 0;
        int skipped = 0;
        for (ContentRating rating : ratings) {
            if (rating == null) continue;
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

    private void saveAliases(Long filmId, List<String> aliases) {
        if (aliases == null) return;

        for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) continue;
            filmMapper.insertAlias(filmId, alias, "id");
        }
        log.debug("Saved {} aliases for filmId: {}", aliases.size(), filmId);
    }

    // ==================== UTILITY METHODS ====================

    private String cleanGenreName(String genre) {
        if (genre == null || genre.isEmpty()) return "";

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

    /**
     * Generate slug unik yang aman untuk film baru.
     *
     * Urutan logika:
     *  1. Base slug dari slugSource (sudah di-resolve ke EN oleh controller)
     *  2. Tambahkan suffix tahun jika ada
     *  3. Jika baseSlug kosong (misal input null/blank), fallback ke QID lowercase
     *  4. Jika slug sudah ada di DB (collision), tambah counter (-2, -3, dst)
     */
    private String generateUniqueSlugSafe(String slugSource, String tahunSuffix, String qid) {

        String baseSlug = generateSlug(slugSource);

        if (baseSlug.isBlank()) {
            log.warn("baseSlug kosong untuk input '{}', fallback ke QID: {}", slugSource, qid);
            baseSlug = qid.toLowerCase();
        }

        String candidate = (tahunSuffix != null && !tahunSuffix.isBlank())
                ? baseSlug + "-" + tahunSuffix
                : baseSlug;

        String finalSlug = candidate;
        int counter = 2;
        while (filmMapper.findBySlug(finalSlug) != null) {
            finalSlug = candidate + "-" + counter;
            counter++;
            if (counter > 999) {
                finalSlug = candidate + "-" + qid.toLowerCase();
                break;
            }
        }

        if (!finalSlug.equals(candidate)) {
            log.info("Slug collision pada '{}', menggunakan '{}'", candidate, finalSlug);
        }

        return finalSlug;
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