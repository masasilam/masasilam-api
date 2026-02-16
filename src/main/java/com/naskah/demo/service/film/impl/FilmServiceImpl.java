package com.naskah.demo.service.film.impl;

import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.Company;
import com.naskah.demo.model.film.Film;
import com.naskah.demo.model.film.FilmDetail;
import com.naskah.demo.model.film.Person;
import com.naskah.demo.service.film.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FilmServiceImpl implements FilmService {

    @Autowired
    private FilmMapper filmMapper;

    // Slug generation patterns
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");

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
        Film existing = filmMapper.findByQid(filmDetail.getWikidataQid());

        Film film = new Film();
        film.setWikidataQid(filmDetail.getWikidataQid());

        // Generate slug for film
        String filmSlug = generateUniqueSlug(
                filmDetail.getJudul(),
                filmDetail.getTahunRilis() != null ? filmDetail.getTahunRilis().substring(0, 4) : null
        );
        film.setSlug(filmSlug);
        filmDetail.setSlug(filmSlug);

        film.setJudul(filmDetail.getJudul());
        film.setTahunRilis(filmDetail.getTahunRilis());
        film.setJenis(filmDetail.getJenis());
        film.setDeskripsi(filmDetail.getDeskripsi());
        film.setDurasi(filmDetail.getDurasi());
        film.setNegaraAsal(filmDetail.getNegaraAsal());
        film.setPosterUrl(filmDetail.getPosterUrl());
        film.setVideoUrl(filmDetail.getVideoUrl());
        film.setSubtitleUrl(filmDetail.getSubtitleUrl());

        if (existing == null) {
            filmMapper.insert(film);
        } else {
            film.setId(existing.getId());
            filmMapper.update(film);
            filmMapper.deleteGenresByFilmId(film.getId());
            filmMapper.deletePersonsByFilmId(film.getId());
            filmMapper.deleteProductionCompaniesByFilmId(film.getId());
            filmMapper.deleteAliasesByFilmId(film.getId());
        }

        // Insert genres (clean genre names by removing "film" suffix)
        if (filmDetail.getGenre() != null) {
            for (String genre : filmDetail.getGenre()) {
                String cleanGenre = cleanGenreName(genre);
                filmMapper.insertGenre(film.getId(), cleanGenre);
            }
        }

        // Insert persons with full details
        if (filmDetail.getSutradara() != null) {
            for (Person director : filmDetail.getSutradara()) {
                Long personId = saveOrGetPerson(director);
                if (personId != null) {
                    filmMapper.insertFilmPerson(film.getId(), personId, "director");
                }
            }
        }

        if (filmDetail.getPenulisSkenario() != null) {
            for (Person writer : filmDetail.getPenulisSkenario()) {
                Long personId = saveOrGetPerson(writer);
                if (personId != null) {
                    filmMapper.insertFilmPerson(film.getId(), personId, "writer");
                }
            }
        }

        if (filmDetail.getPemeran() != null) {
            for (Person actor : filmDetail.getPemeran()) {
                Long personId = saveOrGetPerson(actor);
                if (personId != null) {
                    filmMapper.insertFilmPerson(film.getId(), personId, "actor");
                }
            }
        }

        if (filmDetail.getProduser() != null) {
            for (Person producer : filmDetail.getProduser()) {
                Long personId = saveOrGetPerson(producer);
                if (personId != null) {
                    filmMapper.insertFilmPerson(film.getId(), personId, "producer");
                }
            }
        }

        // Insert companies with full details
        if (filmDetail.getPerusahaanProduksi() != null) {
            for (Company company : filmDetail.getPerusahaanProduksi()) {
                Long companyId = saveOrGetCompany(company);
                if (companyId != null) {
                    filmMapper.insertFilmProductionCompany(film.getId(), companyId);
                }
            }
        }

        if (filmDetail.getAliasIndonesia() != null) {
            for (String alias : filmDetail.getAliasIndonesia()) {
                filmMapper.insertAlias(film.getId(), alias, "id");
            }
        }

        return film;
    }

    private Long saveOrGetPerson(Person person) {
        if (person == null || person.getName() == null) {
            return null;
        }

        if (person.getWikidataQid() == null || person.getWikidataQid().isEmpty()) {
            person.setWikidataQid("TEMP_" + person.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Generate slug for person
        if (person.getSlug() == null || person.getSlug().isEmpty()) {
            person.setSlug(generateSlug(person.getName()));
        }

        Person existing = filmMapper.findPersonByQid(person.getWikidataQid());

        if (existing == null) {
            filmMapper.insertPerson(person);
            return person.getId();
        } else {
            boolean needsUpdate = false;

            if (person.getName() != null && !person.getName().equals(existing.getName())) {
                existing.setName(person.getName());
                needsUpdate = true;
            }
            if (person.getSlug() != null && !person.getSlug().equals(existing.getSlug())) {
                existing.setSlug(person.getSlug());
                needsUpdate = true;
            }
            if (person.getPhotoUrl() != null && !person.getPhotoUrl().equals(existing.getPhotoUrl())) {
                existing.setPhotoUrl(person.getPhotoUrl());
                needsUpdate = true;
            }
            if (person.getDescription() != null && !person.getDescription().equals(existing.getDescription())) {
                existing.setDescription(person.getDescription());
                needsUpdate = true;
            }

            if (needsUpdate) {
                filmMapper.updatePerson(existing);
            }

            return existing.getId();
        }
    }

    private Long saveOrGetCompany(Company company) {
        if (company == null || company.getName() == null) {
            return null;
        }

        if (company.getWikidataQid() == null || company.getWikidataQid().isEmpty()) {
            company.setWikidataQid("TEMP_" + company.getName().replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // Generate slug for company
        if (company.getSlug() == null || company.getSlug().isEmpty()) {
            company.setSlug(generateSlug(company.getName()));
        }

        Company existing = filmMapper.findCompanyByQid(company.getWikidataQid());

        if (existing == null) {
            filmMapper.insertCompany(company);
            return company.getId();
        } else {
            boolean needsUpdate = false;

            if (company.getName() != null && !company.getName().equals(existing.getName())) {
                existing.setName(company.getName());
                needsUpdate = true;
            }
            if (company.getSlug() != null && !company.getSlug().equals(existing.getSlug())) {
                existing.setSlug(company.getSlug());
                needsUpdate = true;
            }
            if (company.getLogoUrl() != null && !company.getLogoUrl().equals(existing.getLogoUrl())) {
                existing.setLogoUrl(company.getLogoUrl());
                needsUpdate = true;
            }
            if (company.getDescription() != null && !company.getDescription().equals(existing.getDescription())) {
                existing.setDescription(company.getDescription());
                needsUpdate = true;
            }

            if (needsUpdate) {
                filmMapper.updateCompany(existing);
            }

            return existing.getId();
        }
    }

    private FilmDetail buildFilmDetail(Film film) {
        FilmDetail detail = new FilmDetail();
        detail.setId(film.getId());
        detail.setWikidataQid(film.getWikidataQid());
        detail.setSlug(film.getSlug());
        detail.setJudul(film.getJudul());
        detail.setTahunRilis(film.getTahunRilis());
        detail.setJenis(film.getJenis());
        detail.setDeskripsi(film.getDeskripsi());
        detail.setDurasi(film.getDurasi());
        detail.setNegaraAsal(film.getNegaraAsal());
        detail.setPosterUrl(film.getPosterUrl());
        detail.setVideoUrl(film.getVideoUrl());
        detail.setSubtitleUrl(film.getSubtitleUrl());

        // Get genres and clean them (no cleaning needed when reading from DB as they're already clean)
        detail.setGenre(filmMapper.findGenresByFilmId(film.getId()));
        detail.setSutradara(filmMapper.findPersonObjectsByFilmIdAndRole(film.getId(), "director"));
        detail.setPenulisSkenario(filmMapper.findPersonObjectsByFilmIdAndRole(film.getId(), "writer"));
        detail.setPemeran(filmMapper.findPersonObjectsByFilmIdAndRole(film.getId(), "actor"));
        detail.setProduser(filmMapper.findPersonObjectsByFilmIdAndRole(film.getId(), "producer"));
        detail.setPerusahaanProduksi(filmMapper.findCompanyObjectsByFilmId(film.getId()));
        detail.setAliasIndonesia(filmMapper.findAliasesByFilmIdAndLanguage(film.getId(), "id"));

        return detail;
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

    private String cleanGenreName(String genre) {
        if (genre == null || genre.isEmpty()) {
            return genre;
        }

        // Remove " film" suffix (case insensitive)
        String cleaned = genre.trim()
                .replaceAll("(?i)\\s+films?$", "")  // Remove " film" or " films" at the end
                .trim();

        // Capitalize first letter
        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }

        return cleaned;
    }

    private String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String slug = input.toLowerCase(Locale.ENGLISH);

        // Normalize unicode characters (remove accents)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);

        // Replace whitespace with dashes
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Remove non-latin characters except dashes
        slug = NON_LATIN.matcher(slug).replaceAll("");

        // Remove dashes from edges
        slug = EDGES_DASHES.matcher(slug).replaceAll("");

        // Replace multiple consecutive dashes with single dash
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
}