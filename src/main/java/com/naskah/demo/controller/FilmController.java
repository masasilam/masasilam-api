package com.naskah.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/films")
public class FilmController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/wikidata")
    public ResponseEntity<Map<String, Object>> getFilmFromWikidata(@RequestParam String qid) {
        String url = "https://www.wikidata.org/wiki/Special:EntityData/" + qid + ".json";
        String response = restTemplate.getForObject(url, String.class);

        try {
            JsonNode root = mapper.readTree(response);
            JsonNode entity = root.path("entities").path(qid);

            String title = getLabel(entity, "en");
            String description = getDescription(entity, "en");
            String releaseDate = extractDate(entity, "P577");
            String instanceOf = extractEntityLabelWithFallback(entity, "P31");
            List<String> genres = extractEntityLabelsWithFallback(entity, "P136");
            List<String> directors = extractEntityLabelsWithFallback(entity, "P57");
            List<String> writers = extractEntityLabelsWithFallback(entity, "P58");
            List<String> actors = extractEntityLabelsWithFallback(entity, "P161");
            List<String> producers = extractEntityLabelsWithFallback(entity, "P162");
            List<String> studios = extractEntityLabelsWithFallback(entity, "P272");
            String country = extractEntityLabelWithFallback(entity, "P495");
            String duration = extractDuration(entity);
            String posterUrl = extractImageUrl(entity, "P18");
            String videoUrl = extractVideoUrl(entity);
            List<String> aliasesId = extractAliases(entity, "id");
            String subtitleUrl = extractSubtitleUrl(videoUrl);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("judul", title != null ? title : "");
            result.put("tahunRilis", releaseDate != null ? releaseDate : "");
            result.put("jenis", instanceOf != null ? instanceOf : "");
            result.put("deskripsi", description != null ? description : "");
            result.put("genre", genres);
            result.put("sutradara", directors);
            result.put("penulisSkenario", writers);
            result.put("pemeran", actors);
            result.put("produser", producers);
            result.put("perusahaanProduksi", studios);
            result.put("durasi", duration != null ? duration : "");
            result.put("negaraAsal", country != null ? country : "");
            result.put("aliasIndonesia", aliasesId);
            result.put("poster", posterUrl);
            result.put("video", videoUrl);
            result.put("subtitle", subtitleUrl);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Gagal memproses data Wikidata", "details", e.getMessage()));
        }
    }

    private String getLabel(JsonNode entity, String lang) {
        JsonNode labels = entity.path("labels").path(lang);
        return labels.has("value") ? labels.path("value").asText() : null;
    }

    private String getDescription(JsonNode entity, String lang) {
        JsonNode descriptions = entity.path("descriptions").path(lang);
        return descriptions.has("value") ? descriptions.path("value").asText() : null;
    }

    private String extractDate(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && claims.size() > 0) {
            JsonNode timeValue = claims.get(0).path("mainsnak").path("datavalue").path("value").path("time");
            if (!timeValue.isMissingNode()) {
                return timeValue.asText().replaceAll("\\+|T.*", "");
            }
        }
        return null;
    }

    private String extractEntityLabelWithFallback(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && claims.size() > 0) {
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
            if (label != null) {
                result.add(label);
            }
        }
        return result;
    }

    private String fetchLabelById(String id) {
        try {
            String url = "https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json";
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode target = root.path("entities").path(id);
            return getLabel(target, "en");
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDuration(JsonNode entity) {
        JsonNode claims = entity.path("claims").path("P2047");
        if (claims.isArray() && claims.size() > 0) {
            JsonNode amount = claims.get(0).path("mainsnak").path("datavalue").path("value").path("amount");
            JsonNode unit = claims.get(0).path("mainsnak").path("datavalue").path("value").path("unit");
            if (!amount.isMissingNode()) {
                String durationValue = amount.asText().replace("+", "");
                return durationValue + " menit";
            }
        }
        return null;
    }

    private String extractImageUrl(JsonNode entity, String property) {
        JsonNode claims = entity.path("claims").path(property);
        if (claims.isArray() && claims.size() > 0) {
            String filename = claims.get(0).path("mainsnak").path("datavalue").path("value").asText();
            return generateCommonsUrl(filename);
        }
        return null;
    }

    private String extractVideoUrl(JsonNode entity) {
        JsonNode videoClaims = entity.path("claims").path("P10");
        if (videoClaims.isArray()) {
            for (JsonNode claim : videoClaims) {
                String roleId = claim.path("qualifiers")
                        .path("P3831").get(0)
                        .path("datavalue").path("value").path("id").asText("");

                if ("Q89347362".equals(roleId)) { // full video available on Wikimedia Commons
                    String filename = claim.path("mainsnak").path("datavalue").path("value").asText();
                    return generateCommonsUrl(filename);
                }
            }
        }

        // Fallback ke kategori Commons jika tidak ada P10
        JsonNode commonsCat = entity.path("claims").path("P373");
        if (commonsCat.isArray() && commonsCat.size() > 0) {
            String category = commonsCat.get(0).path("mainsnak").path("datavalue").path("value").asText();
            return "https://commons.wikimedia.org/wiki/Category:" + category.replace(" ", "_");
        }

        return null;
    }

    private List<String> extractAliases(JsonNode entity, String lang) {
        List<String> aliases = new ArrayList<>();
        JsonNode aliasesNode = entity.path("aliases").path(lang);
        for (JsonNode aliasNode : aliasesNode) {
            String alias = aliasNode.path("value").asText();
            if (!alias.isEmpty()) {
                aliases.add(alias);
            }
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
            return "https://upload.wikimedia.org/wikipedia/commons/" +
                    md5.charAt(0) + "/" + md5.substring(0, 2) + "/" + encodedFile;
        } catch (Exception e) {
            return "https://commons.wikimedia.org/wiki/File:" + filename.replace(" ", "_");
        }
    }

    private String extractSubtitleUrl(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("/")) return null;

        try {
            // Ambil nama file dari URL video
            String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
            String subtitleIdUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".id.srt";
            String subtitleEnUrl = "https://commons.wikimedia.org/wiki/TimedText:" + fileName + ".en.srt";

            // Coba cek subtitle Indonesia dulu
            ResponseEntity<String> responseId = restTemplate.getForEntity(subtitleIdUrl, String.class);
            if (responseId.getStatusCode() == HttpStatus.OK && responseId.getBody() != null && responseId.getBody().contains("<pre")) {
                return subtitleIdUrl;
            }

            // Kalau tidak ada, cek subtitle Inggris
            ResponseEntity<String> responseEn = restTemplate.getForEntity(subtitleEnUrl, String.class);
            if (responseEn.getStatusCode() == HttpStatus.OK && responseEn.getBody() != null && responseEn.getBody().contains("<pre")) {
                return subtitleEnUrl;
            }

        } catch (Exception e) {
            // Lewatkan kalau tidak ada atau error
        }

        return null; // Tidak ada subtitle
    }

}
