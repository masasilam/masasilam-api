package com.naskah.app.service.film.impl;

import com.naskah.app.exception.custom.DataNotFoundException;
import com.naskah.app.mapper.FilmMapper;
import com.naskah.app.mapper.FilmVideoSourceMapper;
import com.naskah.app.model.dto.request.AddFilmRequest;
import com.naskah.app.model.dto.request.AddFilmRequest.CompanyInput;
import com.naskah.app.model.dto.request.AddFilmRequest.PersonInput;
import com.naskah.app.model.dto.request.AddFilmRequest.VideoSourceInput;
import com.naskah.app.model.dto.request.UpdateFilmRequest;
import com.naskah.app.model.film.*;
import com.naskah.app.model.film.FilmDetail.*;
import com.naskah.app.service.film.FilmService;
import com.naskah.app.service.film.video.VideoProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmServiceImpl implements FilmService {

    @Autowired
    private FilmMapper filmMapper;
    @Autowired
    private FilmVideoSourceMapper filmVideoSourceMapper;
    @Autowired
    private VideoProviderService videoProviderService;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    private static final String DELIMITER = "||";
    private static final String SPLIT_REGEX = "\\|\\|";

    // ================================================================
    // Add
    // ================================================================

    @Override
    @Transactional
    public Film addFilm(AddFilmRequest req) {
        Film film = new Film();
        film.setWikidataQid("MANUAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        film.setJudul(req.getJudul());
        film.setTitleEng(req.getJudulSlug());
        film.setTahunRilis(req.getTahunRilis());
        film.setJenis(req.getJenis());
        film.setDeskripsi(req.getDeskripsi());
        film.setCatatan(req.getCatatan());
        film.setDurasi(req.getDurasi());
        film.setNegaraAsal(req.getNegaraAsal());
        film.setOriginalLanguage(req.getOriginalLanguage());
        film.setColor(req.getColor());
        film.setPosterUrl(req.getPosterUrl());
        film.setCopyrightStatusId(req.getCopyrightStatusId());
        film.setTrailerUrl(req.getTrailerUrl());
        film.setFollowedBy(req.getFollowedBy());
        film.setPartOfSeries(req.getPartOfSeries());
        film.setImageUrls(serialize(req.getImageUrls()));

        String slugSource = isValid(req.getJudulSlug()) ? req.getJudulSlug() : req.getJudul();
        String tahun = extractYear(req.getTahunRilis());
        film.setSlug(generateUniqueSlug(slugSource, tahun));

        filmMapper.insert(film);
        log.info("[Add] id={} slug={}", film.getId(), film.getSlug());

        saveRelations(film.getId(), req);
        return film;
    }

    // ================================================================
    // Update
    // ================================================================

    @Override
    @Transactional
    public Film updateFilm(String slug, UpdateFilmRequest req) {
        Film film = filmMapper.findBySlug(slug);
        if (film == null) throw new DataNotFoundException();

        if (isValid(req.getJudul())) film.setJudul(req.getJudul());
        if (isValid(req.getJudulSlug())) film.setTitleEng(req.getJudulSlug());
        if (isValid(req.getTahunRilis())) film.setTahunRilis(req.getTahunRilis());
        if (isValid(req.getJenis())) film.setJenis(req.getJenis());
        if (isValid(req.getDeskripsi())) film.setDeskripsi(req.getDeskripsi());
        if (isValid(req.getCatatan())) film.setCatatan(req.getCatatan());
        if (isValid(req.getDurasi())) film.setDurasi(req.getDurasi());
        if (isValid(req.getNegaraAsal())) film.setNegaraAsal(req.getNegaraAsal());
        if (isValid(req.getOriginalLanguage())) film.setOriginalLanguage(req.getOriginalLanguage());
        if (isValid(req.getColor())) film.setColor(req.getColor());
        if (req.getCopyrightStatusId() != null) film.setCopyrightStatusId(req.getCopyrightStatusId());
        if (isValid(req.getPosterUrl())) film.setPosterUrl(req.getPosterUrl());
        if (isValid(req.getTrailerUrl())) film.setTrailerUrl(req.getTrailerUrl());
        if (isValid(req.getFollowedBy())) film.setFollowedBy(req.getFollowedBy());
        if (isValid(req.getPartOfSeries())) film.setPartOfSeries(req.getPartOfSeries());
        if (req.getImageUrls() != null) film.setImageUrls(serialize(req.getImageUrls()));

        filmMapper.update(film);
        updateRelations(film.getId(), req);

        log.info("[Update] id={} slug={}", film.getId(), film.getSlug());
        return film;
    }

    // ================================================================
    // Delete
    // ================================================================

    @Override
    @Transactional
    public void deleteFilm(String slug) {
        Film film = filmMapper.findBySlug(slug);
        if (film == null) throw new DataNotFoundException();

        deleteAllRelations(film.getId());
        filmVideoSourceMapper.deleteByFilmId(film.getId());
        filmMapper.delete(film.getId());

        log.info("[Delete] id={} slug={}", film.getId(), slug);
    }

    // ================================================================
    // Read
    // ================================================================

    @Override
    public FilmDetail getFilmDetailBySlug(String slug) {
        Film film = filmMapper.findBySlug(slug);
        return film == null ? null : buildFilmDetail(film);
    }

    @Override
    public List<Film> getAllFilms(int page, int size) {
        return filmMapper.findAll(size, page * size);
    }

    @Override
    public int getTotalFilms() {
        return filmMapper.count();
    }

    @Override
    public List<Film> searchFilms(String query, int page, int size) {
        return filmMapper.search(query, size, page * size);
    }

    @Override
    public int getTotalSearchResults(String query) {
        return filmMapper.countSearch(query);
    }

    // ================================================================
    // buildFilmDetail
    // ================================================================

    private FilmDetail buildFilmDetail(Film film) {
        FilmDetail detail = new FilmDetail();

        detail.setId(film.getId());
        detail.setWikidataQid(film.getWikidataQid());
        detail.setSlug(film.getSlug());
        detail.setJudul(film.getJudul());
        detail.setJudulSlug(film.getTitleEng());
        detail.setTahunRilis(film.getTahunRilis());
        detail.setJenis(film.getJenis());
        detail.setDeskripsi(film.getDeskripsi());
        detail.setCatatan(film.getCatatan());
        detail.setDurasi(film.getDurasi());
        detail.setNegaraAsal(film.getNegaraAsal());
        detail.setOriginalLanguage(film.getOriginalLanguage());
        detail.setColor(film.getColor());
        detail.setPosterUrl(film.getPosterUrl());
        detail.setCopyrightStatusId(film.getCopyrightStatusId());
        detail.setTrailerUrl(film.getTrailerUrl());
        detail.setFollowedBy(film.getFollowedBy());
        detail.setPartOfSeries(film.getPartOfSeries());
        detail.setImageUrls(deserialize(film.getImageUrls()));

        // Relasi — semua field Person & Company sudah include photo/logo/description
        detail.setGenre(filmMapper.findGenresByFilmId(film.getId()));
        detail.setSutradara(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "director"));
        detail.setPenulisSkenario(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "writer"));
        detail.setPemeran(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "actor"));
        detail.setNarator(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "narrator"));
        detail.setProduser(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "producer"));
        detail.setFilmEditor(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "editor"));
        detail.setCinematographer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "cinematographer"));
        detail.setComposer(filmMapper.findPersonsByFilmIdAndRole(film.getId(), "composer"));
        detail.setPerusahaanProduksi(filmMapper.findProductionCompaniesByFilmId(film.getId()));
        detail.setDistributor(filmMapper.findDistributorsByFilmId(film.getId()));
        detail.setNarrativeLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "narrative"));
        detail.setFilmingLocation(filmMapper.findLocationsByFilmIdAndType(film.getId(), "filming"));
        detail.setAliasIndonesia(filmMapper.findAliasesByFilmIdAndLanguage(film.getId(), "id"));

        // Video sources
        List<FilmVideoSource> sources = filmVideoSourceMapper.findActiveByFilmId(film.getId());
        detail.setVideoSources(sources.stream().map(vs -> {
            VideoSource v = new VideoSource();
            v.setRawUrl(vs.getRawUrl());
            v.setProviderType(vs.getProviderType());
            v.setEmbedUrl(vs.getEmbedUrl());
            v.setDirectUrl(vs.getDirectUrl());
            v.setThumbnailUrl(vs.getThumbnailUrl());
            v.setTitle(vs.getTitle());
            v.setDurationSeconds(vs.getDurationSeconds());
            v.setIsTrailer(vs.getIsTrailer());
            v.setPriority(vs.getPriority());
            return v;
        }).toList());

        return detail;
    }

    // ================================================================
    // Save relations
    // ================================================================

    private void saveRelations(Long filmId, AddFilmRequest req) {
        saveGenres(filmId, req.getGenre());
        savePersons(filmId, req.getSutradara(), "director");
        savePersons(filmId, req.getPenulisSkenario(), "writer");
        savePersons(filmId, req.getPemeran(), "actor");
        savePersons(filmId, req.getProduser(), "producer");
        savePersons(filmId, req.getFilmEditor(), "editor");
        savePersons(filmId, req.getCinematographer(), "cinematographer");
        savePersons(filmId, req.getComposer(), "composer");
        savePersons(filmId, req.getNarator(), "narrator");
        saveCompanies(filmId, req.getPerusahaanProduksi(), "production");
        saveCompanies(filmId, req.getDistributor(), "distributor");
        saveLocations(filmId, req.getNarrativeLocation(), "narrative");
        saveLocations(filmId, req.getFilmingLocation(), "filming");
        saveAliases(filmId, req.getAliasIndonesia());
        saveVideoSources(filmId, req.getVideoSources());
    }

    private void updateRelations(Long filmId, UpdateFilmRequest req) {
        if (req.getGenre() != null) {
            filmMapper.deleteGenresByFilmId(filmId);
            saveGenres(filmId, req.getGenre());
        }
        if (req.getSutradara() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "director");
            savePersons(filmId, req.getSutradara(), "director");
        }
        if (req.getPenulisSkenario() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "writer");
            savePersons(filmId, req.getPenulisSkenario(), "writer");
        }
        if (req.getPemeran() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "actor");
            savePersons(filmId, req.getPemeran(), "actor");
        }
        if (req.getProduser() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "producer");
            savePersons(filmId, req.getProduser(), "producer");
        }
        if (req.getFilmEditor() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "editor");
            savePersons(filmId, req.getFilmEditor(), "editor");
        }
        if (req.getCinematographer() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "cinematographer");
            savePersons(filmId, req.getCinematographer(), "cinematographer");
        }
        if (req.getComposer() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "composer");
            savePersons(filmId, req.getComposer(), "composer");
        }
        if (req.getNarator() != null) {
            filmMapper.deletePersonsByFilmIdAndRole(filmId, "narrator");
            savePersons(filmId, req.getNarator(), "narrator");
        }
        if (req.getPerusahaanProduksi() != null) {
            filmMapper.deleteProductionCompaniesByFilmId(filmId);
            saveCompanies(filmId, req.getPerusahaanProduksi(), "production");
        }
        if (req.getDistributor() != null) {
            filmMapper.deleteDistributorsByFilmId(filmId);
            saveCompanies(filmId, req.getDistributor(), "distributor");
        }
        if (req.getNarrativeLocation() != null) {
            filmMapper.deleteLocationsByFilmId(filmId);
            saveLocations(filmId, req.getNarrativeLocation(), "narrative");
        }
        if (req.getFilmingLocation() != null) {
            saveLocations(filmId, req.getFilmingLocation(), "filming");
        }
        if (req.getAliasIndonesia() != null) {
            filmMapper.deleteAliasesByFilmId(filmId);
            saveAliases(filmId, req.getAliasIndonesia());
        }
        if (req.getVideoSources() != null) {
            filmVideoSourceMapper.deleteByFilmId(filmId);
            saveVideoSources(filmId, req.getVideoSources());
        }
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
    }

    // ================================================================
    // Save helpers
    // ================================================================

    private void saveGenres(Long filmId, List<String> genres) {
        if (genres == null) return;
        for (String genre : genres) {
            if (isValid(genre)) filmMapper.insertGenre(filmId, genre.trim());
        }
    }

    /**
     * Simpan / upsert Person lalu link ke film.
     * Jika person sudah ada (by slug), update photo & description jika ada nilai baru.
     */
    private void savePersons(Long filmId, List<PersonInput> inputs, String role) {
        if (inputs == null) return;
        for (PersonInput input : inputs) {
            if (input == null || !isValid(input.getName())) continue;

            String slug = generateSlug(input.getName());
            Person existing = filmMapper.findPersonBySlug(slug);

            if (existing != null) {
                boolean changed = false;
                if (isValid(input.getPhotoUrl()) && !input.getPhotoUrl().equals(existing.getPhotoUrl())) {
                    existing.setPhotoUrl(input.getPhotoUrl());
                    changed = true;
                }
                if (isValid(input.getDescription()) && !input.getDescription().equals(existing.getDescription())) {
                    existing.setDescription(input.getDescription());
                    changed = true;
                }
                if (changed) filmMapper.updatePerson(existing);
                filmMapper.insertFilmPerson(filmId, existing.getId(), role);
            } else {
                Person p = new Person();
                p.setName(input.getName().trim());
                p.setSlug(slug);
                p.setWikidataQid("MANUAL_" + slug);
                p.setPhotoUrl(input.getPhotoUrl());
                p.setDescription(input.getDescription());
                filmMapper.insertPerson(p);
                filmMapper.insertFilmPerson(filmId, p.getId(), role);
            }
        }
    }

    /**
     * Simpan / upsert Company lalu link ke film.
     * Jika company sudah ada (by slug), update logo & description jika ada nilai baru.
     */
    private void saveCompanies(Long filmId, List<CompanyInput> inputs, String type) {
        if (inputs == null) return;
        for (CompanyInput input : inputs) {
            if (input == null || !isValid(input.getName())) continue;

            String slug = generateSlug(input.getName());
            Company existing = filmMapper.findCompanyBySlug(slug);

            if (existing != null) {
                boolean changed = false;
                if (isValid(input.getLogoUrl()) && !input.getLogoUrl().equals(existing.getLogoUrl())) {
                    existing.setLogoUrl(input.getLogoUrl());
                    changed = true;
                }
                if (isValid(input.getDescription()) && !input.getDescription().equals(existing.getDescription())) {
                    existing.setDescription(input.getDescription());
                    changed = true;
                }
                if (changed) filmMapper.updateCompany(existing);
                linkCompany(filmId, existing.getId(), type);
            } else {
                Company c = new Company();
                c.setName(input.getName().trim());
                c.setSlug(slug);
                c.setWikidataQid("MANUAL_" + slug);
                c.setLogoUrl(input.getLogoUrl());
                c.setDescription(input.getDescription());
                filmMapper.insertCompany(c);
                linkCompany(filmId, c.getId(), type);
            }
        }
    }

    private void linkCompany(Long filmId, Long companyId, String type) {
        if ("production".equals(type)) filmMapper.insertFilmProductionCompany(filmId, companyId);
        else filmMapper.insertFilmDistributor(filmId, companyId);
    }

    private void saveLocations(Long filmId, List<String> locations, String type) {
        if (locations == null) return;
        for (String loc : locations) {
            if (isValid(loc)) filmMapper.insertLocation(filmId, type, loc.trim());
        }
    }

    private void saveAliases(Long filmId, List<String> aliases) {
        if (aliases == null) return;
        for (String alias : aliases) {
            if (isValid(alias)) filmMapper.insertAlias(filmId, alias.trim(), "id");
        }
    }

    private void saveVideoSources(Long filmId, List<VideoSourceInput> inputs) {
        if (inputs == null) return;
        for (VideoSourceInput input : inputs) {
            if (!isValid(input.getUrl())) continue;
            FilmVideoSource source = videoProviderService.resolveToEntity(
                    input.getUrl(), filmId, input.getIsTrailer()
            );
            if (source != null) {
                source.setPriority(input.getPriority());
                filmVideoSourceMapper.insert(source);
            }
        }
    }

    // ================================================================
    // Utility
    // ================================================================

    private boolean isValid(String text) {
        return text != null && !text.isBlank();
    }

    private String extractYear(String tahunRilis) {
        if (!isValid(tahunRilis)) return null;
        String digits = tahunRilis.replaceAll("[^\\d]", "");
        return digits.length() >= 4 ? digits.substring(0, 4) : null;
    }

    private String serialize(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return urls.stream()
                .filter(u -> u != null && !u.isBlank())
                .collect(Collectors.joining(DELIMITER));
    }

    private List<String> deserialize(String raw) {
        if (!isValid(raw)) return new ArrayList<>();
        return Arrays.stream(raw.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String generateSlug(String input) {
        if (!isValid(input)) return "";
        String slug = input.toLowerCase(Locale.ENGLISH);
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        return slug.replaceAll("-+", "-");
    }

    private String generateUniqueSlug(String source, String tahun) {
        String base = generateSlug(source);
        String candidate = isValid(tahun) ? base + "-" + tahun : base;
        String result = candidate;
        int counter = 2;
        while (filmMapper.findBySlug(result) != null) {
            result = candidate + "-" + counter++;
            if (counter > 999) {
                result = candidate + "-" + UUID.randomUUID().toString().substring(0, 6);
                break;
            }
        }
        return result;
    }
}