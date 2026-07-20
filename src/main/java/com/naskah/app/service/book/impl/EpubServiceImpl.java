package com.naskah.app.service.book.impl;

import com.naskah.app.config.ClearChapterCache;
import com.naskah.app.exception.custom.DataNotFoundException;
import com.naskah.app.model.dto.ChapterHierarchy;
import com.naskah.app.model.dto.EpubProcessResult;
import com.naskah.app.model.entity.Book;
import com.naskah.app.model.entity.BookChapter;
import com.naskah.app.repository.ChapterRepository;
import com.naskah.app.service.book.EpubService;
import com.naskah.app.util.file.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubServiceImpl implements EpubService {

    // ⚠️  BookChapterMapper DIHAPUS dari sini.
    // Gunakan ChapterRepository yang di-inject dari caller (Book atau Zine).
    private final FileUtil fileUtil;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    @Override
    @ClearChapterCache
    public EpubProcessResult processEpubFile(
            MultipartFile epubFile,
            Book book,
            ChapterRepository chapterRepository) throws IOException {

        log.info("Processing EPUB for book: {} (ID: {})", book.getTitle(), book.getId());

        EpubProcessResult result = new EpubProcessResult();

        try (InputStream is = epubFile.getInputStream()) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(is);

            Map<String, ChapterHierarchy> tocStructure = parseTocStructure(epubBook);

            List<BookChapter> chapters = extractAndSaveChaptersWithHierarchy(
                    epubBook, book.getId(), tocStructure, new HashMap<>(), chapterRepository
            );

            result.setChapters(chapters);
            result.setTotalChapters(chapters.size());

            long totalWords = chapters.stream().mapToLong(BookChapter::getWordCount).sum();
            result.setTotalWords(totalWords);

            Resource coverResource = epubBook.getCoverImage();
            if (coverResource != null) {
                String coverUrl = extractAndUploadCover(coverResource, book.getId(), book.getTitle());
                result.setCoverImageUrl(coverUrl);
            }

            if (!chapters.isEmpty()) {
                String preview = fileUtil.generatePreviewText(chapters.getFirst().getContent(), 500);
                result.setPreviewText(preview);
            }

            log.info("EPUB processing completed: {} chapters, {} words", chapters.size(), totalWords);
            return result;

        } catch (Exception e) {
            log.error("Failed to process EPUB: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @ClearChapterCache
    public EpubProcessResult processEpubFileForUpdate(
            MultipartFile epubFile,
            Book book,
            ChapterRepository chapterRepository) throws IOException {

        log.info("Processing EPUB for UPDATE - book: {} (ID: {})", book.getTitle(), book.getId());

        EpubProcessResult result = new EpubProcessResult();

        try (InputStream is = epubFile.getInputStream()) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(is);

            Map<String, ChapterHierarchy> tocStructure = parseTocStructure(epubBook);

            List<BookChapter> existingChapters = chapterRepository.findChaptersByEntityId(book.getId());
            Map<Integer, BookChapter> existingChapterMap = new HashMap<>();
            for (BookChapter chapter : existingChapters) {
                existingChapterMap.put(chapter.getChapterNumber(), chapter);
            }

            Map<String, String> imageCache = buildExistingImageCache(existingChapters);

            List<BookChapter> processedChapters = updateOrInsertChaptersWithHierarchy(
                    epubBook, book.getId(), tocStructure, existingChapterMap, imageCache, chapterRepository
            );

            result.setChapters(processedChapters);
            result.setTotalChapters(processedChapters.size());

            Set<Integer> processedChapterNumbers = processedChapters.stream()
                    .map(BookChapter::getChapterNumber)
                    .collect(Collectors.toSet());

            for (BookChapter existingChapter : existingChapters) {
                if (!processedChapterNumbers.contains(existingChapter.getChapterNumber())) {
                    chapterRepository.deleteChapterById(existingChapter.getId());
                    log.info("Deleted obsolete chapter: {} (ID: {})",
                            existingChapter.getTitle(), existingChapter.getId());
                }
            }

            long totalWords = processedChapters.stream().mapToLong(BookChapter::getWordCount).sum();
            result.setTotalWords(totalWords);

            Resource coverResource = epubBook.getCoverImage();
            if (coverResource != null) {
                String coverUrl = extractAndUploadCover(coverResource, book.getId(), book.getTitle());
                result.setCoverImageUrl(coverUrl);
            }

            if (!processedChapters.isEmpty()) {
                String preview = fileUtil.generatePreviewText(processedChapters.getFirst().getContent(), 500);
                result.setPreviewText(preview);
            }

            log.info("EPUB update completed: {} chapters, {} words", processedChapters.size(), totalWords);
            return result;

        } catch (Exception e) {
            log.error("Failed to update EPUB: {}", e.getMessage(), e);
            throw e;
        }
    }

    // =========================================================================
    // QUERY METHODS (still need a repository — caller passes the right one)
    // =========================================================================

    @Override
    public BookChapter getChapter(Long entityId, Integer chapterNumber, ChapterRepository chapterRepository) {
        BookChapter chapter = chapterRepository.findChapterByNumber(entityId, chapterNumber);
        if (chapter == null) {
            throw new DataNotFoundException();
        }
        return chapter;
    }

    @Override
    public List<BookChapter> getAllChapters(Long entityId, ChapterRepository chapterRepository) {
        return chapterRepository.findChaptersByEntityId(entityId);
    }

    @Override
    public List<BookChapter> searchInEntity(Long entityId, String query, ChapterRepository chapterRepository) {
        return chapterRepository.searchInEntity(entityId, query);
    }

    @Override
    public void deleteChaptersByEntityId(Long entityId, ChapterRepository chapterRepository) {
        chapterRepository.deleteChaptersByEntityId(entityId);
        log.info("Deleted all chapters for entity ID: {}", entityId);
    }

    // =========================================================================
    // PRIVATE — EXTRACT & SAVE
    // =========================================================================

    private List<BookChapter> extractAndSaveChaptersWithHierarchy(
            nl.siegmann.epublib.domain.Book epubBook,
            Long entityId,
            Map<String, ChapterHierarchy> tocStructure,
            Map<String, String> imageCache,
            ChapterRepository chapterRepository) {

        List<BookChapter> chapters = new ArrayList<>();
        Map<String, Long> hrefToChapterId = new HashMap<>();

        List<ChapterHierarchy> orderedToc = getOrderedTocEntries(epubBook);
        log.info("Processing {} TOC entries in correct order", orderedToc.size());

        int chapterNumber = 0;

        for (int i = 0; i < orderedToc.size(); i++) {
            ChapterHierarchy hierarchy = orderedToc.get(i);

            try {
                chapterNumber++;

                String fullHref  = hierarchy.getHref();
                String fileName  = fullHref.split("#")[0];
                String anchorId  = fullHref.contains("#") ? fullHref.split("#")[1] : null;

                Resource resource = findResource(epubBook, fileName);
                if (resource == null) {
                    log.warn("Resource not found for: {}", fileName);
                    continue;
                }

                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(htmlContent);

                processImages(epubBook, doc, entityId, imageCache);

                String[] extracted = extractContent(doc, anchorId, i, orderedToc, fileName);
                String content         = extracted[0];
                String htmlContentStr  = extracted[1];

                int wordCount = fileUtil.countWords(content);

                Long parentChapterId = null;
                if (hierarchy.getParentHref() != null && !hierarchy.getParentHref().isEmpty()) {
                    parentChapterId = hrefToChapterId.get(hierarchy.getParentHref());
                    if (parentChapterId == null) {
                        log.warn("Parent chapter not found for href: {}", fullHref);
                    }
                }

                BookChapter chapter = new BookChapter();
                chapter.setBookId(entityId);            // bookId = entityId (zine_id or book_id)
                chapter.setChapterNumber(chapterNumber);
                chapter.setTitle(fileUtil.toTitleCase(hierarchy.getTitle()));
                chapter.setSlug(fileUtil.sanitizeFilename(hierarchy.getTitle()));
                chapter.setContent(content);
                chapter.setHtmlContent(htmlContentStr);
                chapter.setWordCount(wordCount);
                chapter.setParentChapterId(parentChapterId);
                chapter.setChapterLevel(hierarchy.getLevel());
                chapter.setCreatedAt(LocalDateTime.now());
                chapter.setUpdatedAt(LocalDateTime.now());

                chapterRepository.insertChapter(chapter);
                chapters.add(chapter);
                hrefToChapterId.put(fullHref, chapter.getId());

                log.info("Saved chapter {} (Level {}): {} [Parent: {}]",
                        chapter.getChapterNumber(), chapter.getChapterLevel(),
                        hierarchy.getTitle(),
                        parentChapterId != null ? "#" + parentChapterId : "Root");

            } catch (Exception e) {
                log.error("Failed to extract chapter '{}': {}", hierarchy.getTitle(), e.getMessage(), e);
            }
        }

        log.info("Successfully extracted and saved {} chapters", chapters.size());
        return chapters;
    }

    private List<BookChapter> updateOrInsertChaptersWithHierarchy(
            nl.siegmann.epublib.domain.Book epubBook,
            Long entityId,
            Map<String, ChapterHierarchy> tocStructure,
            Map<Integer, BookChapter> existingChapterMap,
            Map<String, String> imageCache,
            ChapterRepository chapterRepository) {

        List<BookChapter> chapters = new ArrayList<>();
        Map<String, Long> hrefToChapterId = new HashMap<>();

        List<ChapterHierarchy> orderedToc = getOrderedTocEntries(epubBook);
        log.info("Processing {} TOC entries in correct order", orderedToc.size());

        int chapterNumber = 0;

        for (int i = 0; i < orderedToc.size(); i++) {
            ChapterHierarchy hierarchy = orderedToc.get(i);

            try {
                chapterNumber++;

                String fullHref = hierarchy.getHref();
                String fileName = fullHref.split("#")[0];
                String anchorId = fullHref.contains("#") ? fullHref.split("#")[1] : null;

                Resource resource = findResource(epubBook, fileName);
                if (resource == null) {
                    log.warn("Resource not found for: {}", fileName);
                    continue;
                }

                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(htmlContent);

                processImages(epubBook, doc, entityId, imageCache);

                String[] extracted = extractContent(doc, anchorId, i, orderedToc, fileName);
                String content        = extracted[0];
                String htmlContentStr = extracted[1];

                int wordCount = fileUtil.countWords(content);

                Long parentChapterId = null;
                if (hierarchy.getParentHref() != null && !hierarchy.getParentHref().isEmpty()) {
                    parentChapterId = hrefToChapterId.get(hierarchy.getParentHref());
                }

                BookChapter chapter = existingChapterMap.get(chapterNumber);

                if (chapter != null) {
                    chapter.setTitle(fileUtil.toTitleCase(hierarchy.getTitle()));
                    chapter.setSlug(fileUtil.sanitizeFilename(hierarchy.getTitle()));
                    chapter.setContent(content);
                    chapter.setHtmlContent(htmlContentStr);
                    chapter.setWordCount(wordCount);
                    chapter.setParentChapterId(parentChapterId);
                    chapter.setChapterLevel(hierarchy.getLevel());
                    chapter.setUpdatedAt(LocalDateTime.now());

                    chapterRepository.updateChapter(chapter);
                    log.info("✏️ Updated chapter {} (ID: {}): {}", chapter.getChapterNumber(), chapter.getId(), hierarchy.getTitle());
                } else {
                    chapter = new BookChapter();
                    chapter.setBookId(entityId);
                    chapter.setChapterNumber(chapterNumber);
                    chapter.setTitle(fileUtil.toTitleCase(hierarchy.getTitle()));
                    chapter.setSlug(fileUtil.sanitizeFilename(hierarchy.getTitle()));
                    chapter.setContent(content);
                    chapter.setHtmlContent(htmlContentStr);
                    chapter.setWordCount(wordCount);
                    chapter.setParentChapterId(parentChapterId);
                    chapter.setChapterLevel(hierarchy.getLevel());
                    chapter.setCreatedAt(LocalDateTime.now());
                    chapter.setUpdatedAt(LocalDateTime.now());

                    chapterRepository.insertChapter(chapter);
                    log.info("➕ Inserted new chapter {} (ID: {}): {}", chapter.getChapterNumber(), chapter.getId(), hierarchy.getTitle());
                }

                chapters.add(chapter);
                hrefToChapterId.put(fullHref, chapter.getId());

            } catch (Exception e) {
                log.error("Failed to process chapter '{}': {}", hierarchy.getTitle(), e.getMessage(), e);
            }
        }

        log.info("Successfully processed {} chapters (updated/inserted)", chapters.size());
        return chapters;
    }

    // =========================================================================
    // PRIVATE — HELPERS
    // =========================================================================

    /**
     * Upload semua <img> di document, ganti src dengan Cloudinary URL.
     */
    private void processImages(
            nl.siegmann.epublib.domain.Book epubBook,
            Document doc,
            Long entityId,
            Map<String, String> imageCache) {

        Elements imgElements = doc.select("img");
        for (Element img : imgElements) {
            String imgSrc = img.attr("src");
            if (!imgSrc.isEmpty()) {
                try {
                    String cloudinaryUrl = extractAndUploadChapterImageOptimized(
                            epubBook, imgSrc, entityId, imageCache);
                    if (cloudinaryUrl != null) {
                        img.attr("src", cloudinaryUrl);
                    }
                } catch (Exception e) {
                    log.warn("Failed to upload image {}: {}", imgSrc, e.getMessage());
                }
            }
        }
    }

    /**
     * Extract text content + html content dari document berdasarkan anchor / struktur TOC.
     * Returns String[2]: [0] = plain content, [1] = htmlContent
     */
    private String[] extractContent(
            Document doc,
            String anchorId,
            int i,
            List<ChapterHierarchy> orderedToc,
            String fileName) {

        if (anchorId != null) {
            return extractAnchoredContent(doc, anchorId, i, orderedToc, fileName);
        } else {
            return extractRootContent(doc, i, orderedToc, fileName);
        }
    }

    private String[] extractAnchoredContent(
            Document doc,
            String anchorId,
            int i,
            List<ChapterHierarchy> orderedToc,
            String fileName) {

        String nextAnchorInSameFile = null;
        for (int j = i + 1; j < orderedToc.size(); j++) {
            ChapterHierarchy next = orderedToc.get(j);
            if (next.getHref().startsWith(fileName + "#")) {
                nextAnchorInSameFile = next.getHref().split("#")[1];
                break;
            } else if (!next.getHref().startsWith(fileName)) {
                break;
            }
        }

        Element startElement = doc.getElementById(anchorId);
        if (startElement == null) {
            log.warn("Anchor not found: {}", anchorId);
            return new String[]{doc.body().text().trim(), doc.body().html()};
        }

        StringBuilder sb     = new StringBuilder();
        StringBuilder htmlSb = new StringBuilder();
        Element current      = startElement;
        boolean started      = false;

        while (current != null) {
            if (!started && current.id().equals(anchorId)) started = true;

            if (started) {
                if (!current.id().isEmpty()
                        && !current.id().equals(anchorId)
                        && current.id().equals(nextAnchorInSameFile)) {
                    break;
                }
                sb.append(current.text()).append("\n");
                htmlSb.append(current.outerHtml());
            }
            current = current.nextElementSibling();
        }

        return new String[]{sb.toString().trim(), htmlSb.toString()};
    }

    private String[] extractRootContent(
            Document doc,
            int i,
            List<ChapterHierarchy> orderedToc,
            String fileName) {

        String firstSubAnchor = null;
        for (int j = i + 1; j < orderedToc.size(); j++) {
            ChapterHierarchy next = orderedToc.get(j);
            if (next.getHref().startsWith(fileName + "#")) {
                firstSubAnchor = next.getHref().split("#")[1];
                break;
            } else if (!next.getHref().startsWith(fileName)) {
                break;
            }
        }

        if (firstSubAnchor == null) {
            return new String[]{doc.body().text().trim(), doc.body().html()};
        }

        Element stopElement = doc.getElementById(firstSubAnchor);
        if (stopElement == null) {
            log.warn("Sub-chapter anchor element not found: {}", firstSubAnchor);
            return new String[]{doc.body().text().trim(), doc.body().html()};
        }

        Element sectionElement = doc.select("section.chapter").first();
        if (sectionElement == null) {
            return new String[]{doc.body().text().trim(), doc.body().html()};
        }

        StringBuilder sb     = new StringBuilder();
        StringBuilder htmlSb = new StringBuilder();
        htmlSb.append("<section class=\"chapter\" epub:type=\"chapter\">\n");

        for (Element child : sectionElement.children()) {
            if (child.id().equals(firstSubAnchor) || child.getElementById(firstSubAnchor) != null) break;
            sb.append(child.text()).append("\n");
            htmlSb.append(child.outerHtml()).append("\n");
        }

        htmlSb.append("</section>");
        return new String[]{sb.toString().trim(), htmlSb.toString()};
    }

    private Map<String, String> buildExistingImageCache(List<BookChapter> existingChapters) {
        Map<String, String> imageCache = new HashMap<>();
        for (BookChapter chapter : existingChapters) {
            if (chapter.getHtmlContent() == null) continue;
            try {
                Document doc    = Jsoup.parse(chapter.getHtmlContent());
                Elements images = doc.select("img");
                for (Element img : images) {
                    String src = img.attr("src");
                    String alt = img.attr("alt");
                    if (src.contains("cloudinary.com")) {
                        String key = extractImageKey(src, alt);
                        if (key != null) imageCache.put(key, src);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse chapter HTML for image cache: {}", e.getMessage());
            }
        }
        log.info("Built image cache with {} entries", imageCache.size());
        return imageCache;
    }

    private String extractImageKey(String cloudinaryUrl, String alt) {
        Pattern pattern = Pattern.compile("/chapters/([^/]+?)(?:\\.[^.]+)?$");
        Matcher matcher = pattern.matcher(cloudinaryUrl);
        if (matcher.find()) return matcher.group(1);
        if (alt != null && !alt.isEmpty()) return fileUtil.sanitizeFilename(alt);
        return null;
    }

    private String extractAndUploadChapterImageOptimized(
            nl.siegmann.epublib.domain.Book epubBook,
            String imagePath,
            Long entityId,
            Map<String, String> imageCache) {

        try {
            String normalizedPath = imagePath.replace("../", "");
            Resource imageResource = epubBook.getResources().getByHref(normalizedPath);

            if (imageResource == null) {
                String[] alternatives = {
                        "Images/" + imagePath,
                        imagePath.replace("Images/", ""),
                        "images/" + imagePath.toLowerCase()
                };
                for (String alt : alternatives) {
                    imageResource = epubBook.getResources().getByHref(alt);
                    if (imageResource != null) break;
                }
            }

            if (imageResource == null) {
                log.warn("Image not found in EPUB: {}", imagePath);
                return null;
            }

            byte[] imageData = imageResource.getData();
            String fileName  = normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1);
            String imageKey  = fileUtil.sanitizeFilename(fileName.replaceAll("\\.[^.]+$", ""));

            String cachedUrl = imageCache.get(imageKey);
            if (cachedUrl != null) {
                log.info("♻️ Reusing existing image: {} -> {}", imagePath, cachedUrl);
                return cachedUrl;
            }

            String cloudinaryUrl = fileUtil.uploadChapterImageFromBytes(imageData, entityId, fileName);
            imageCache.put(imageKey, cloudinaryUrl);
            log.info("📤 Uploaded new chapter image: {} -> {}", imagePath, cloudinaryUrl);
            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("Failed to process chapter image {}: {}", imagePath, e.getMessage());
            return null;
        }
    }

    private List<ChapterHierarchy> getOrderedTocEntries(nl.siegmann.epublib.domain.Book epubBook) {
        List<ChapterHierarchy> ordered = new ArrayList<>();
        try {
            for (SpineReference ref : epubBook.getSpine().getSpineReferences()) {
                Resource resource  = ref.getResource();
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc       = Jsoup.parse(htmlContent);

                Element nav = doc.select("nav[epub:type='toc']").first();
                if (nav == null) nav = doc.select("#toc").first();

                if (nav != null) {
                    log.info("Found TOC, parsing in order...");
                    parseOrderedToc(nav.select("> ol").first(), ordered, 1, null);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse ordered TOC: {}", e.getMessage());
        }
        return ordered;
    }

    private void parseOrderedToc(Element ol, List<ChapterHierarchy> ordered, int level, String parentHref) {
        if (ol == null) return;
        for (Element li : ol.select("> li")) {
            Element link = li.select("> a").first();
            if (link == null) continue;

            String href  = link.attr("href");
            String title = link.text();

            ordered.add(new ChapterHierarchy(href, title, level, parentHref));

            Element nestedOl = li.select("> ol").first();
            if (nestedOl != null) parseOrderedToc(nestedOl, ordered, level + 1, href);
        }
    }

    private Resource findResource(nl.siegmann.epublib.domain.Book epubBook, String fileName) {
        Resource res = epubBook.getResources().getByHref(fileName);
        if (res != null) return res;

        res = epubBook.getResources().getByHref("Text/" + fileName);
        if (res != null) return res;

        res = epubBook.getResources().getByHref("OEBPS/Text/" + fileName);
        if (res != null) return res;

        for (Resource r : epubBook.getResources().getAll()) {
            if (r.getHref().endsWith(fileName)) return r;
        }
        return null;
    }

    private Map<String, ChapterHierarchy> parseTocStructure(nl.siegmann.epublib.domain.Book epubBook) {
        Map<String, ChapterHierarchy> structure = new HashMap<>();
        try {
            for (SpineReference ref : epubBook.getSpine().getSpineReferences()) {
                Resource resource  = ref.getResource();
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc       = Jsoup.parse(htmlContent);

                Element nav = doc.select("nav[epub:type='toc']").first();
                if (nav == null) nav = doc.select("#toc").first();

                if (nav != null) {
                    log.info("Found TOC in: {}", resource.getHref());
                    parseTocRecursive(nav.select("> ol").first(), structure, 1, null);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse TOC structure: {}", e.getMessage());
        }
        return structure;
    }

    private void parseTocRecursive(Element ol, Map<String, ChapterHierarchy> structure, int level, String parentHref) {
        if (ol == null) return;
        for (Element li : ol.select("> li")) {
            Element link = li.select("> a").first();
            if (link == null) continue;

            String href  = link.attr("href");
            String title = link.text();
            structure.put(href, new ChapterHierarchy(href, title, level, parentHref));

            Element nestedOl = li.select("> ol").first();
            if (nestedOl != null) parseTocRecursive(nestedOl, structure, level + 1, href);
        }
    }

    private String extractAndUploadCover(Resource coverResource, Long entityId, String title) {
        try {
            byte[] imageData = coverResource.getData();
            String coverUrl  = fileUtil.uploadBookCoverFromBytes(imageData, title, entityId);
            log.info("Uploaded EPUB cover image: {}", coverUrl);
            return coverUrl;
        } catch (Exception e) {
            log.error("Failed to upload EPUB cover image: {}", e.getMessage());
            return null;
        }
    }
}