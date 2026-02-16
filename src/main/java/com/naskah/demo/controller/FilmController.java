package com.naskah.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.Company;
import com.naskah.demo.model.film.Film;
import com.naskah.demo.model.film.FilmDetail;
import com.naskah.demo.model.film.Person;
import com.naskah.demo.service.film.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

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

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "FilmApp/1.0 (https://github.com/yourusername/filmapp; your.email@example.com)");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ==================== SPECIFIC ROUTES FIRST! ====================

    /**
     * Search films - BEFORE /{slug} to avoid conflict
     */
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

    /**
     * GET person by SLUG - BEFORE /{slug} to avoid conflict
     */
    @GetMapping(value = "/person/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Person> getPersonBySlug(@PathVariable String slug) {
        Person person = filmMapper.findPersonBySlug(slug);
        if (person == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(person);
    }

    /**
     * GET company by SLUG - BEFORE /{slug} to avoid conflict
     */
    @GetMapping(value = "/company/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Company> getCompanyBySlug(@PathVariable String slug) {
        Company company = filmMapper.findCompanyBySlug(slug);
        if (company == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(company);
    }

    /**
     * GET film from Wikidata and save to database
     */
    @GetMapping(value = "/wikidata/{qid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmFromWikidata(@PathVariable String qid) {
        try {
            // Check if already in database
            FilmDetail cached = filmService.getFilmDetailByQid(qid);
            if (cached != null) {
                return ResponseEntity.ok(cached);
            }

            // Fetch from Wikidata
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode entityNode = root.path("entities").path(qid);

            FilmDetail filmDetail = new FilmDetail();
            filmDetail.setWikidataQid(qid);
            filmDetail.setJudul(getLabel(entityNode));
            filmDetail.setDeskripsi(getDescription(entityNode));
            filmDetail.setTahunRilis(extractDate(entityNode));
            filmDetail.setJenis(extractEntityLabelWithFallback(entityNode, "P31"));
            filmDetail.setGenre(extractEntityLabelsWithFallback(entityNode, "P136"));
            filmDetail.setSutradara(extractPersonsWithDetails(entityNode, "P57"));
            filmDetail.setPenulisSkenario(extractPersonsWithDetails(entityNode, "P58"));
            filmDetail.setPemeran(extractPersonsWithDetails(entityNode, "P161"));
            filmDetail.setProduser(extractPersonsWithDetails(entityNode, "P162"));
            filmDetail.setPerusahaanProduksi(extractCompaniesWithDetails(entityNode, "P272"));
            filmDetail.setNegaraAsal(extractEntityLabelWithFallback(entityNode, "P495"));
            filmDetail.setDurasi(extractDuration(entityNode));
            filmDetail.setPosterUrl(extractImageUrl(entityNode));
            filmDetail.setVideoUrl(extractVideoUrl(entityNode));
            filmDetail.setAliasIndonesia(extractAliases(entityNode));
            filmDetail.setSubtitleUrl(extractSubtitleUrl(filmDetail.getVideoUrl()));

            // Save to database
            Film savedFilm = filmService.saveFilm(filmDetail);
            filmDetail.setId(savedFilm.getId());

            return ResponseEntity.ok(filmDetail);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Proxy endpoint untuk subtitle
     * CRITICAL: This MUST be before /{slug} route!
     */
    @GetMapping(value = "/{filmSlug}/subtitle", produces = "text/vtt")
    public ResponseEntity<String> getSubtitle(@PathVariable String filmSlug) {
        try {
            System.out.println("=== SUBTITLE REQUEST ===");
            System.out.println("Film Slug: " + filmSlug);

            // Get film from database
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null) {
                System.out.println("Film not found: " + filmSlug);
                return ResponseEntity.notFound().build();
            }

            System.out.println("Film found: " + film.getJudul());
            System.out.println("Subtitle URL: " + film.getSubtitleUrl());

            if (film.getSubtitleUrl() == null) {
                System.out.println("No subtitle URL for this film");
                return ResponseEntity.notFound().build();
            }

            // Parse subtitle URL to raw file URL
            String rawUrl = parseSubtitleUrl(film.getSubtitleUrl());
            System.out.println("Raw URL: " + rawUrl);

            if (rawUrl == null) {
                System.out.println("Failed to parse subtitle URL");
                return ResponseEntity.notFound().build();
            }

            // Fetch subtitle from Wikimedia
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Backend Proxy)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            System.out.println("Fetching from: " + rawUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    rawUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            System.out.println("Response status: " + response.getStatusCode());

            String subtitleContent = response.getBody();
            if (subtitleContent == null || subtitleContent.isEmpty()) {
                System.out.println("Empty subtitle content");
                return ResponseEntity.notFound().build();
            }

            System.out.println("Content length: " + subtitleContent.length());

            // ✅ KONVERSI SRT KE VTT
            String vttContent = convertSrtToVtt(subtitleContent);
            System.out.println("Converted to VTT, length: " + vttContent.length());

            // Return subtitle with proper headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.valueOf("text/vtt; charset=UTF-8"));
            responseHeaders.set("Cache-Control", "public, max-age=86400");

            return new ResponseEntity<>(vttContent, responseHeaders, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Error fetching subtitle:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert SRT subtitle format to WebVTT format
     */
    private String convertSrtToVtt(String srtContent) {
        // Add WEBVTT header
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");

        // Replace comma with dot in timestamps (SRT uses comma, VTT uses dot)
        String converted = srtContent.replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2");

        // Split by double newline (subtitle blocks)
        String[] blocks = converted.split("\\n\\n");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            // Split each block into lines
            String[] lines = block.split("\\n");

            if (lines.length < 2) continue;

            // Skip the sequence number (first line) and only keep timestamp + text
            // SRT format: 1\n00:00:01,000 --> 00:00:04,000\nText
            // VTT format: 00:00:01.000 --> 00:00:04.000\nText

            boolean timestampFound = false;
            for (String line : lines) {
                // Skip numeric sequence lines
                if (line.matches("^\\d+$")) {
                    continue;
                }

                // Keep timestamp and text lines
                if (line.contains("-->")) {
                    timestampFound = true;
                    vtt.append(line).append("\n");
                } else if (timestampFound) {
                    vtt.append(line).append("\n");
                }
            }

            // Add blank line between cues
            if (timestampFound) {
                vtt.append("\n");
            }
        }

        return vtt.toString();
    }

    @GetMapping(value = "/{filmSlug}/video-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoInfo(@PathVariable String filmSlug) {
        try {
            System.out.println("=== VIDEO INFO REQUEST ===");
            System.out.println("Film Slug: " + filmSlug);

            // Get film from database
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null) {
                System.out.println("Film not found: " + filmSlug);
                return ResponseEntity.notFound().build();
            }

            System.out.println("Film found: " + film.getJudul());
            System.out.println("Video URL: " + film.getVideoUrl());

            if (film.getVideoUrl() == null) {
                System.out.println("No video URL for this film");
                return ResponseEntity.notFound().build();
            }

            // Extract filename from video URL
            String filename = extractFilename(film.getVideoUrl());
            if (filename == null) {
                System.out.println("Failed to extract filename");
                return ResponseEntity.badRequest().build();
            }

            System.out.println("Filename: " + filename);

            // ✅ Build URL with UriComponentsBuilder to avoid double encoding
            String apiUrl = UriComponentsBuilder
                    .fromHttpUrl("https://commons.wikimedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("titles", "File:" + filename)  // Filename akan di-encode otomatis
                    .queryParam("prop", "videoinfo")
                    .queryParam("viprop", "derivatives|url|size|mediatype")
                    .queryParam("format", "json")
                    .build()
                    .toUriString();

            System.out.println("API URL: " + apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Video Info Request)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            System.out.println("API Response status: " + response.getStatusCode());

            // Parse response
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode pages = root.path("query").path("pages");

            // ✅ Handle case where pages might be empty
            if (!pages.elements().hasNext()) {
                System.out.println("No pages found in response");
                return ResponseEntity.notFound().build();
            }

            // Get first page (there's only one)
            JsonNode page = pages.elements().next();
            JsonNode videoInfo = page.path("videoinfo");

            if (!videoInfo.isArray() || videoInfo.isEmpty()) {
                System.out.println("No video info found in response");
                return ResponseEntity.notFound().build();
            }

            JsonNode info = videoInfo.get(0);

            // Build response with quality options
            Map<String, Object> result = new HashMap<>();
            result.put("originalUrl", info.path("url").asText());
            result.put("originalSize", info.path("size").asLong());
            result.put("mediaType", info.path("mediatype").asText());

            // Extract derivatives (different quality options)
            List<Map<String, Object>> qualities = new ArrayList<>();
            JsonNode derivatives = info.path("derivatives");

            if (derivatives.isArray()) {
                for (JsonNode derivative : derivatives) {
                    Map<String, Object> quality = new HashMap<>();
                    quality.put("type", derivative.path("type").asText());
                    quality.put("src", derivative.path("src").asText());
                    quality.put("width", derivative.path("width").asInt());
                    quality.put("height", derivative.path("height").asInt());

                    // Calculate quality label (e.g., "360p", "720p")
                    int height = derivative.path("height").asInt();
                    quality.put("label", height + "p");

                    qualities.add(quality);
                }
            }

            // Sort by height (quality)
            qualities.sort((a, b) -> {
                int heightA = (int) a.get("height");
                int heightB = (int) b.get("height");
                return Integer.compare(heightB, heightA); // Descending
            });

            result.put("qualities", qualities);

            System.out.println("Found " + qualities.size() + " quality options");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("Error fetching video info:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET all films with pagination - BEFORE /{slug} to avoid conflict
     */
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

    /**
     * GET film by SLUG (SEO Friendly)
     * THIS MUST BE LAST - it's a catch-all for any remaining /{slug} patterns
     */
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmBySlug(@PathVariable String slug) {
        System.out.println("=== FILM DETAIL REQUEST ===");
        System.out.println("Slug: " + slug);

        FilmDetail film = filmService.getFilmDetailBySlug(slug);
        if (film == null) {
            System.out.println("Film not found");
            return ResponseEntity.notFound().build();
        }

        System.out.println("Film found: " + film.getJudul());
        return ResponseEntity.ok(film);
    }

    // ==================== HELPER METHODS ====================

    private Person fetchPersonDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode entityNode = root.path("entities").path(qid);

            Person person = new Person();
            person.setWikidataQid(qid);
            person.setName(getLabel(entityNode));
            person.setDescription(getDescription(entityNode));
            person.setPhotoUrl(extractImageUrl(entityNode));

            return person;
        } catch (Exception e) {
            Person person = new Person();
            person.setWikidataQid(qid);
            person.setName(qid);
            return person;
        }
    }

    private Company fetchCompanyDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode entityNode = root.path("entities").path(qid);

            Company company = new Company();
            company.setWikidataQid(qid);
            company.setName(getLabel(entityNode));
            company.setDescription(getDescription(entityNode));
            company.setLogoUrl(extractImageUrl(entityNode));

            return company;
        } catch (Exception e) {
            Company company = new Company();
            company.setWikidataQid(qid);
            company.setName(qid);
            return company;
        }
    }

    private List<Person> extractPersonsWithDetails(JsonNode entity, String property) {
        List<Person> result = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
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
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!qid.isEmpty()) {
                Company company = fetchCompanyDetails(qid);
                result.add(company);
            }
        }

        return result;
    }

    private String fetchLabelById(String id) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode target = root.path("entities").path(id);
            return getLabel(target);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractSubtitleUrl(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("/")) return null;
        try {
            String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            String subtitleIdUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".id.srt";
            String subtitleEnUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".en.srt";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> responseId = restTemplate.exchange(subtitleIdUrl, HttpMethod.GET, entity, String.class);
            if (responseId.getStatusCode() == HttpStatus.OK && responseId.getBody() != null && responseId.getBody().contains("<pre")) {
                return subtitleIdUrl;
            }
            ResponseEntity<String> responseEn = restTemplate.exchange(subtitleEnUrl, HttpMethod.GET, entity, String.class);
            if (responseEn.getStatusCode() == HttpStatus.OK && responseEn.getBody() != null && responseEn.getBody().contains("<pre")) {
                return subtitleEnUrl;
            }
        } catch (Exception e) {}
        return null;
    }

    /**
     * Parse Wikimedia subtitle URL to raw file URL
     */
    private String parseSubtitleUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // If it's a wiki page URL, convert to raw file URL
        if (url.contains("/wiki/TimedText:")) {
            // Extract the title part
            int startIndex = url.indexOf("/wiki/") + 6;
            String title = url.substring(startIndex);

            // Build raw file URL
            return "https://commons.wikimedia.org/w/index.php?title=" + title + "&action=raw";
        }

        // If it's already a raw URL or direct file, return as is
        if (url.contains("action=raw") || url.endsWith(".srt") || url.endsWith(".vtt")) {
            return url;
        }

        return null;
    }

    /**
     * Extract filename from Wikimedia Commons URL
     */
    private String extractFilename(String url) {
        if (url == null || !url.contains("/")) {
            return null;
        }

        try {
            // Extract filename from URL
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }

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
            JsonNode timeValue = claims.get(0).path("mainsnak").path("datavalue").path("value").path("time");
            if (!timeValue.isMissingNode()) {
                return timeValue.asText().replaceAll("\\+|T.*", "");
            }
        }
        return null;
    }

    private String extractEntityLabelWithFallback(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && !claims.isEmpty()) {
            String id = claims.get(0).path("mainsnak").path("datavalue").path("value").path("id").asText();
            return fetchLabelById(id);
        }
        return null;
    }

    private List<String> extractEntityLabelsWithFallback(JsonNode entity, String property) {
        List<String> result = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);
        for (JsonNode claim : claims) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) result.add(label);
        }
        return result;
    }

    private String extractDuration(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2047");
        if (claims.isArray() && !claims.isEmpty()) {
            JsonNode amount = claims.get(0).path("mainsnak").path("datavalue").path("value").path("amount");
            if (!amount.isMissingNode()) {
                String durationValue = amount.asText().replace("+", "");
                return durationValue + " menit";
            }
        }
        return null;
    }

    private String extractImageUrl(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P18");
        if (claims.isArray() && !claims.isEmpty()) {
            String filename = claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
            return generateCommonsUrl(filename);
        }
        return null;
    }

    private String extractVideoUrl(JsonNode entity) {
        JsonNode videoClaims = entity.path("claims").path("P10");
        if (videoClaims.isArray()) {
            for (JsonNode claim : videoClaims) {
                JsonNode qualifiers = claim.path("qualifiers").path("P3831");
                if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                    String roleId = qualifiers.get(0).path("datavalue").path("value").path("id").asText("");
                    if ("Q89347362".equals(roleId)) {
                        String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                        return generateCommonsUrl(filename);
                    }
                }
            }
        }
        JsonNode commonsCat = entity.path("claims").path("P373");
        if (commonsCat.isArray() && !commonsCat.isEmpty()) {
            String category = commonsCat.get(0).path("mainsnak").path("datavalue").path("value").asText();
            return "https://commons.wikimedia.org/wiki/Category:" + category.replace(" ", "_");
        }
        return null;
    }

    private List<String> extractAliases(JsonNode entity) {
        List<String> aliases = new ArrayList<>();
        JsonNode aliasesNode = entity.path("aliases").path("id");
        for (JsonNode aliasNode : aliasesNode) {
            String alias = aliasNode.path("value").asText();
            if (!alias.isEmpty()) aliases.add(alias);
        }
        return aliases;
    }

    private String generateCommonsUrl(String filename) {
        try {
            String encodedFile = filename.replace(" ", "_");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(encodedFile.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String md5 = sb.toString();
            return "https://upload.wikimedia.org/wikipedia/commons/" + md5.charAt(0) + "/" + md5.substring(0, 2) + "/" + encodedFile;
        } catch (Exception e) {
            return "https://commons.wikimedia.org/wiki/File:" + filename.replace(" ", "_");
        }
    }
}