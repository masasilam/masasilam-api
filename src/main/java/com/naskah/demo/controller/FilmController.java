package com.naskah.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naskah.demo.mapper.FilmMapper;
import com.naskah.demo.model.film.*;
import com.naskah.demo.model.film.FilmDetail.*;
import com.naskah.demo.service.film.FilmService;
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

/**
 * FilmController - REST controller for film operations
 */
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

    /**
     * Create headers for Wikidata requests
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "FilmApp/1.0 (Enhanced Metadata Extractor)");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ==================== SPECIFIC ROUTES FIRST ====================

    /**
     * Search films by query
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
     * GET person by SLUG
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
     * GET company by SLUG
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
     * This is the main endpoint for fetching complete film metadata
     */
    @GetMapping(value = "/wikidata/{qid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmFromWikidata(@PathVariable String qid) {
        try {
            System.out.println("=== FETCHING COMPLETE METADATA FOR: " + qid + " ===");

            // Check if already in database
            FilmDetail cached = filmService.getFilmDetailByQid(qid);
            if (cached != null) {
                System.out.println("Found in cache");
                return ResponseEntity.ok(cached);
            }

            // Fetch from Wikidata
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode entityNode = root.path("entities").path(qid);

            FilmDetail filmDetail = new FilmDetail();

            // ==================== BASIC INFO ====================
            filmDetail.setWikidataQid(qid);
            filmDetail.setJudul(getLabel(entityNode));
            filmDetail.setDeskripsi(getDescription(entityNode));
            filmDetail.setTahunRilis(extractDate(entityNode));
            filmDetail.setJenis(extractEntityLabel(entityNode, "P31"));
            filmDetail.setGenre(extractGenres(entityNode));
            System.out.println("✓ Basic info extracted");

            // ==================== PEOPLE - CORE ROLES ====================
            filmDetail.setSutradara(extractPersonsWithDetails(entityNode, "P57"));
            filmDetail.setPenulisSkenario(extractPersonsWithDetails(entityNode, "P58"));
            filmDetail.setPemeran(extractPersonsWithDetails(entityNode, "P161"));
            filmDetail.setProduser(extractPersonsWithDetails(entityNode, "P162"));
            System.out.println("✓ Core crew extracted");

            // ==================== PEOPLE - ADDITIONAL CREW ====================
            filmDetail.setFilmEditor(extractPersonsWithDetails(entityNode, "P1040"));
            filmDetail.setCinematographer(extractPersonsWithDetails(entityNode, "P344"));
            filmDetail.setComposer(extractPersonsWithDetails(entityNode, "P86"));
            System.out.println("✓ Additional crew extracted");

            // ==================== COMPANIES ====================
            filmDetail.setPerusahaanProduksi(extractCompaniesWithDetails(entityNode, "P272"));
            filmDetail.setDistributor(extractCompaniesWithDetails(entityNode, "P750"));
            System.out.println("✓ Companies extracted");

            // ==================== LOCATIONS ====================
            filmDetail.setNegaraAsal(extractEntityLabel(entityNode, "P495"));
            filmDetail.setNarrativeLocation(extractEntityLabels(entityNode, "P840"));
            filmDetail.setFilmingLocation(extractEntityLabels(entityNode, "P915"));
            filmDetail.setDurasi(extractDuration(entityNode));
            System.out.println("✓ Location data extracted");

            // ==================== TECHNICAL INFO ====================
            filmDetail.setColor(extractColor(entityNode));
            filmDetail.setOriginalLanguage(extractOriginalLanguage(entityNode));
            filmDetail.setPosterUrl(extractImageUrl(entityNode));
            filmDetail.setVideoUrl(extractVideoUrl(entityNode));
            filmDetail.setTrailerUrl(extractTrailerUrl(entityNode));  // NEW: Extract trailer
            filmDetail.setSubtitleUrl(extractSubtitleUrl(filmDetail.getVideoUrl()));
            System.out.println("✓ Technical info extracted");

            // ==================== FINANCIAL DATA ====================
            filmDetail.setBudget(extractBudget(entityNode));
            filmDetail.setBoxOffice(extractBoxOffice(entityNode));
            System.out.println("✓ Financial data extracted");

            // ==================== RATINGS & REVIEWS ====================
            filmDetail.setReviewScores(extractReviewScores(entityNode));
            filmDetail.setContentRatings(extractContentRatings(entityNode));
            System.out.println("✓ Ratings & Reviews extracted");

            // ==================== RELATIONS ====================
            filmDetail.setFollowedBy(extractFollowedBy(entityNode));
            filmDetail.setPartOfSeries(extractPartOfSeries(entityNode));
            System.out.println("✓ Relations extracted");

            // ==================== ALIASES ====================
            filmDetail.setAliasIndonesia(extractAliases(entityNode));
            System.out.println("✓ Aliases extracted");

            System.out.println("=== EXTRACTION COMPLETE ===");

            // Save to database
            Film savedFilm = filmService.saveFilm(filmDetail);
            filmDetail.setId(savedFilm.getId());

            return ResponseEntity.ok(filmDetail);

        } catch (Exception e) {
            System.err.println("ERROR extracting metadata:");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Proxy endpoint for subtitle with SRT to VTT conversion
     */
    @GetMapping(value = "/{filmSlug}/subtitle", produces = "text/vtt")
    public ResponseEntity<String> getSubtitle(@PathVariable String filmSlug) {
        try {
            System.out.println("=== SUBTITLE REQUEST ===");
            System.out.println("Film Slug: " + filmSlug);

            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getSubtitleUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            String rawUrl = parseSubtitleUrl(film.getSubtitleUrl());
            if (rawUrl == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Backend Proxy)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(rawUrl, HttpMethod.GET, entity, String.class);
            String subtitleContent = response.getBody();

            if (subtitleContent == null || subtitleContent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Convert SRT to VTT
            String vttContent = convertSrtToVtt(subtitleContent);

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
     * GET video information with quality options
     */
    @GetMapping(value = "/{filmSlug}/video-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoInfo(@PathVariable String filmSlug) {
        try {
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getVideoUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            String filename = extractFilename(film.getVideoUrl());
            if (filename == null) {
                return ResponseEntity.badRequest().build();
            }

            String apiUrl = UriComponentsBuilder
                    .fromHttpUrl("https://commons.wikimedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("titles", "File:" + filename)
                    .queryParam("prop", "videoinfo")
                    .queryParam("viprop", "derivatives|url|size|mediatype")
                    .queryParam("format", "json")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0 (Video Info Request)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode pages = root.path("query").path("pages");

            if (!pages.elements().hasNext()) {
                return ResponseEntity.notFound().build();
            }

            JsonNode page = pages.elements().next();
            JsonNode videoInfo = page.path("videoinfo");

            if (!videoInfo.isArray() || videoInfo.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            JsonNode info = videoInfo.get(0);

            Map<String, Object> result = new HashMap<>();
            result.put("originalUrl", info.path("url").asText());
            result.put("originalSize", info.path("size").asLong());
            result.put("mediaType", info.path("mediatype").asText());

            List<Map<String, Object>> qualities = new ArrayList<>();
            JsonNode derivatives = info.path("derivatives");

            if (derivatives.isArray()) {
                for (JsonNode derivative : derivatives) {
                    Map<String, Object> quality = new HashMap<>();
                    quality.put("type", derivative.path("type").asText());
                    quality.put("src", derivative.path("src").asText());
                    quality.put("width", derivative.path("width").asInt());
                    quality.put("height", derivative.path("height").asInt());
                    quality.put("label", derivative.path("height").asInt() + "p");
                    qualities.add(quality);
                }
            }

            qualities.sort((a, b) -> Integer.compare((int) b.get("height"), (int) a.get("height")));
            result.put("qualities", qualities);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET all films with pagination
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
     * THIS MUST BE LAST - catch-all route
     */
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmBySlug(@PathVariable String slug) {
        FilmDetail film = filmService.getFilmDetailBySlug(slug);
        if (film == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(film);
    }

// ==================== EXTRACTION METHODS ====================

    /**
     * Extract genres and clean names
     */
    private List<String> extractGenres(JsonNode entity) {
        List<String> genres = new ArrayList<>();
        JsonNode claims = entity.path("claims").path("P136");

        for (JsonNode claim : claims) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) {
                // Clean genre name (remove "film" suffix)
                label = label.replaceAll("(?i)\\s+films?$", "").trim();
                if (!label.isEmpty()) {
                    label = label.substring(0, 1).toUpperCase() + label.substring(1);
                    genres.add(label);
                }
            }
        }

        return genres;
    }

    /**
     * Extract persons with full details
     */
    private List<Person> extractPersonsWithDetails(JsonNode entity, String property) {
        List<Person> result = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!qid.isEmpty()) {
                Person person = fetchPersonDetails(qid);
                if (person != null) {
                    result.add(person);
                }
            }
        }

        return result;
    }

    /**
     * Extract companies with full details
     */
    private List<Company> extractCompaniesWithDetails(JsonNode entity, String property) {
        List<Company> result = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!qid.isEmpty()) {
                Company company = fetchCompanyDetails(qid);
                if (company != null) {
                    result.add(company);
                }
            }
        }

        return result;
    }

    /**
     * Extract color information
     */
    private String extractColor(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P462");
        if (claims.isArray() && !claims.isEmpty()) {
            String colorId = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();

            if ("Q838368".equals(colorId)) return "black-and-white";
            if ("Q22006653".equals(colorId)) return "color";

            return fetchLabelById(colorId);
        }
        return null;
    }

    /**
     * Extract original language
     */
    private String extractOriginalLanguage(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P364");
        if (claims.isArray() && !claims.isEmpty()) {
            String langId = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();
            return fetchLabelById(langId);
        }
        return null;
    }

    /**
     * Extract budget
     */
    private BudgetData extractBudget(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2130");

        if (claims.isArray() && !claims.isEmpty()) {
            String amount = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("amount").asText();

            if (!amount.isEmpty()) {
                amount = amount.replace("+", "");
                try {
                    BudgetData budget = new BudgetData();
                    long cents = (long) (Double.parseDouble(amount) * 100);
                    budget.setAmount(cents);
                    budget.setCurrency("USD");
                    budget.setDisplayValue(formatCurrency(cents, "USD"));
                    return budget;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Extract box office data with regions
     */
    private List<BoxOfficeData> extractBoxOffice(JsonNode entity) {
        List<BoxOfficeData> results = new ArrayList<>();
        JsonNode claims = entity.path("claims").path("P2142");

        for (JsonNode claim : claims) {
            BoxOfficeData data = new BoxOfficeData();

            String amount = claim.path("mainsnak").path("datavalue")
                    .path("value").path("amount").asText();

            if (!amount.isEmpty()) {
                amount = amount.replace("+", "");
                try {
                    long cents = (long) (Double.parseDouble(amount) * 100);
                    data.setAmount(cents);
                    data.setCurrency("USD");
                    data.setDisplayValue(formatCurrency(cents, "USD"));
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // Get region from qualifiers
            JsonNode qualifiers = claim.path("qualifiers").path("P3005");
            if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                String regionId = qualifiers.get(0).path("datavalue")
                        .path("value").path("id").asText();
                data.setRegion(fetchLabelById(regionId));
            } else {
                data.setRegion("worldwide");
            }

            results.add(data);
        }

        return results;
    }

    /**
     * Extract review scores
     */
    private List<ReviewScore> extractReviewScores(JsonNode entity) {
        List<ReviewScore> scores = new ArrayList<>();
        JsonNode claims = entity.path("claims").path("P444");

        for (JsonNode claim : claims) {
            ReviewScore score = new ReviewScore();

            String value = claim.path("mainsnak").path("datavalue").path("value").asText();
            score.setValue(value);

            JsonNode qualifiers = claim.path("qualifiers");

            // Review source
            JsonNode reviewerNode = qualifiers.path("P447");
            if (reviewerNode.isArray() && !reviewerNode.isEmpty()) {
                String reviewerId = reviewerNode.get(0).path("datavalue")
                        .path("value").path("id").asText();
                score.setSource(fetchLabelById(reviewerId));
            }

            // Score type
            JsonNode methodNode = qualifiers.path("P459");
            if (methodNode.isArray() && !methodNode.isEmpty()) {
                String methodId = methodNode.get(0).path("datavalue")
                        .path("value").path("id").asText();
                score.setScoreType(fetchLabelById(methodId));
            }

            // Number of reviews
            JsonNode numReviewsNode = qualifiers.path("P3744");
            if (numReviewsNode.isArray() && !numReviewsNode.isEmpty()) {
                String numStr = numReviewsNode.get(0).path("datavalue")
                        .path("value").path("amount").asText().replace("+", "");
                try {
                    score.setNumReviews(Integer.parseInt(numStr));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            // Review date
            JsonNode dateNode = qualifiers.path("P585");
            if (dateNode.isArray() && !dateNode.isEmpty()) {
                String date = dateNode.get(0).path("datavalue").path("value")
                        .path("time").asText().replaceAll("\\+|T.*", "");
                score.setReviewDate(date);
            }

            if (score.getSource() != null) {
                scores.add(score);
            }
        }

        return scores;
    }

    /**
     * Extract content ratings from multiple rating systems
     */
    private List<ContentRating> extractContentRatings(JsonNode entity) {
        List<ContentRating> ratings = new ArrayList<>();

        // MPA (P1657), BBFC (P2758), FSK (P1981), EIRIN (P2980),
        // CNC (P1562), RARS (P5458)
        ratings.addAll(extractRatingByProperty(entity, "P1657", "MPA"));
        ratings.addAll(extractRatingByProperty(entity, "P2758", "BBFC"));
        ratings.addAll(extractRatingByProperty(entity, "P1981", "FSK"));
        ratings.addAll(extractRatingByProperty(entity, "P2980", "EIRIN"));
        ratings.addAll(extractRatingByProperty(entity, "P1562", "CNC"));
        ratings.addAll(extractRatingByProperty(entity, "P5458", "RARS"));

        return ratings;
    }

    /**
     * Extract rating by property
     */
    private List<ContentRating> extractRatingByProperty(JsonNode entity, String property, String system) {
        List<ContentRating> ratings = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
            ContentRating rating = new ContentRating();
            rating.setSystem(system);

            String ratingId = claim.path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();

            if (!ratingId.isEmpty()) {
                rating.setValue(fetchLabelById(ratingId));
            }

            JsonNode qualifiers = claim.path("qualifiers");

            // Content descriptors
            List<String> descriptors = new ArrayList<>();
            JsonNode descriptorNodes = qualifiers.path("P2614");
            for (JsonNode node : descriptorNodes) {
                String descId = node.path("datavalue").path("value").path("id").asText();
                if (!descId.isEmpty()) {
                    descriptors.add(fetchLabelById(descId));
                }
            }
            if (!descriptors.isEmpty()) {
                rating.setContentDescriptors(String.join(", ", descriptors));
            }

            // Start date
            JsonNode dateNode = qualifiers.path("P580");
            if (dateNode.isArray() && !dateNode.isEmpty()) {
                String date = dateNode.get(0).path("datavalue").path("value")
                        .path("time").asText().replaceAll("\\+|T.*", "");
                rating.setStartDate(date);
            }

            // Distribution format
            JsonNode formatNode = qualifiers.path("P437");
            if (formatNode.isArray() && !formatNode.isEmpty()) {
                String formatId = formatNode.get(0).path("datavalue")
                        .path("value").path("id").asText();
                rating.setDistributionFormat(fetchLabelById(formatId));
            }

            ratings.add(rating);
        }

        return ratings;
    }

    /**
     * Extract sequel information
     */
    private String extractFollowedBy(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P156");
        if (claims.isArray() && !claims.isEmpty()) {
            String sequelId = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();
            return fetchLabelById(sequelId);
        }
        return null;
    }

    /**
     * Extract series information
     */
    private String extractPartOfSeries(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P179");
        if (claims.isArray() && !claims.isEmpty()) {
            String seriesId = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();
            return fetchLabelById(seriesId);
        }
        return null;
    }

    // ==================== WIKIDATA FETCHING METHODS ====================

    /**
     * Fetch person details from Wikidata
     */
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

    /**
     * Fetch company details from Wikidata
     */
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

    /**
     * Fetch label by Wikidata ID
     */
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

    // ==================== BASIC EXTRACTION HELPERS ====================

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
            JsonNode timeValue = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("time");
            if (!timeValue.isMissingNode()) {
                return timeValue.asText().replaceAll("\\+|T.*", "");
            }
        }
        return null;
    }

    private String extractEntityLabel(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && !claims.isEmpty()) {
            String id = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();
            return fetchLabelById(id);
        }
        return null;
    }

    private List<String> extractEntityLabels(JsonNode entity, String property) {
        List<String> result = new ArrayList<>();
        JsonNode claims = entity.path("claims").path(property);

        for (JsonNode claim : claims) {
            String id = claim.path("mainsnak").path("datavalue")
                    .path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) result.add(label);
        }

        return result;
    }

    private String extractDuration(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2047");
        if (claims.isArray() && !claims.isEmpty()) {
            JsonNode amount = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").path("amount");
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
            String filename = claims.get(0).path("mainsnak").path("datavalue")
                    .path("value").asText();
            return generateCommonsUrl(filename);
        }
        return null;
    }

    /**
     * Extract full video URL (full film)
     * Looks for P10 (video) with qualifier P3831=Q89347362 (full video available on Wikimedia Commons)
     */
    private String extractVideoUrl(JsonNode entity) {
        JsonNode videoClaims = entity.path("claims").path("P10");
        if (videoClaims.isArray()) {
            for (JsonNode claim : videoClaims) {
                JsonNode qualifiers = claim.path("qualifiers").path("P3831");
                if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                    String roleId = qualifiers.get(0).path("datavalue")
                            .path("value").path("id").asText("");
                    if ("Q89347362".equals(roleId)) {  // Full video
                        String filename = claim.path("mainsnak").path("datavalue")
                                .path("value").asText();
                        return generateCommonsUrl(filename);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extract trailer URL (NEW METHOD)
     * Looks for P10 (video) with qualifier P3831=Q622550 (trailer)
     */
    private String extractTrailerUrl(JsonNode entity) {
        JsonNode videoClaims = entity.path("claims").path("P10");
        if (videoClaims.isArray()) {
            for (JsonNode claim : videoClaims) {
                JsonNode qualifiers = claim.path("qualifiers").path("P3831");
                if (qualifiers.isArray() && !qualifiers.isEmpty()) {
                    String roleId = qualifiers.get(0).path("datavalue")
                            .path("value").path("id").asText("");
                    if ("Q622550".equals(roleId)) {  // Trailer
                        String filename = claim.path("mainsnak").path("datavalue")
                                .path("value").asText();
                        return generateCommonsUrl(filename);
                    }
                }
            }
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

    private String extractSubtitleUrl(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("/")) return null;

        try {
            String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            String subtitleIdUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".id.srt";
            String subtitleEnUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".en.srt";

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());

            ResponseEntity<String> responseId = restTemplate.exchange(
                    subtitleIdUrl, HttpMethod.GET, entity, String.class);
            if (responseId.getStatusCode() == HttpStatus.OK &&
                    responseId.getBody() != null &&
                    responseId.getBody().contains("<pre")) {
                return subtitleIdUrl;
            }

            ResponseEntity<String> responseEn = restTemplate.exchange(
                    subtitleEnUrl, HttpMethod.GET, entity, String.class);
            if (responseEn.getStatusCode() == HttpStatus.OK &&
                    responseEn.getBody() != null &&
                    responseEn.getBody().contains("<pre")) {
                return subtitleEnUrl;
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Generate Wikimedia Commons URL from filename
     */
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
            return "https://upload.wikimedia.org/wikipedia/commons/" +
                    md5.charAt(0) + "/" + md5.substring(0, 2) + "/" + encodedFile;
        } catch (Exception e) {
            return "https://commons.wikimedia.org/wiki/File:" + filename.replace(" ", "_");
        }
    }

    /**
     * Parse subtitle URL to raw format
     */
    private String parseSubtitleUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        if (url.contains("/wiki/TimedText:")) {
            int startIndex = url.indexOf("/wiki/") + 6;
            String title = url.substring(startIndex);
            return "https://commons.wikimedia.org/w/index.php?title=" + title + "&action=raw";
        }

        if (url.contains("action=raw") || url.endsWith(".srt") || url.endsWith(".vtt")) {
            return url;
        }

        return null;
    }

    /**
     * Convert SRT to VTT format
     */
    private String convertSrtToVtt(String srtContent) {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        String converted = srtContent.replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2");
        String[] blocks = converted.split("\\n\\n");

        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;

            String[] lines = block.split("\\n");
            if (lines.length < 2) continue;

            boolean timestampFound = false;
            for (String line : lines) {
                if (line.matches("^\\d+$")) continue;

                if (line.contains("-->")) {
                    timestampFound = true;
                    vtt.append(line).append("\n");
                } else if (timestampFound) {
                    vtt.append(line).append("\n");
                }
            }

            if (timestampFound) {
                vtt.append("\n");
            }
        }

        return vtt.toString();
    }

    /**
     * Extract filename from URL
     */
    private String extractFilename(String url) {
        if (url == null || !url.contains("/")) return null;

        try {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Format currency
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