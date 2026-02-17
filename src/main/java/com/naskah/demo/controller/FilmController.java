package com.naskah.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.*;
import com.naskah.demo.model.film.FilmDetail.*;
import com.naskah.demo.service.film.FilmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.*;
import java.util.Arrays;

/**
 * FilmController - REST controller for film operations
 */
@Slf4j
@RestController
@RequestMapping("/api/films")
@CrossOrigin(origins = "*")
public class FilmController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FilmService filmService;

    @Autowired
    private FilmMapper filmMapper;

    // ==================== HEADERS ====================

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "FilmApp/1.0 (Enhanced Metadata Extractor)");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ==================== SPECIFIC ROUTES FIRST ====================

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchFilms(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Film> films = filmService.searchFilms(q, page, size);
        int total = filmService.getTotalSearchResults(q);

        Map<String, Object> response = new HashMap<>();
        response.put("films", films);
        response.put("query", q);
        response.put("currentPage", page);
        response.put("totalItems", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/person/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Person> getPersonBySlug(@PathVariable String slug) {
        Person person = filmMapper.findPersonBySlug(slug);
        if (person == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(person);
    }

    @GetMapping(value = "/company/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Company> getCompanyBySlug(@PathVariable String slug) {
        Company company = filmMapper.findCompanyBySlug(slug);
        if (company == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(company);
    }

    /**
     * GET film from Wikidata and save to database
     */
    @GetMapping(value = "/wikidata/{qid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmFromWikidata(@PathVariable String qid) {
        try {
            log.info("=== FETCHING COMPLETE METADATA FOR: {} ===", qid);

            FilmDetail cached = filmService.getFilmDetailByQid(qid);
            if (cached != null) {
                log.info("Found in cache: {}", qid);
                return ResponseEntity.ok(cached);
            }

            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode entityNode = root.path("entities").path(qid);

            FilmDetail filmDetail = new FilmDetail();

            // ==================== BASIC INFO ====================
            filmDetail.setWikidataQid(qid);
            filmDetail.setJudul(getLabel(entityNode));
            filmDetail.setTahunRilis(extractDate(entityNode));
            filmDetail.setJenis(extractEntityLabel(entityNode, "P31"));
            filmDetail.setGenre(extractGenres(entityNode));

            // Deskripsi: idwiki → enwiki + translate → fallback Wikidata
            String synopsis = extractWikipediaSynopsisIndonesian(entityNode);
            if (synopsis != null && !synopsis.isEmpty()) {
                filmDetail.setDeskripsi(synopsis);
                log.info("✓ Deskripsi Bahasa Indonesia berhasil untuk: {}", qid);
            } else {
                filmDetail.setDeskripsi(getDescription(entityNode));
                log.warn("Fallback ke Wikidata description untuk: {}", qid);
            }
            log.debug("✓ Basic info extracted");

            // ==================== PEOPLE ====================
            filmDetail.setSutradara(extractPersonsWithDetails(entityNode, "P57"));
            filmDetail.setPenulisSkenario(extractPersonsWithDetails(entityNode, "P58"));
            filmDetail.setPemeran(extractPersonsWithDetails(entityNode, "P161"));
            filmDetail.setProduser(extractPersonsWithDetails(entityNode, "P162"));
            filmDetail.setFilmEditor(extractPersonsWithDetails(entityNode, "P1040"));
            filmDetail.setCinematographer(extractPersonsWithDetails(entityNode, "P344"));
            filmDetail.setComposer(extractPersonsWithDetails(entityNode, "P86"));
            log.debug("✓ Crew extracted");

            // ==================== COMPANIES ====================
            filmDetail.setPerusahaanProduksi(extractCompaniesWithDetails(entityNode, "P272"));
            filmDetail.setDistributor(extractCompaniesWithDetails(entityNode, "P750"));
            log.debug("✓ Companies extracted");

            // ==================== LOCATIONS ====================
            filmDetail.setNegaraAsal(extractEntityLabel(entityNode, "P495"));
            filmDetail.setNarrativeLocation(extractEntityLabels(entityNode, "P840"));
            filmDetail.setFilmingLocation(extractEntityLabels(entityNode, "P915"));
            filmDetail.setDurasi(extractDuration(entityNode));
            log.debug("✓ Locations extracted");

            // ==================== TECHNICAL INFO ====================
            filmDetail.setColor(extractColor(entityNode));
            filmDetail.setOriginalLanguage(extractOriginalLanguage(entityNode));
            filmDetail.setPosterUrl(extractPosterUrl(entityNode));
            filmDetail.setImageUrls(extractImageUrls(entityNode));

            filmDetail.setVideoUrl(extractVideoUrl(entityNode));
            filmDetail.setTrailerUrl(extractTrailerUrl(entityNode));
            filmDetail.setSubtitleUrl(extractSubtitleUrl(filmDetail.getVideoUrl()));
            log.debug("✓ Technical info: poster={}, images={}, video={}, trailer={}",
                    filmDetail.getPosterUrl() != null,
                    filmDetail.getImageUrls() != null ? filmDetail.getImageUrls().size() : 0,
                    filmDetail.getVideoUrl() != null,
                    filmDetail.getTrailerUrl() != null);

            // ==================== FINANCIAL ====================
            filmDetail.setBudget(extractBudget(entityNode));
            filmDetail.setBoxOffice(extractBoxOffice(entityNode));
            log.debug("✓ Financial data extracted");

            // ==================== RATINGS ====================
            filmDetail.setReviewScores(extractReviewScores(entityNode));
            filmDetail.setContentRatings(extractContentRatings(entityNode));
            log.debug("✓ Ratings extracted");

            // ==================== RELATIONS ====================
            filmDetail.setFollowedBy(extractFollowedBy(entityNode));
            filmDetail.setPartOfSeries(extractPartOfSeries(entityNode));
            filmDetail.setAliasIndonesia(extractAliases(entityNode));
            log.debug("✓ Relations & aliases extracted");

            log.info("=== EXTRACTION COMPLETE: {} ===", qid);

            Film savedFilm = filmService.saveFilm(filmDetail);
            filmDetail.setId(savedFilm.getId());

            return ResponseEntity.ok(filmDetail);

        } catch (Exception e) {
            log.error("ERROR extracting metadata for QID: {}", qid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping(value = "/{filmSlug}/subtitle", produces = "text/vtt")
    public ResponseEntity<String> getSubtitle(@PathVariable String filmSlug) {
        try {
            log.info("Subtitle request for slug: {}", filmSlug);
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getSubtitleUrl() == null) {
                log.warn("Subtitle not found for slug: {}", filmSlug);
                return ResponseEntity.notFound().build();
            }

            String rawUrl = parseSubtitleUrl(film.getSubtitleUrl());
            if (rawUrl == null) return ResponseEntity.notFound().build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Backend Proxy)");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(rawUrl, HttpMethod.GET, entity, String.class);

            String subtitleContent = response.getBody();
            if (subtitleContent == null || subtitleContent.isEmpty()) return ResponseEntity.notFound().build();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.valueOf("text/vtt; charset=UTF-8"));
            responseHeaders.set("Cache-Control", "public, max-age=86400");
            return new ResponseEntity<>(convertSrtToVtt(subtitleContent), responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error fetching subtitle for slug: {}", filmSlug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{filmSlug}/video-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoInfo(@PathVariable String filmSlug) {
        try {
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getVideoUrl() == null) return ResponseEntity.notFound().build();

            String filename = extractFilename(film.getVideoUrl());
            if (filename == null) return ResponseEntity.badRequest().build();

            String apiUrl = UriComponentsBuilder
                    .fromHttpUrl("https://commons.wikimedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("titles", "File:" + filename)
                    .queryParam("prop", "videoinfo")
                    .queryParam("viprop", "derivatives|url|size|mediatype")
                    .queryParam("format", "json")
                    .build().toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Video Info Request)");
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode pages = root.path("query").path("pages");
            if (!pages.elements().hasNext()) return ResponseEntity.notFound().build();

            JsonNode info = pages.elements().next().path("videoinfo").get(0);
            if (info == null) return ResponseEntity.notFound().build();

            Map<String, Object> result = new HashMap<>();
            result.put("originalUrl", info.path("url").asText());
            result.put("originalSize", info.path("size").asLong());
            result.put("mediaType", info.path("mediatype").asText());

            List<Map<String, Object>> qualities = new ArrayList<>();
            for (JsonNode d : info.path("derivatives")) {
                Map<String, Object> q = new HashMap<>();
                q.put("type", d.path("type").asText());
                q.put("src", d.path("src").asText());
                q.put("width", d.path("width").asInt());
                q.put("height", d.path("height").asInt());
                q.put("label", d.path("height").asInt() + "p");
                qualities.add(q);
            }
            qualities.sort((a, b) -> Integer.compare((int) b.get("height"), (int) a.get("height")));
            result.put("qualities", qualities);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching video info for slug: {}", filmSlug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllFilms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<Film> films = filmService.getAllFilms(page, size);
        int total = filmService.getTotalFilms();

        Map<String, Object> response = new HashMap<>();
        response.put("films", films);
        response.put("currentPage", page);
        response.put("totalItems", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(response);
    }

    /** Harus paling bawah — catch-all */
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmBySlug(@PathVariable String slug) {
        FilmDetail film = filmService.getFilmDetailBySlug(slug);
        if (film == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(film);
    }

    // ==================== WIKIPEDIA + TRANSLATION ====================

    /**
     * Strategi bertingkat untuk mendapat sinopsis Bahasa Indonesia:
     * 1. idwiki → ambil dari Wikipedia BI (sudah BI, tidak perlu translate)
     * 2. enwiki → ambil dari Wikipedia EN, lalu translate ke BI via MyMemory
     * 3. null → caller fallback ke Wikidata description
     */
    private String extractWikipediaSynopsisIndonesian(JsonNode entity) {
        JsonNode sitelinks = entity.path("sitelinks");

        log.debug("Sitelinks tersedia: idwiki={}, enwiki={}",
                sitelinks.has("idwiki"), sitelinks.has("enwiki"));

        // Step 1: Indonesian Wikipedia
        if (sitelinks.has("idwiki")) {
            String pageTitle = sitelinks.path("idwiki").path("title").asText();
            log.info("Mencoba Wikipedia BI dengan judul: {}", pageTitle);
            String idSynopsis = fetchWikipediaSummary(pageTitle, "id");
            if (idSynopsis != null && !idSynopsis.isEmpty()) {
                log.info("✓ Sinopsis dari Wikipedia BI ({}chars)", idSynopsis.length());
                return idSynopsis;
            }
            log.warn("Wikipedia BI gagal untuk: {}", pageTitle);
        }

        // Step 2: English Wikipedia + translate
        if (sitelinks.has("enwiki")) {
            String pageTitle = sitelinks.path("enwiki").path("title").asText();
            log.info("Mencoba Wikipedia EN dengan judul: {}", pageTitle);
            String enSynopsis = fetchWikipediaSummary(pageTitle, "en");
            if (enSynopsis != null && !enSynopsis.isEmpty()) {
                log.info("✓ Wikipedia EN berhasil ({}chars), menerjemahkan...", enSynopsis.length());
                String translated = translateToIndonesian(enSynopsis);
                if (translated != null && !translated.isEmpty()) {
                    log.info("✓ Terjemahan berhasil ({}chars)", translated.length());
                    return translated;
                }
                log.warn("Terjemahan gagal, kembalikan versi EN sebagai fallback");
                return enSynopsis;
            }
            log.warn("Wikipedia EN gagal untuk: {}", pageTitle);
        }

        log.warn("Tidak ada sitelink Wikipedia yang berhasil diambil");
        return null;
    }

    /**
     * Fetch summary dari Wikipedia REST API.
     *
     * PENTING: Wikipedia REST API menggunakan format URL:
     *   https://{lang}.wikipedia.org/api/rest_v1/page/summary/{title}
     * Di mana {title} menggunakan underscore (_) untuk spasi,
     * dan karakter khusus seperti kurung () harus di-encode.
     *
     * Contoh: "Charade (1963 film)" → "Charade_(1963_film)"
     *         Kurung tidak perlu di-encode karena Wikipedia REST API menerimanya.
     */
    private String fetchWikipediaSummary(String pageTitle, String lang) {
        try {
            // Ganti spasi dengan underscore (standar Wikipedia URL)
            // URLEncoder mengubah spasi jadi '+', tapi Wikipedia butuh '_'
            String normalizedTitle = pageTitle.replace(" ", "_");

            // Encode karakter khusus KECUALI underscore dan karakter URL-safe
            // Gunakan URI encoding standar
            String encodedTitle = URLEncoder.encode(normalizedTitle, StandardCharsets.UTF_8)
                    .replace("+", "_")    // URLEncoder ubah spasi jadi '+', kita butuh '_'
                    .replace("%28", "(")  // Kembalikan kurung buka — Wikipedia REST API menerimanya
                    .replace("%29", ")"); // Kembalikan kurung tutup

            String wikiUrl = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/" + encodedTitle;
            log.debug("Fetching Wikipedia URL: {}", wikiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Wikipedia Summary Fetcher)");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    wikiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode wikiRoot = mapper.readTree(response.getBody());

                // Cek apakah ini halaman "missing" atau redirect yang salah
                String type = wikiRoot.path("type").asText("");
                if ("https://mediawiki.org/wiki/HyperSwitch/errors/not_found".equals(type)) {
                    log.warn("Wikipedia halaman tidak ditemukan untuk: {}", pageTitle);
                    return null;
                }

                String extract = wikiRoot.path("extract").asText();
                if (!extract.isEmpty()) {
                    log.debug("Wikipedia extract fetched: {}chars", extract.length());
                    return extract;
                }
            }

        } catch (Exception e) {
            log.warn("Wikipedia ({}) fetch gagal untuk '{}': {}", lang, pageTitle, e.getMessage());
        }
        return null;
    }

    /**
     * Translate teks EN → ID menggunakan MyMemory API (gratis, tanpa API key).
     *
     * Limit: 5000 karakter/request, ~100 request/hari (anonymous).
     * Untuk meningkatkan limit: tambahkan "&de=email@kamu.com" ke URL
     * agar dapat 10.000 request/hari.
     *
     * Teks > 4500 karakter otomatis dipotong di batas kalimat.
     */
    private String translateToIndonesian(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) return null;

        try {
            // Potong di batas kalimat jika > 4500 chars (safety margin dari limit 5000)
            String textToTranslate = englishText;
            if (textToTranslate.length() > 4500) {
                int cutPoint = textToTranslate.lastIndexOf(". ", 4500);
                textToTranslate = cutPoint > 0
                        ? textToTranslate.substring(0, cutPoint + 1)
                        : textToTranslate.substring(0, 4500);
                log.debug("Teks dipotong: {} → {} chars", englishText.length(), textToTranslate.length());
            }

            String encodedText = URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8);
            String translateUrl = "https://api.mymemory.translated.net/get"
                    + "?q=" + encodedText
                    + "&langpair=en|id";

            log.debug("Mengirim ke MyMemory API ({} chars)...", textToTranslate.length());

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Translation Service)");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    translateUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode translateRoot = mapper.readTree(response.getBody());

                int responseStatus = translateRoot.path("responseStatus").asInt();
                if (responseStatus != 200) {
                    log.warn("MyMemory API status: {}, detail: {}",
                            responseStatus,
                            translateRoot.path("responseDetails").asText());
                    return null;
                }

                String translated = translateRoot.path("responseData").path("translatedText").asText();
                if (!translated.isEmpty() && !translated.equalsIgnoreCase(textToTranslate)) {
                    return translated;
                }
                log.warn("MyMemory mengembalikan teks yang sama (tidak diterjemahkan)");
            }

        } catch (Exception e) {
            log.warn("Terjemahan gagal: {}", e.getMessage());
        }

        return null;
    }

    // ==================== IMAGE EXTRACTION METHODS ====================

    // QID Wikidata untuk role "film poster" di qualifier P3831.
    // Q429785  = "film poster" (label EN)
    // Q14660174 = "poster" (label EN, alias umum)
    private static final Set<String> POSTER_ROLE_QIDS = new HashSet<>(Arrays.asList(
            "Q429785", "Q14660174"
    ));

    /**
     * Extract poster URL dengan prioritas:
     *
     * Pass 1: P3383 (film poster) — properti resmi Wikidata untuk poster film.
     *         Terbukti dari raw JSON Q496266: "P3383" → "Charade (1963 poster).jpg"
     *         Catatan: P2080 = lyrics (SALAH), P7818 = tidak valid (SALAH).
     *
     * Pass 2: P18 (image) dengan qualifier P3831 = Q429785 atau Q14660174 (poster role).
     *         Beberapa film menyimpan poster di P18 dengan qualifier ini.
     *
     * Pass 3: Heuristik nama file — P18 yang namanya mengandung kata "poster"
     *         (case-insensitive). Efektif untuk film yang tidak memakai qualifier.
     *
     * Pass 4: P18 pertama tanpa qualifier P3831 (fallback untuk film lama
     *         yang hanya punya satu gambar dan itu posternya).
     *
     * Pass 5: P18 entry pertama (absolute last resort).
     */
    private String extractPosterUrl(JsonNode entity) {

        // Pass 1: P3383 — properti resmi film poster di Wikidata
        // FIX: P2080 = lyrics (salah), P7818 = tidak valid (salah), P3383 = benar
        JsonNode p3383Claims = entity.path("claims").path("P3383");
        if (p3383Claims.isArray() && !p3383Claims.isEmpty()) {
            String filename = p3383Claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
            if (!filename.isEmpty()) {
                log.debug("Poster via P3383: {}", filename);
                return generateCommonsUrl(filename);
            }
        }

        JsonNode p18Claims = entity.path("claims").path("P18");
        if (!p18Claims.isArray() || p18Claims.isEmpty()) return null;

        // Debug: log semua P18 beserta qualifier-nya agar mudah dianalisis
        for (JsonNode claim : p18Claims) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            JsonNode p3831 = claim.path("qualifiers").path("P3831");
            if (p3831.isArray() && !p3831.isEmpty()) {
                String roleId = p3831.get(0).path("datavalue").path("value").path("id").asText("");
                log.debug("P18 entry: {} | P3831 qualifier: {}", fn, roleId);
            } else {
                log.debug("P18 entry: {} | no P3831 qualifier", fn);
            }
        }

        // Pass 2: P18 dengan qualifier P3831 yang merupakan poster role
        for (JsonNode claim : p18Claims) {
            JsonNode p3831 = claim.path("qualifiers").path("P3831");
            if (p3831.isArray() && !p3831.isEmpty()) {
                String roleId = p3831.get(0).path("datavalue").path("value").path("id").asText("");
                if (POSTER_ROLE_QIDS.contains(roleId)) {
                    String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!filename.isEmpty()) {
                        log.debug("Poster via P18+{}: {}", roleId, filename);
                        return generateCommonsUrl(filename);
                    }
                }
            }
        }

        // Pass 3: heuristik nama file — P18 yang namanya mengandung "poster"
        for (JsonNode claim : p18Claims) {
            String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!filename.isEmpty() && filename.toLowerCase().contains("poster")) {
                log.debug("Poster via filename heuristic: {}", filename);
                return generateCommonsUrl(filename);
            }
        }

        // Pass 4: P18 tanpa qualifier P3831 (fallback film lama dengan 1 gambar)
        for (JsonNode claim : p18Claims) {
            if (!claim.path("qualifiers").has("P3831")) {
                String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                if (!filename.isEmpty()) {
                    log.debug("Poster via P18 tanpa qualifier: {}", filename);
                    return generateCommonsUrl(filename);
                }
            }
        }

        // Pass 5: absolute last resort
        String filename = p18Claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
        if (!filename.isEmpty()) {
            log.debug("Poster via P18 entry pertama (last resort): {}", filename);
            return generateCommonsUrl(filename);
        }

        return null;
    }

    /**
     * Extract semua image URL dari P18 yang bukan poster.
     *
     * Sebuah P18 dianggap POSTER jika salah satu dari ini terpenuhi:
     *   a) Sudah diambil oleh extractPosterUrl via P3383 (properti resmi poster)
     *   b) Punya qualifier P3831 yang merupakan poster role
     *   c) Nama filenya mengandung "poster" (heuristik)
     *   d) Merupakan satu-satunya P18 dan tidak ada P3383
     *      → dalam kasus ini extractPosterUrl sudah mengambilnya via Pass 3/4
     *
     * Strategi: gunakan posterFilename yang dikembalikan extractPosterUrl
     * sebagai acuan untuk meng-exclude dari imageUrls.
     */
    private List<String> extractImageUrls(JsonNode entity) {
        List<String> images = new ArrayList<>();
        JsonNode claims = entity.path("claims").path("P18");
        if (!claims.isArray() || claims.isEmpty()) return images;

        // Tentukan filename yang sudah jadi poster:
        // 1. Dari P3383 (properti resmi film poster)
        Set<String> posterFilenames = new HashSet<>();
        JsonNode p3383Claims = entity.path("claims").path("P3383");
        if (p3383Claims.isArray() && !p3383Claims.isEmpty()) {
            String fn = p3383Claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty()) posterFilenames.add(fn);
        }

        // 2. Dari P18 dengan qualifier poster role
        for (JsonNode claim : claims) {
            JsonNode p3831 = claim.path("qualifiers").path("P3831");
            if (p3831.isArray() && !p3831.isEmpty()) {
                String roleId = p3831.get(0).path("datavalue").path("value").path("id").asText("");
                if (POSTER_ROLE_QIDS.contains(roleId)) {
                    String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!fn.isEmpty()) posterFilenames.add(fn);
                }
            }
        }

        // 3. Dari P18 yang namanya mengandung "poster" (heuristik, sama dengan Pass 3)
        for (JsonNode claim : claims) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty() && fn.toLowerCase().contains("poster")) {
                posterFilenames.add(fn);
            }
        }

        // 4. Jika posterFilenames masih kosong (tidak ada P3383, tidak ada qualifier poster,
        //    tidak ada nama "poster"), maka Pass 4/5 extractPosterUrl mengambil P18 pertama
        //    tanpa qualifier → exclude juga dari imageUrls
        if (posterFilenames.isEmpty()) {
            for (JsonNode claim : claims) {
                if (!claim.path("qualifiers").has("P3831")) {
                    String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!fn.isEmpty()) {
                        posterFilenames.add(fn);
                        break; // hanya yang pertama
                    }
                }
            }
            // Jika masih kosong → tambahkan entry pertama (Pass 5)
            if (posterFilenames.isEmpty()) {
                String fn = claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
                if (!fn.isEmpty()) posterFilenames.add(fn);
            }
        }

        // Kumpulkan semua P18 yang bukan poster
        for (JsonNode claim : claims) {
            String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (filename.isEmpty()) continue;
            if (posterFilenames.contains(filename)) continue; // skip poster

            images.add(generateCommonsUrl(filename));
            log.debug("Image added: {}", filename);
        }

        log.debug("Extracted {} additional images", images.size());
        return images;
    }

    /**
     * Extract image URL tunggal untuk Person/Company (P18 entry pertama).
     */
    private String extractSingleImageUrl(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P18");
        if (claims.isArray() && !claims.isEmpty()) {
            String filename = claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
            if (!filename.isEmpty()) return generateCommonsUrl(filename);
        }
        return null;
    }

    /**
     * Extract full video URL (P10 dengan qualifier P3831 = Q89347362)
     */
    private String extractVideoUrl(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P10")) {
            JsonNode qualifiers = claim.path("qualifiers").path("P3831");
            if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                String roleId = qualifiers.get(0).path("datavalue").path("value").path("id").asText("");
                if ("Q89347362".equals(roleId)) {
                    String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!filename.isEmpty()) return generateCommonsUrl(filename);
                }
            }
        }
        return null;
    }

    /**
     * Extract trailer URL (P10 dengan qualifier P3831 = Q622550)
     */
    private String extractTrailerUrl(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P10")) {
            JsonNode qualifiers = claim.path("qualifiers").path("P3831");
            if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                String roleId = qualifiers.get(0).path("datavalue").path("value").path("id").asText("");
                if ("Q622550".equals(roleId)) {
                    String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!filename.isEmpty()) return generateCommonsUrl(filename);
                }
            }
        }
        return null;
    }

    // ==================== WIKIDATA EXTRACTION METHODS ====================

    private List<String> extractGenres(JsonNode entity) {
        List<String> genres = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P136")) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) {
                label = label.replaceAll("(?i)\\s+films?$", "").trim();
                if (!label.isEmpty()) {
                    genres.add(Character.toUpperCase(label.charAt(0)) + label.substring(1));
                }
            }
        }
        return genres;
    }

    private List<Person> extractPersonsWithDetails(JsonNode entity, String property) {
        List<Person> result = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!qid.isEmpty()) {
                Person person = fetchPersonDetails(qid);
                result.add(person);
            }
        }
        return result;
    }

    private List<Company> extractCompaniesWithDetails(JsonNode entity, String property) {
        List<Company> result = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!qid.isEmpty()) {
                Company company = fetchCompanyDetails(qid);
                result.add(company);
            }
        }
        return result;
    }

    private String extractColor(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P462");
        if (claims.isArray() && !claims.isEmpty()) {
            String colorId = claims.get(0).path("mainsnak").path("datavalue").path("value").path("id").asText();
            if ("Q838368".equals(colorId)) return "black-and-white";
            if ("Q22006653".equals(colorId)) return "color";
            return fetchLabelById(colorId);
        }
        return null;
    }

    private String extractOriginalLanguage(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P364");
        if (claims.isArray() && !claims.isEmpty()) {
            return fetchLabelById(claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText());
        }
        return null;
    }

    private BudgetData extractBudget(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2130");
        if (claims.isArray() && !claims.isEmpty()) {
            String amount = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("amount").asText().replace("+", "");
            if (!amount.isEmpty()) {
                try {
                    long cents = (long) (Double.parseDouble(amount) * 100);
                    BudgetData budget = new BudgetData();
                    budget.setAmount(cents);
                    budget.setCurrency("USD");
                    budget.setDisplayValue(formatCurrency(cents, "USD"));
                    return budget;
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse budget: {}", amount);
                }
            }
        }
        return null;
    }

    private List<BoxOfficeData> extractBoxOffice(JsonNode entity) {
        List<BoxOfficeData> results = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P2142")) {
            String amount = claim.path("mainsnak").path("datavalue")
                    .path("value").path("amount").asText().replace("+", "");
            if (amount.isEmpty()) continue;
            try {
                long cents = (long) (Double.parseDouble(amount) * 100);
                BoxOfficeData data = new BoxOfficeData();
                data.setAmount(cents);
                data.setCurrency("USD");
                data.setDisplayValue(formatCurrency(cents, "USD"));

                JsonNode qualifiers = claim.path("qualifiers").path("P3005");
                if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                    data.setRegion(fetchLabelById(qualifiers.get(0).path("datavalue")
                            .path("value").path("id").asText()));
                } else {
                    data.setRegion("worldwide");
                }
                results.add(data);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse box office: {}", amount);
            }
        }
        return results;
    }

    private List<ReviewScore> extractReviewScores(JsonNode entity) {
        List<ReviewScore> scores = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P444")) {
            ReviewScore score = new ReviewScore();
            score.setValue(claim.path("mainsnak").path("datavalue").path("value").asText());

            JsonNode q = claim.path("qualifiers");
            JsonNode reviewer = q.path("P447");
            if (reviewer.isArray() && !reviewer.isEmpty())
                score.setSource(fetchLabelById(reviewer.get(0).path("datavalue").path("value").path("id").asText()));

            JsonNode method = q.path("P459");
            if (method.isArray() && !method.isEmpty())
                score.setScoreType(fetchLabelById(method.get(0).path("datavalue").path("value").path("id").asText()));

            JsonNode numRev = q.path("P3744");
            if (numRev.isArray() && !numRev.isEmpty()) {
                try {
                    score.setNumReviews(Integer.parseInt(numRev.get(0).path("datavalue").path("value").path("amount").asText().replace("+", "")));
                } catch (NumberFormatException ignored) {}
            }

            JsonNode date = q.path("P585");
            if (date.isArray() && !date.isEmpty())
                score.setReviewDate(date.get(0).path("datavalue").path("value")
                        .path("time").asText().replaceAll("\\+|T.*", ""));

            if (score.getSource() != null) scores.add(score);
        }
        return scores;
    }

    private List<ContentRating> extractContentRatings(JsonNode entity) {
        List<ContentRating> ratings = new ArrayList<>();
        ratings.addAll(extractRatingByProperty(entity, "P1657", "MPA"));
        ratings.addAll(extractRatingByProperty(entity, "P2758", "BBFC"));
        ratings.addAll(extractRatingByProperty(entity, "P1981", "FSK"));
        ratings.addAll(extractRatingByProperty(entity, "P2980", "EIRIN"));
        ratings.addAll(extractRatingByProperty(entity, "P1562", "CNC"));
        ratings.addAll(extractRatingByProperty(entity, "P5458", "RARS"));
        return ratings;
    }

    private List<ContentRating> extractRatingByProperty(JsonNode entity, String property, String system) {
        List<ContentRating> ratings = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            ContentRating rating = new ContentRating();
            rating.setSystem(system);

            String ratingId = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!ratingId.isEmpty()) rating.setValue(fetchLabelById(ratingId));

            JsonNode q = claim.path("qualifiers");
            List<String> descriptors = new ArrayList<>();
            for (JsonNode node : q.path("P2614")) {
                String descId = node.path("datavalue").path("value").path("id").asText();
                if (!descId.isEmpty()) descriptors.add(fetchLabelById(descId));
            }
            if (!descriptors.isEmpty()) rating.setContentDescriptors(String.join(", ", descriptors));

            JsonNode dateNode = q.path("P580");
            if (dateNode.isArray() && !dateNode.isEmpty())
                rating.setStartDate(dateNode.get(0).path("datavalue").path("value")
                        .path("time").asText().replaceAll("\\+|T.*", ""));

            JsonNode formatNode = q.path("P437");
            if (formatNode.isArray() && !formatNode.isEmpty())
                rating.setDistributionFormat(fetchLabelById(formatNode.get(0).path("datavalue")
                        .path("value").path("id").asText()));

            ratings.add(rating);
        }
        return ratings;
    }

    private String extractFollowedBy(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P156");
        if (claims.isArray() && !claims.isEmpty())
            return fetchLabelById(claims.get(0).path("mainsnak").path("datavalue").path("value").path("id").asText());
        return null;
    }

    private String extractPartOfSeries(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P179");
        if (claims.isArray() && !claims.isEmpty())
            return fetchLabelById(claims.get(0).path("mainsnak").path("datavalue").path("value").path("id").asText());
        return null;
    }

    private List<String> extractAliases(JsonNode entity) {
        List<String> aliases = new ArrayList<>();
        for (JsonNode aliasNode : entity.path("aliases").path("id")) {
            String alias = aliasNode.path("value").asText();
            if (!alias.isEmpty()) aliases.add(alias);
        }
        return aliases;
    }

    private String extractSubtitleUrl(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("/")) return null;
        try {
            String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            for (String lang : new String[]{"id", "en"}) {
                String subtitleUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + "." + lang + ".srt";
                try {
                    ResponseEntity<String> r = restTemplate.exchange(subtitleUrl, HttpMethod.GET, entity, String.class);
                    if (r.getStatusCode() == HttpStatus.OK && r.getBody() != null && r.getBody().contains("<pre")) {
                        return subtitleUrl;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Subtitle check failed for video: {}", videoUrl);
        }
        return null;
    }

    // ==================== WIKIDATA FETCH HELPERS ====================

    private Person fetchPersonDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
            JsonNode entityNode = mapper.readTree(response.getBody()).path("entities").path(qid);
            Person person = new Person();
            person.setWikidataQid(qid);
            person.setName(getLabel(entityNode));
            person.setDescription(getDescription(entityNode));
            person.setPhotoUrl(extractSingleImageUrl(entityNode));
            return person;
        } catch (Exception e) {
            log.warn("Failed to fetch person QID: {}", qid);
            Person person = new Person();
            person.setWikidataQid(qid);
            person.setName(qid);
            return person;
        }
    }

    private Company fetchCompanyDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
            JsonNode entityNode = mapper.readTree(response.getBody()).path("entities").path(qid);
            Company company = new Company();
            company.setWikidataQid(qid);
            company.setName(getLabel(entityNode));
            company.setDescription(getDescription(entityNode));
            company.setLogoUrl(extractSingleImageUrl(entityNode));
            return company;
        } catch (Exception e) {
            log.warn("Failed to fetch company QID: {}", qid);
            Company company = new Company();
            company.setWikidataQid(qid);
            company.setName(qid);
            return company;
        }
    }

    private String fetchLabelById(String id) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
            return getLabel(mapper.readTree(response.getBody()).path("entities").path(id));
        } catch (Exception e) {
            log.warn("Failed to fetch label for: {}", id);
            return null;
        }
    }

    // ==================== BASIC HELPERS ====================

    private String getLabel(JsonNode entity) {
        JsonNode labels = entity.path("labels").path("en");
        return labels.has("value") ? labels.path("value").asText() : null;
    }

    private String getDescription(JsonNode entity) {
        JsonNode descriptions = entity.path("descriptions").path("en");
        return descriptions.has("value") ? descriptions.path("value").asText() : null;
    }

    private String extractDate(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P577");
        if (claims.isArray() && !claims.isEmpty()) {
            JsonNode time = claims.get(0).path("mainsnak").path("datavalue").path("value").path("time");
            if (!time.isMissingNode()) return time.asText().replaceAll("\\+|T.*", "");
        }
        return null;
    }

    private String extractEntityLabel(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && !claims.isEmpty())
            return fetchLabelById(claims.get(0).path("mainsnak").path("datavalue").path("value").path("id").asText());
        return null;
    }

    private List<String> extractEntityLabels(JsonNode entity, String property) {
        List<String> result = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String label = fetchLabelById(claim.path("mainsnak").path("datavalue").path("value").path("id").asText());
            if (label != null) result.add(label);
        }
        return result;
    }

    private String extractDuration(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2047");
        if (claims.isArray() && !claims.isEmpty()) {
            JsonNode amount = claims.get(0).path("mainsnak").path("datavalue").path("value").path("amount");
            if (!amount.isMissingNode()) return amount.asText().replace("+", "") + " menit";
        }
        return null;
    }

    // ==================== UTILITY ====================

    private String generateCommonsUrl(String filename) {
        try {
            String encodedFile = filename.replace(" ", "_");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(encodedFile.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            String md5 = sb.toString();
            return "https://upload.wikimedia.org/wikipedia/commons/" +
                    md5.charAt(0) + "/" + md5.substring(0, 2) + "/" + encodedFile;
        } catch (Exception e) {
            return "https://commons.wikimedia.org/wiki/File:" + filename.replace(" ", "_");
        }
    }

    private String parseSubtitleUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.contains("/wiki/TimedText:")) {
            String title = url.substring(url.indexOf("/wiki/") + 6);
            return "https://commons.wikimedia.org/w/index.php?title=" + title + "&action=raw";
        }
        if (url.contains("action=raw") || url.endsWith(".srt") || url.endsWith(".vtt")) return url;
        return null;
    }

    private String convertSrtToVtt(String srtContent) {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        String converted = srtContent.replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2");
        for (String block : converted.split("\\n\\n")) {
            if (block.trim().isEmpty()) continue;
            boolean found = false;
            for (String line : block.split("\\n")) {
                if (line.matches("^\\d+$")) continue;
                if (line.contains("-->")) { found = true; vtt.append(line).append("\n"); }
                else if (found) vtt.append(line).append("\n");
            }
            if (found) vtt.append("\n");
        }
        return vtt.toString();
    }

    private String extractFilename(String url) {
        if (url == null || !url.contains("/")) return null;
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    private String formatCurrency(long amountInCents, String currency) {
        double amount = amountInCents / 100.0;
        if ("USD".equals(currency)) return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
        return currency + " " + String.format("%,.2f", amount);
    }
}