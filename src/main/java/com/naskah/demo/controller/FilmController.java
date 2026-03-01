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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Slf4j
@RestController
@RequestMapping("/api/films")
@CrossOrigin(origins = "*")
public class FilmController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired private FilmService filmService;
    @Autowired private FilmMapper filmMapper;

    // ==================== BATCH STATE ====================

    private ThreadPoolExecutor batchExecutor;
    private final AtomicBoolean batchRunning = new AtomicBoolean(false);
    private final AtomicBoolean batchPaused  = new AtomicBoolean(false);
    private final AtomicInteger batchTotal   = new AtomicInteger(0);
    private final AtomicInteger batchDone    = new AtomicInteger(0);
    private final AtomicInteger batchFailed  = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<String> pendingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> errorLog     = new ConcurrentLinkedQueue<>();

    private static final int DEFAULT_THREADS = 10;
    private static final int MAX_THREADS     = 50;

    @PostConstruct
    public void initBatchExecutor() {
        batchExecutor = buildExecutor(DEFAULT_THREADS);
    }

    @PreDestroy
    public void shutdownBatchExecutor() {
        if (batchExecutor != null) batchExecutor.shutdownNow();
    }

    private ThreadPoolExecutor buildExecutor(int threads) {
        return new ThreadPoolExecutor(
                threads, threads, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "batch-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    // ==================== HEADERS ====================

    private HttpHeaders createHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("User-Agent", "FilmApp/1.0 (Metadata Extractor)");
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return h;
    }

    // ==================== ROUTES ====================

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchFilms(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Film> films = filmService.searchFilms(q, page, size);
        int total = filmService.getTotalSearchResults(q);
        Map<String, Object> res = new HashMap<>();
        res.put("films", films); res.put("query", q);
        res.put("currentPage", page); res.put("totalItems", total);
        res.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = "/person/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Person> getPersonBySlug(@PathVariable String slug) {
        Person p = filmMapper.findPersonBySlug(slug);
        return p == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(p);
    }

    @GetMapping(value = "/company/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Company> getCompanyBySlug(@PathVariable String slug) {
        Company c = filmMapper.findCompanyBySlug(slug);
        return c == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(c);
    }

    @GetMapping(value = "/wikidata/{qid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmFromWikidata(@PathVariable String qid) {
        try {
            log.info("=== FETCHING: {} ===", qid);
            FilmDetail cached = filmService.getFilmDetailByQid(qid);
            if (cached != null) { log.info("Cache hit: {}", qid); return ResponseEntity.ok(cached); }
            FilmDetail fd = fetchAndBuildFilmDetail(qid);
            Film saved = filmService.saveFilm(fd);
            fd.setId(saved.getId());
            return ResponseEntity.ok(fd);
        } catch (Exception e) {
            log.error("ERROR for QID {}: {}", qid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== BATCH ENDPOINTS ====================

    @PostMapping(value = "/batch/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> batchImportCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipExisting", defaultValue = "true") boolean skipExisting,
            @RequestParam(value = "threads", defaultValue = "10") int threads) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File CSV kosong"));
        }
        if (batchRunning.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Batch sedang berjalan. Panggil /batch/stop dulu jika ingin membatalkan."));
        }

        try {
            List<String> qids = parseCsvQids(file);
            if (qids.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tidak ada QID valid di CSV"));
            }
            return startBatch(qids, skipExisting, threads);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/batch/import/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> batchImportJson(
            @RequestBody Map<String, Object> body,
            @RequestParam(value = "skipExisting", defaultValue = "true") boolean skipExisting,
            @RequestParam(value = "threads", defaultValue = "10") int threads) {

        if (batchRunning.get()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Batch sedang berjalan. Panggil /batch/stop dulu jika ingin membatalkan."));
        }

        @SuppressWarnings("unchecked")
        List<String> raw = (List<String>) body.get("qids");
        if (raw == null || raw.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Field 'qids' wajib diisi"));
        }

        List<String> qids = raw.stream()
                .map(String::trim)
                .filter(q -> q.matches("Q\\d+"))
                .toList();

        return startBatch(qids, skipExisting, threads);
    }

    @GetMapping("/batch/status")
    public ResponseEntity<Map<String, Object>> batchStatus() {
        int total   = batchTotal.get();
        int done    = batchDone.get();
        int failed  = batchFailed.get();
        int active  = batchExecutor != null ? batchExecutor.getActiveCount() : 0;
        long queued = batchExecutor != null ? batchExecutor.getQueue().size() : 0;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("running",  batchRunning.get());
        res.put("total",    total);
        res.put("done",     done);
        res.put("failed",   failed);
        res.put("waitingInQueue", queued);
        res.put("activeThreads", active);
        res.put("paused",   batchPaused.get());
        res.put("progressPercent", total == 0 ? 0 : Math.round(done * 100.0 / total * 10) / 10.0);
        res.put("recentErrors", errorLog.stream().toList());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/batch/pause")
    public ResponseEntity<Map<String, Object>> batchPause() {
        batchPaused.set(true);
        return ResponseEntity.ok(Map.of("status", "paused"));
    }

    @PostMapping("/batch/resume")
    public ResponseEntity<Map<String, Object>> batchResume() {
        batchPaused.set(false);
        return ResponseEntity.ok(Map.of("status", "resumed"));
    }

    @PostMapping("/batch/stop")
    public ResponseEntity<Map<String, Object>> batchStop() {
        batchRunning.set(false);
        batchPaused.set(false);
        if (batchExecutor != null) {
            batchExecutor.shutdownNow();
            batchExecutor = buildExecutor(DEFAULT_THREADS);
        }
        pendingQueue.clear();
        return ResponseEntity.ok(Map.of("status", "stopped",
                "done", batchDone.get(), "failed", batchFailed.get()));
    }

    // ==================== BATCH CORE LOGIC ====================

    private ResponseEntity<Map<String, Object>> startBatch(
            List<String> allQids, boolean skipExisting, int threads) {

        int threadCount = Math.max(1, Math.min(threads, MAX_THREADS));

        // Rebuild executor dengan jumlah thread yang diminta
        if (batchExecutor != null) batchExecutor.shutdownNow();
        batchExecutor = buildExecutor(threadCount);

        // Filter QID yang sudah ada di DB
        List<String> toProcess = new ArrayList<>();
        int skipped = 0;
        for (String qid : allQids) {
            if (skipExisting && filmService.existsByQid(qid)) { skipped++; continue; }
            toProcess.add(qid);
        }

        if (toProcess.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "queued", 0, "skipped", skipped, "message", "Semua QID sudah ada di DB"));
        }

        // Reset counter
        batchTotal.set(toProcess.size());
        batchDone.set(0);
        batchFailed.set(0);
        batchPaused.set(false);
        batchRunning.set(true);
        pendingQueue.clear();
        pendingQueue.addAll(toProcess);
        errorLog.clear();

        // Submit SEMUA sekaligus — tidak ada batching, tidak ada chunking
        for (String qid : toProcess) {
            batchExecutor.submit(() -> processOneBatchQid(qid));
        }

        log.info("Batch dimulai: {} QID total, {} di-skip, {} thread paralel",
                toProcess.size(), skipped, threadCount);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("queued",  toProcess.size());
        res.put("skipped", skipped);
        res.put("threads", threadCount);
        res.put("message", String.format(
                "Semua %d QID langsung masuk queue. %d berjalan paralel, sisanya menunggu di antrian.",
                toProcess.size(), threadCount));
        return ResponseEntity.ok(res);
    }

    private void processOneBatchQid(String qid) {
        if (!batchRunning.get()) return;

        while (batchPaused.get()) {
            if (!batchRunning.get()) return;
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return;
            }
        }

        pendingQueue.remove(qid);

        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            if (!batchRunning.get()) return;
            try {
                Thread.sleep(150); // jeda kecil antar request

                if (filmService.existsByQid(qid)) {
                    log.debug("Skip (sudah ada): {}", qid);
                    batchDone.incrementAndGet();
                    checkBatchComplete();
                    return;
                }

                FilmDetail fd = fetchAndBuildFilmDetail(qid);
                filmService.saveFilm(fd);
                batchDone.incrementAndGet();
                log.info("[{}/{}] Done: {}", batchDone.get(), batchTotal.get(), qid);
                checkBatchComplete();
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                String errMsg = qid + ": " + e.getMessage();
                log.warn("[attempt {}/{}] Gagal: {}", attempt, maxRetry, errMsg);
                if (attempt == maxRetry) {
                    batchFailed.incrementAndGet();
                    errorLog.offer(errMsg);
                    while (errorLog.size() > 100) errorLog.poll();
                    checkBatchComplete();
                } else {
                    try { Thread.sleep((long) Math.pow(2, attempt) * 1000L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
    }

    private void checkBatchComplete() {
        if (batchDone.get() + batchFailed.get() >= batchTotal.get()) {
            batchRunning.set(false);
            log.info("=== BATCH SELESAI: {} berhasil, {} gagal dari {} total ===",
                    batchDone.get(), batchFailed.get(), batchTotal.get());
        }
    }

    // ==================== CORE WIKIDATA FETCH ====================

    FilmDetail fetchAndBuildFilmDetail(String qid) throws Exception {
        String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(createHeaders()), String.class);
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode entity = root.path("entities").path(qid);

        FilmDetail fd = new FilmDetail();
        fd.setWikidataQid(qid);

        String judul = getLabel(entity);
        fd.setJudul(judul);

        String slugTitle = resolveSlugTitle(entity, judul, qid);
        fd.setJudulSlug(slugTitle);
        log.info("[{}] judul='{}' | slugTitle='{}'", qid, judul, slugTitle);

        fd.setTahunRilis(extractDate(entity));
        fd.setJenis(extractEntityLabel(entity, "P31"));
        fd.setGenre(extractGenres(entity));
        fd.setDeskripsi(extractDeskripsiIndonesian(entity, qid));

        fd.setSutradara(extractPersonsWithDetails(entity, "P57"));
        fd.setPenulisSkenario(extractPersonsWithDetails(entity, "P58"));
        fd.setPemeran(extractPersonsWithDetails(entity, "P161"));
        fd.setProduser(extractPersonsWithDetails(entity, "P162"));
        fd.setFilmEditor(extractPersonsWithDetails(entity, "P1040"));
        fd.setCinematographer(extractPersonsWithDetails(entity, "P344"));
        fd.setComposer(extractPersonsWithDetails(entity, "P86"));

        fd.setPerusahaanProduksi(extractCompaniesWithDetails(entity, "P272"));
        fd.setDistributor(extractCompaniesWithDetails(entity, "P750"));

        fd.setNegaraAsal(extractEntityLabel(entity, "P495"));
        fd.setNarrativeLocation(extractEntityLabels(entity, "P840"));
        fd.setFilmingLocation(extractEntityLabels(entity, "P915"));
        fd.setDurasi(extractDuration(entity));

        fd.setColor(extractColor(entity));
        fd.setOriginalLanguage(extractOriginalLanguage(entity));
        fd.setPosterUrl(extractPosterUrl(entity));
        fd.setImageUrls(extractImageUrls(entity));
        fd.setVideoUrl(extractVideoUrl(entity));
        fd.setTrailerUrl(extractTrailerUrl(entity));
        fd.setSubtitleUrl(extractSubtitleUrl(fd.getVideoUrl()));

        fd.setBudget(extractBudget(entity));
        fd.setBoxOffice(extractBoxOffice(entity));
        fd.setReviewScores(extractReviewScores(entity));
        fd.setContentRatings(extractContentRatings(entity));

        fd.setFollowedBy(extractFollowedBy(entity));
        fd.setPartOfSeries(extractPartOfSeries(entity));
        fd.setAliasIndonesia(extractAliases(entity));

        log.info("=== DONE: {} | deskripsi={}chars ===", qid,
                fd.getDeskripsi() != null ? fd.getDeskripsi().length() : 0);

        return fd;
    }

    // ==================== SLUG TITLE RESOLUTION ====================

    private String resolveSlugTitle(JsonNode entity, String judul, String qid) {

        // Prioritas 1: P1476 + P2441 EN
        String p1476En = extractEnglishTitleFromP1476(entity);
        if (isValid(p1476En) && !isNonLatin(p1476En)) {
            log.info("[{}] Slug title dari P1476/P2441: {}", qid, p1476En);
            return p1476En;
        }

        // Prioritas 2: Label "en"
        String labelEn = getLabelByLang(entity, "en");
        if (isValid(labelEn) && !isNonLatin(labelEn)) {
            log.info("[{}] Slug title dari label EN: {}", qid, labelEn);
            return labelEn;
        }

        // Prioritas 3: Label "mul" (multilingual — Wikidata menggunakannya untuk judul resmi EN)
        String labelMul = getLabelByLang(entity, "mul");
        if (isValid(labelMul) && !isNonLatin(labelMul)) {
            log.info("[{}] Slug title dari label MUL: {}", qid, labelMul);
            return labelMul;
        }

        // Prioritas 4: Label Latin lainnya
        String[] latinLangs = {"it", "es", "de", "fr", "nl", "pt", "sv", "pl", "ro", "cs", "hu", "fi"};
        for (String lang : latinLangs) {
            String label = getLabelByLang(entity, lang);
            if (isValid(label) && !isNonLatin(label)) {
                log.info("[{}] Slug title dari label {}: {}", qid, lang, label);
                return label;
            }
        }

        // Prioritas 5: Translate via MyMemory jika judul non-latin
        if (isNonLatin(judul)) {
            log.info("[{}] Judul non-latin '{}', mencoba translate ke EN...", qid, judul);
            String translated = translateTitleToEnglish(judul, qid);
            if (isValid(translated) && !isNonLatin(translated)) {
                log.info("[{}] Slug title dari translate: {}", qid, translated);
                return translated;
            }
            log.warn("[{}] Translate gagal, fallback ke QID", qid);
        }

        // Prioritas 6: Judul asli jika latin
        if (isValid(judul) && !isNonLatin(judul)) return judul;

        // Last resort: QID lowercase
        return qid.toLowerCase();
    }

    private String extractEnglishTitleFromP1476(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P1476")) {
            JsonNode p2441 = claim.path("qualifiers").path("P2441");
            if (p2441.isArray()) {
                for (JsonNode q : p2441) {
                    String lang = q.path("datavalue").path("value").path("language").asText("");
                    String text = q.path("datavalue").path("value").path("text").asText("");
                    if ("en".equals(lang) && !text.isBlank()) return text;
                }
            }
            String lang = claim.path("mainsnak").path("datavalue").path("value").path("language").asText("");
            String text = claim.path("mainsnak").path("datavalue").path("value").path("text").asText("");
            if ("en".equals(lang) && !text.isBlank()) return text;
        }
        return null;
    }

    boolean isNonLatin(String text) {
        if (text == null || text.isBlank()) return false;
        long nonLatin = text.chars().filter(c -> !Character.isWhitespace(c)).filter(c -> c > 127).count();
        long total = text.chars().filter(c -> !Character.isWhitespace(c)).count();
        return total > 0 && (double) nonLatin / total > 0.3;
    }

    private String translateTitleToEnglish(String title, String qid) {
        try {
            String url = "https://api.mymemory.translated.net/get?q=" +
                    URLEncoder.encode(title, StandardCharsets.UTF_8) + "&langpair=autodetect|en";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                if (root.path("responseStatus").asInt() == 206) {
                    log.warn("[{}] MyMemory quota exceeded untuk translate judul", qid);
                    return null;
                }
                String translated = root.path("responseData").path("translatedText").asText("");
                if (!translated.isBlank() && !translated.equalsIgnoreCase(title)) return translated;
            }
        } catch (Exception e) {
            log.warn("[{}] translateTitleToEnglish gagal untuk '{}': {}", qid, title, e.getMessage());
        }
        return null;
    }

    // ==================== CSV PARSING ====================

    private List<String> parseCsvQids(MultipartFile file) throws Exception {
        List<String> qids = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>(); // deduplikasi, pertahankan urutan
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim());
            }
        }
        if (lines.isEmpty()) return qids;

        // Deteksi header
        String firstLine = lines.get(0);
        boolean hasHeader = firstLine.toLowerCase().contains("qid") ||
                firstLine.toLowerCase().contains("wikidata") ||
                firstLine.toLowerCase().contains("item") ||
                firstLine.startsWith("?");
        int start = hasHeader ? 1 : 0;
        java.util.regex.Pattern qidPattern = java.util.regex.Pattern.compile("(?:entity/)?(Q\\d+)");

        for (int i = start; i < lines.size(); i++) {
            String[] cols = lines.get(i).split(",");
            if (cols.length == 0) continue;

            String cell = cols[0].trim().replaceAll("\"", "");
            java.util.regex.Matcher m = qidPattern.matcher(cell);
            if (m.find()) {
                String qid = m.group(1);
                seen.add(qid);
            }
        }

        qids.addAll(seen);

        // Debug sample rows tetap ada, tapi batasi agar tidak spam log
        log.info("CSV: {} QID unik valid dari {} baris", qids.size(), lines.size() - start);
        return qids;
    }

    // ==================== EXISTING ROUTES ====================

    @GetMapping(value = "/{filmSlug}/subtitle", produces = "text/vtt")
    public ResponseEntity<String> getSubtitle(@PathVariable String filmSlug) {
        try {
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getSubtitleUrl() == null) return ResponseEntity.notFound().build();
            String rawUrl = parseSubtitleUrl(film.getSubtitleUrl());
            if (rawUrl == null) return ResponseEntity.notFound().build();
            HttpHeaders h = new HttpHeaders(); h.set("User-Agent", "FilmApp/1.0");
            ResponseEntity<String> r = restTemplate.exchange(rawUrl, HttpMethod.GET, new HttpEntity<>(h), String.class);
            if (r.getBody() == null || r.getBody().isEmpty()) return ResponseEntity.notFound().build();
            HttpHeaders rh = new HttpHeaders();
            rh.setContentType(MediaType.valueOf("text/vtt; charset=UTF-8"));
            rh.set("Cache-Control", "public, max-age=86400");
            return new ResponseEntity<>(convertSrtToVtt(r.getBody()), rh, HttpStatus.OK);
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @GetMapping(value = "/{filmSlug}/video-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoInfo(@PathVariable String filmSlug) {
        try {
            Film film = filmMapper.findBySlug(filmSlug);
            if (film == null || film.getVideoUrl() == null) return ResponseEntity.notFound().build();
            String filename = extractFilename(film.getVideoUrl());
            if (filename == null) return ResponseEntity.badRequest().build();
            String apiUrl = UriComponentsBuilder.fromHttpUrl("https://commons.wikimedia.org/w/api.php")
                    .queryParam("action", "query").queryParam("titles", "File:" + filename)
                    .queryParam("prop", "videoinfo").queryParam("viprop", "derivatives|url|size|mediatype")
                    .queryParam("format", "json").build().toUriString();
            HttpHeaders h = new HttpHeaders(); h.set("User-Agent", "FilmApp/1.0");
            ResponseEntity<String> r = restTemplate.exchange(apiUrl, HttpMethod.GET, new HttpEntity<>(h), String.class);
            JsonNode pages = mapper.readTree(r.getBody()).path("query").path("pages");
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
                q.put("type", d.path("type").asText()); q.put("src", d.path("src").asText());
                q.put("width", d.path("width").asInt()); q.put("height", d.path("height").asInt());
                q.put("label", d.path("height").asInt() + "p"); qualities.add(q);
            }
            qualities.sort((a, b) -> Integer.compare((int) b.get("height"), (int) a.get("height")));
            result.put("qualities", qualities);
            return ResponseEntity.ok(result);
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllFilms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Film> films = filmService.getAllFilms(page, size);
        int total = filmService.getTotalFilms();
        Map<String, Object> res = new HashMap<>();
        res.put("films", films); res.put("currentPage", page);
        res.put("totalItems", total); res.put("totalPages", (int) Math.ceil((double) total / size));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FilmDetail> getFilmBySlug(@PathVariable String slug) {
        FilmDetail film = filmService.getFilmDetailBySlug(slug);
        return film == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(film);
    }

    // ==================== DESKRIPSI: STRATEGI BERTINGKAT ====================

    private String extractDeskripsiIndonesian(JsonNode entity, String qid) {
        JsonNode sitelinks = entity.path("sitelinks");

        if (sitelinks.has("idwiki")) {
            String title = sitelinks.path("idwiki").path("title").asText();
            log.info("[{}] Step 1: Wikipedia BI → '{}'", qid, title);
            String text = fetchWikipediaSummary(title, "id");
            if (isValid(text)) { log.info("[{}] ✓ Step 1 berhasil ({}chars)", qid, text.length()); return text; }
            log.warn("[{}] Step 1 gagal", qid);
        }

        if (sitelinks.has("mswiki")) {
            String title = sitelinks.path("mswiki").path("title").asText();
            log.info("[{}] Step 2: Wikipedia Melayu → '{}'", qid, title);
            String msText = fetchWikipediaSummary(title, "ms");
            if (isValid(msText)) {
                String adapted = adaptMelayuToIndonesian(msText);
                log.info("[{}] ✓ Step 2 berhasil ({}chars)", qid, adapted.length());
                return adapted;
            }
            log.warn("[{}] Step 2 gagal", qid);
        }

        if (sitelinks.has("enwiki")) {
            String title = sitelinks.path("enwiki").path("title").asText();
            log.info("[{}] Step 3: Wikipedia EN → '{}'", qid, title);
            String enText = fetchWikipediaSummary(title, "en");
            if (isValid(enText)) {
                String translated = translateWithFallback(enText, qid);
                if (isValid(translated) && !translated.equalsIgnoreCase(enText)) {
                    log.info("[{}] ✓ Step 3 berhasil ({}chars)", qid, translated.length());
                    return translated;
                }
                log.warn("[{}] Translate gagal, simpan EN as-is", qid);
                return enText;
            }
            log.warn("[{}] Step 3 gagal", qid);
        }

        String idDesc = getDescriptionById(entity);
        if (isValid(idDesc)) { log.info("[{}] Step 4: Wikidata desc ID", qid); return idDesc; }

        String enDesc = getDescriptionByEn(entity);
        if (isValid(enDesc)) {
            String translated = translateWithFallback(enDesc, qid);
            if (isValid(translated) && !translated.equalsIgnoreCase(enDesc)) return translated;
            return enDesc;
        }

        return null;
    }

    private String adaptMelayuToIndonesian(String melayuText) {
        if (melayuText == null) return null;
        String[][] replacements = {
                {"filem", "film"}, {"pawagam", "bioskop"}, {"pengarah", "sutradara"},
                {"pengeluar", "produser"}, {"pelakon", "pemeran"}, {"tayangan", "penayangan"},
                {"ditayangkan", "ditayangkan"}, {"awam", "umum"}, {"kerajaan", "pemerintah"},
                {"syarikat", "perusahaan"}, {"perniagaan", "bisnis"}, {"wang", "uang"},
                {"berjaya", "berhasil"}, {"daripada", "dari"}, {"dilancarkan", "diluncurkan"},
                {"pelancaran", "peluncuran"}, {"beliau", "ia"}, {"bagi", "untuk"},
        };
        String result = melayuText;
        for (String[] pair : replacements) {
            result = result.replaceAll("(?i)\\b" + pair[0] + "\\b", pair[1]);
        }
        return result;
    }

    private String fetchWikipediaSummary(String pageTitle, String lang) {
        try {
            String encodedTitle = URLEncoder.encode(pageTitle.replace(" ", "_"), StandardCharsets.UTF_8)
                    .replace("+", "_").replace("%28", "(").replace("%29", ")").replace("%E2%80%93", "–");
            String wikiUrl = "https://" + lang + ".wikipedia.org/api/rest_v1/page/summary/" + encodedTitle;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    wikiUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode wikiRoot = mapper.readTree(response.getBody());
                if (wikiRoot.path("type").asText("").contains("not_found")) return null;
                String extract = wikiRoot.path("extract").asText("");
                return extract.isEmpty() ? null : extract;
            }
        } catch (Exception e) { log.warn("Wikipedia {} gagal '{}': {}", lang, pageTitle, e.getMessage()); }
        return null;
    }

    private String translateWithFallback(String text, String qid) {
        if (!isValid(text)) return null;
        String toTranslate = truncateAtSentence(text, 4500);
        String result = translateViaMyMemory(toTranslate, qid);
        if (isValid(result) && !result.equalsIgnoreCase(toTranslate)) return result;
        result = translateViaLibreTranslate(toTranslate, qid);
        if (isValid(result) && !result.equalsIgnoreCase(toTranslate)) return result;
        return null;
    }

    private String translateViaMyMemory(String text, String qid) {
        try {
            String url = "https://api.mymemory.translated.net/get?q=" +
                    URLEncoder.encode(text, StandardCharsets.UTF_8) + "&langpair=en|id";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "FilmApp/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                if (root.path("responseStatus").asInt() == 206) {
                    log.warn("[{}] MyMemory quota exceeded", qid); return null;
                }
                String translated = root.path("responseData").path("translatedText").asText("");
                return translated.isEmpty() ? null : translated;
            }
        } catch (Exception e) { log.warn("[{}] MyMemory error: {}", qid, e.getMessage()); }
        return null;
    }

    private String translateViaLibreTranslate(String text, String qid) {
        String[] instances = {
                "https://translate.terraprint.co",
                "https://libretranslate.de",
                "https://translate.argosopentech.com"
        };
        for (String baseUrl : instances) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "FilmApp/1.0");
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                Map<String, String> body = new HashMap<>();
                body.put("q", text); body.put("source", "en"); body.put("target", "id");
                body.put("format", "text"); body.put("api_key", "");
                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/translate", HttpMethod.POST,
                        new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    String translated = mapper.readTree(response.getBody()).path("translatedText").asText("");
                    if (!translated.isEmpty()) return translated;
                }
            } catch (Exception e) { log.warn("[{}] LibreTranslate ({}) gagal: {}", qid, baseUrl, e.getMessage()); }
        }
        return null;
    }

    // ==================== IMAGE EXTRACTION ====================

    private static final Set<String> POSTER_ROLE_QIDS = new HashSet<>(Arrays.asList("Q429785", "Q14660174"));

    private String extractPosterUrl(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P3383")) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty()) return generateCommonsUrl(fn);
        }
        JsonNode p18 = entity.path("claims").path("P18");
        if (!p18.isArray() || p18.isEmpty()) return null;
        for (JsonNode claim : p18) {
            JsonNode q3831 = claim.path("qualifiers").path("P3831");
            if (q3831.isArray() && !q3831.isEmpty()) {
                String roleId = q3831.get(0).path("datavalue").path("value").path("id").asText("");
                if (POSTER_ROLE_QIDS.contains(roleId)) {
                    String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                    if (!fn.isEmpty()) return generateCommonsUrl(fn);
                }
            }
        }
        for (JsonNode claim : p18) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty() && fn.toLowerCase().contains("poster")) return generateCommonsUrl(fn);
        }
        for (JsonNode claim : p18) {
            if (!claim.path("qualifiers").has("P3831")) {
                String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                if (!fn.isEmpty()) return generateCommonsUrl(fn);
            }
        }
        String fn = p18.get(0).path("mainsnak").path("datavalue").path("value").asText();
        return fn.isEmpty() ? null : generateCommonsUrl(fn);
    }

    private List<String> extractImageUrls(JsonNode entity) {
        List<String> images = new ArrayList<>();
        JsonNode p18 = entity.path("claims").path("P18");
        if (p18.isArray() && !p18.isEmpty()) {
            Set<String> posterFilenames = new HashSet<>();
            for (JsonNode claim : entity.path("claims").path("P3383")) {
                String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                if (!fn.isEmpty()) posterFilenames.add(fn);
            }
            for (JsonNode claim : p18) {
                JsonNode q3831 = claim.path("qualifiers").path("P3831");
                if (q3831.isArray() && !q3831.isEmpty()) {
                    String roleId = q3831.get(0).path("datavalue").path("value").path("id").asText("");
                    if (POSTER_ROLE_QIDS.contains(roleId)) {
                        String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                        if (!fn.isEmpty()) posterFilenames.add(fn);
                    }
                }
            }
            for (JsonNode claim : p18) {
                String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                if (!fn.isEmpty() && fn.toLowerCase().contains("poster")) posterFilenames.add(fn);
            }
            if (posterFilenames.isEmpty()) {
                for (JsonNode claim : p18) {
                    if (!claim.path("qualifiers").has("P3831")) {
                        String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                        if (!fn.isEmpty()) { posterFilenames.add(fn); break; }
                    }
                }
                if (posterFilenames.isEmpty()) {
                    String fn = p18.get(0).path("mainsnak").path("datavalue").path("value").asText();
                    if (!fn.isEmpty()) posterFilenames.add(fn);
                }
            }
            for (JsonNode claim : p18) {
                String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
                if (!fn.isEmpty() && !posterFilenames.contains(fn)) images.add(generateCommonsUrl(fn));
            }
        }
        for (JsonNode claim : entity.path("claims").path("P6802")) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty()) images.add(generateCommonsUrl(fn));
        }
        return images;
    }

    private String extractSingleImageUrl(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P18")) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (!fn.isEmpty()) return generateCommonsUrl(fn);
        }
        return null;
    }

    private String extractVideoUrl(JsonNode entity) {
        String fallback = null;
        for (JsonNode claim : entity.path("claims").path("P10")) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (fn.isEmpty()) continue;
            JsonNode q3831 = claim.path("qualifiers").path("P3831");
            if (q3831.isArray() && !q3831.isEmpty()) {
                if ("Q89347362".equals(q3831.get(0).path("datavalue").path("value").path("id").asText("")))
                    return generateCommonsUrl(fn);
            } else {
                if (fallback == null) fallback = generateCommonsUrl(fn);
            }
        }
        return fallback;
    }

    private String extractTrailerUrl(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P10")) {
            String fn = claim.path("mainsnak").path("datavalue").path("value").asText();
            if (fn.isEmpty()) continue;
            JsonNode q3831 = claim.path("qualifiers").path("P3831");
            if (q3831.isArray() && !q3831.isEmpty() &&
                    "Q622550".equals(q3831.get(0).path("datavalue").path("value").path("id").asText("")))
                return generateCommonsUrl(fn);
        }
        return null;
    }

    // ==================== WIKIDATA EXTRACTION ====================

    private List<String> extractGenres(JsonNode entity) {
        List<String> genres = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P136")) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) {
                label = label.replaceAll("(?i)\\s+films?$", "").trim();
                if (!label.isEmpty()) genres.add(Character.toUpperCase(label.charAt(0)) + label.substring(1));
            }
        }
        return genres;
    }

    private List<Person> extractPersonsWithDetails(JsonNode entity, String property) {
        List<Person> result = new ArrayList<>();
        Set<String> seenQids = new LinkedHashSet<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (qid.isEmpty() || !seenQids.add(qid)) continue;
            Person p = fetchPersonDetails(qid);
            if (p != null) result.add(p);
        }
        return result;
    }

    private List<Company> extractCompaniesWithDetails(JsonNode entity, String property) {
        List<Company> result = new ArrayList<>();
        Set<String> seenQids = new LinkedHashSet<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String qid = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (qid.isEmpty() || !seenQids.add(qid)) continue;
            Company c = fetchCompanyDetails(qid);
            if (c != null) result.add(c);
        }
        return result;
    }

    private String extractColor(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P462")) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if ("Q838368".equals(id)) return "black-and-white";
            if ("Q22006653".equals(id)) return "color";
            return fetchLabelById(id);
        }
        return null;
    }

    private String extractOriginalLanguage(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P364")) {
            if (!"value".equals(claim.path("mainsnak").path("snaktype").asText())) continue;
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!id.isEmpty()) return fetchLabelById(id);
        }
        return null;
    }

    private BudgetData extractBudget(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P2130")) {
            JsonNode v = claim.path("mainsnak").path("datavalue").path("value");
            String amount = v.path("amount").asText("").replace("+", "");
            if (amount.isEmpty()) continue;
            try {
                long cents = (long)(Double.parseDouble(amount) * 100);
                String currency = detectCurrency(v.path("unit").asText());
                BudgetData b = new BudgetData();
                b.setAmount(cents); b.setCurrency(currency);
                b.setDisplayValue(formatCurrency(cents, currency));
                return b;
            } catch (NumberFormatException e) { log.warn("Budget parse error: {}", amount); }
        }
        return null;
    }

    private String detectCurrency(String unitUrl) {
        if (unitUrl == null || unitUrl.isEmpty() || "1".equals(unitUrl)) return "USD";
        if (unitUrl.endsWith("Q4917"))   return "USD";
        if (unitUrl.endsWith("Q25224"))  return "GBP";
        if (unitUrl.endsWith("Q4916"))   return "EUR";
        if (unitUrl.endsWith("Q155009")) return "RM";
        if (unitUrl.endsWith("Q4306"))   return "JPY";
        if (unitUrl.endsWith("Q41291"))  return "IDR";
        String[] parts = unitUrl.split("/");
        return parts[parts.length - 1];
    }

    private List<BoxOfficeData> extractBoxOffice(JsonNode entity) {
        List<BoxOfficeData> results = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P2142")) {
            JsonNode v = claim.path("mainsnak").path("datavalue").path("value");
            String amount = v.path("amount").asText("").replace("+", "");
            if (amount.isEmpty()) continue;
            try {

                long cents = (long)(Double.parseDouble(amount) * 100);
                String currency = detectCurrency(v.path("unit").asText());
                BoxOfficeData d = new BoxOfficeData();
                d.setAmount(cents); d.setCurrency(currency);
                d.setDisplayValue(formatCurrency(cents, currency));
                JsonNode q = claim.path("qualifiers").path("P3005");
                String region = "worldwide";
                if (q.isArray() && !q.isEmpty()) {
                    String regionId = q.get(0).path("datavalue").path("value").path("id").asText();
                    String regionLabel = fetchLabelById(regionId);
                    if (regionLabel != null && !regionLabel.isBlank()) {
                        region = regionLabel;
                    }
                }
                d.setRegion(region);
                results.add(d);
            } catch (NumberFormatException e) { log.warn("BoxOffice parse error: {}", amount); }
        }
        return results;
    }

    private List<ReviewScore> extractReviewScores(JsonNode entity) {
        List<ReviewScore> scores = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path("P444")) {
            ReviewScore s = new ReviewScore();
            s.setValue(claim.path("mainsnak").path("datavalue").path("value").asText());
            JsonNode q = claim.path("qualifiers");
            JsonNode rev = q.path("P447");
            if (rev.isArray() && !rev.isEmpty())
                s.setSource(fetchLabelById(rev.get(0).path("datavalue").path("value").path("id").asText()));
            JsonNode meth = q.path("P459");
            if (meth.isArray() && !meth.isEmpty())
                s.setScoreType(fetchLabelById(meth.get(0).path("datavalue").path("value").path("id").asText()));
            JsonNode num = q.path("P3744");
            if (num.isArray() && !num.isEmpty()) {
                try { s.setNumReviews(Integer.parseInt(
                        num.get(0).path("datavalue").path("value").path("amount").asText("").replace("+",""))); }
                catch (NumberFormatException ignored) {}
            }
            JsonNode date = q.path("P585");
            if (date.isArray() && !date.isEmpty())
                s.setReviewDate(date.get(0).path("datavalue").path("value").path("time").asText("")
                        .replaceAll("\\+|T.*",""));
            if (s.getSource() != null) scores.add(s);
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
        ratings.addAll(extractRatingByProperty(entity, "P13773", "LSF"));
        return ratings;
    }

    private List<ContentRating> extractRatingByProperty(JsonNode entity, String property, String system) {
        List<ContentRating> ratings = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            ContentRating r = new ContentRating();
            r.setSystem(system);
            JsonNode dv = claim.path("mainsnak").path("datavalue");
            String type = dv.path("type").asText("");
            if ("wikibase-entityid".equals(type)) {
                String id = dv.path("value").path("id").asText();
                if (!id.isEmpty()) r.setValue(fetchLabelById(id));
            } else if ("string".equals(type)) {
                String val = dv.path("value").asText();
                if (!val.isEmpty()) r.setValue(val);
            }
            if (!isValid(r.getValue())) continue;
            JsonNode q = claim.path("qualifiers");
            List<String> descs = new ArrayList<>();
            for (JsonNode n : q.path("P2614")) {
                String id = n.path("datavalue").path("value").path("id").asText();
                if (!id.isEmpty()) descs.add(fetchLabelById(id));
            }
            if (!descs.isEmpty()) r.setContentDescriptors(String.join(", ", descs));
            JsonNode startDate = q.path("P580");
            if (startDate.isArray() && !startDate.isEmpty()) {
                String d = startDate.get(0).path("datavalue").path("value").path("time").asText("")
                        .replaceAll("\\+|T.*","");
                if (!d.isEmpty()) r.setStartDate(d);
            }
            JsonNode fmt = q.path("P437");
            if (fmt.isArray() && !fmt.isEmpty())
                r.setDistributionFormat(fetchLabelById(
                        fmt.get(0).path("datavalue").path("value").path("id").asText()));
            ratings.add(r);
        }
        return ratings;
    }

    private String extractFollowedBy(JsonNode entity) {
        for (JsonNode c : entity.path("claims").path("P156"))
            return fetchLabelById(c.path("mainsnak").path("datavalue").path("value").path("id").asText());
        return null;
    }

    private String extractPartOfSeries(JsonNode entity) {
        for (JsonNode c : entity.path("claims").path("P179"))
            return fetchLabelById(c.path("mainsnak").path("datavalue").path("value").path("id").asText());
        return null;
    }

    private List<String> extractAliases(JsonNode entity) {
        List<String> aliases = new ArrayList<>();
        for (JsonNode a : entity.path("aliases").path("id")) {
            String v = a.path("value").asText();
            if (!v.isEmpty()) aliases.add(v);
        }
        return aliases;
    }

    private String extractSubtitleUrl(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("/")) return null;
        try {
            String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            HttpEntity<String> ent = new HttpEntity<>(createHeaders());
            for (String lang : new String[]{"id", "en"}) {
                String surl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + "." + lang + ".srt";
                try {
                    ResponseEntity<String> r = restTemplate.exchange(surl, HttpMethod.GET, ent, String.class);
                    if (r.getStatusCode() == HttpStatus.OK && r.getBody() != null && r.getBody().contains("<pre"))
                        return surl;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { log.warn("Subtitle check error: {}", e.getMessage()); }
        return null;
    }

    // ==================== WIKIDATA FETCH HELPERS ====================

    private Person fetchPersonDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            JsonNode ent = mapper.readTree(restTemplate.exchange(url, HttpMethod.GET,
                            new HttpEntity<>(createHeaders()), String.class).getBody())
                    .path("entities").path(qid);
            String name = getLabel(ent);
            if (name == null || name.trim().isEmpty()) {
                log.warn("fetchPersonDetails: nama kosong untuk {}, di-skip", qid);
                return null;
            }
            Person p = new Person();
            p.setWikidataQid(qid); p.setName(name);
            p.setDescription(getDescriptionByEn(ent)); p.setPhotoUrl(extractSingleImageUrl(ent));
            return p;
        } catch (Exception e) {
            log.warn("fetchPersonDetails failed {}: {} — di-skip", qid, e.getMessage());
            return null;
        }
    }

    private Company fetchCompanyDetails(String qid) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
            JsonNode ent = mapper.readTree(restTemplate.exchange(url, HttpMethod.GET,
                            new HttpEntity<>(createHeaders()), String.class).getBody())
                    .path("entities").path(qid);
            String name = getLabel(ent);
            if (name == null || name.trim().isEmpty()) {
                log.warn("fetchCompanyDetails: nama kosong untuk {}, di-skip", qid);
                return null;
            }
            Company c = new Company();
            c.setWikidataQid(qid); c.setName(name);
            c.setDescription(getDescriptionByEn(ent)); c.setLogoUrl(extractSingleImageUrl(ent));
            return c;
        } catch (Exception e) {
            log.warn("fetchCompanyDetails failed {}: {} — di-skip", qid, e.getMessage());
            return null;
        }
    }

    private String fetchLabelById(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json";
            return getLabel(mapper.readTree(restTemplate.exchange(url, HttpMethod.GET,
                            new HttpEntity<>(createHeaders()), String.class).getBody())
                    .path("entities").path(id));
        } catch (Exception e) { log.warn("fetchLabelById failed: {}", id); return null; }
    }

    // ==================== LABEL & DESCRIPTION HELPERS ====================

    /**
     * Ambil label terbaik dari entity Wikidata.
     *
     * Urutan prioritas:
     *  1. "en"  — bahasa Inggris
     *  2. "mul" — multilingual; Wikidata menggunakannya untuk judul resmi EN
     *             Contoh Q3222924: tidak ada "en", tapi mul = "The Jazz Fool"
     *  3. "id"  — Bahasa Indonesia
     *  4. Bahasa Latin lainnya (it, es, de, fr, nl, dst)
     *  5. Label apapun yang tersedia (fallback terakhir)
     */
    private String getLabel(JsonNode entity) {
        JsonNode labels = entity.path("labels");

        // 1. EN
        if (labels.has("en"))  return labels.path("en").path("value").asText();
        // 2. MUL (multilingual — sering berisi judul EN resmi)
        if (labels.has("mul")) return labels.path("mul").path("value").asText();
        // 3. ID (Indonesia)
        if (labels.has("id"))  return labels.path("id").path("value").asText();
        // 4. Bahasa Latin lainnya
        String[] latinLangs = {"it", "es", "de", "fr", "nl", "pt", "sv", "pl", "ro", "cs", "hu", "fi", "da", "no"};
        for (String lang : latinLangs) {
            if (labels.has(lang)) return labels.path(lang).path("value").asText();
        }
        // 5. Fallback: label pertama apapun
        if (labels.elements().hasNext()) return labels.elements().next().path("value").asText();

        return null;
    }

    private String getLabelByLang(JsonNode entity, String lang) {
        JsonNode label = entity.path("labels").path(lang);
        return label.has("value") ? label.path("value").asText() : null;
    }

    private String getDescriptionById(JsonNode entity) {
        JsonNode d = entity.path("descriptions").path("id");
        return d.has("value") ? d.path("value").asText() : null;
    }

    private String getDescriptionByEn(JsonNode entity) {
        JsonNode d = entity.path("descriptions").path("en");
        return d.has("value") ? d.path("value").asText() : null;
    }

    private String extractDate(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P577")) {
            String t = claim.path("mainsnak").path("datavalue").path("value").path("time").asText("");
            if (!t.isEmpty()) return t.replaceAll("\\+|T.*", "");
        }
        return null;
    }

    private String extractEntityLabel(JsonNode entity, String property) {
        for (JsonNode claim : entity.path("claims").path(property)) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            if (!id.isEmpty()) return fetchLabelById(id);
        }
        return null;
    }

    private List<String> extractEntityLabels(JsonNode entity, String property) {
        List<String> result = new ArrayList<>();
        for (JsonNode claim : entity.path("claims").path(property)) {
            String id = claim.path("mainsnak").path("datavalue").path("value").path("id").asText();
            String label = fetchLabelById(id);
            if (label != null) result.add(label);
        }
        return result;
    }

    private String extractDuration(JsonNode entity) {
        for (JsonNode claim : entity.path("claims").path("P2047")) {
            String amount = claim.path("mainsnak").path("datavalue").path("value").path("amount").asText("");
            if (!amount.isEmpty()) return amount.replace("+", "") + " menit";
        }
        return null;
    }

    // ==================== UTILITY ====================

    private boolean isValid(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String truncateAtSentence(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) return text;
        int cut = text.lastIndexOf(". ", maxChars);
        return cut > 0 ? text.substring(0, cut + 1) : text.substring(0, maxChars);
    }

    private String generateCommonsUrl(String filename) {
        try {
            String fn = filename.replace(" ", "_");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(fn.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            String md5 = sb.toString();
            return "https://upload.wikimedia.org/wikipedia/commons/" +
                    md5.charAt(0) + "/" + md5.substring(0, 2) + "/" + fn;
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
        return (url.contains("action=raw") || url.endsWith(".srt") || url.endsWith(".vtt")) ? url : null;
    }

    private String convertSrtToVtt(String srt) {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        String converted = srt.replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2");
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
        if ("GBP".equals(currency)) return NumberFormat.getCurrencyInstance(Locale.UK).format(amount);
        return currency + " " + String.format("%,.2f", amount);
    }
}