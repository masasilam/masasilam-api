package com.naskah.demo.service.book.impl;

import com.naskah.demo.exception.custom.DataNotFoundException;
import com.naskah.demo.model.dto.ChapterHierarchy;
import com.naskah.demo.model.dto.EpubProcessResult;
import com.naskah.demo.model.entity.Book;
import com.naskah.demo.model.entity.BookChapter;
import com.naskah.demo.mapper.BookChapterMapper;
import com.naskah.demo.service.book.EpubService;
import com.naskah.demo.util.file.FileUtil;
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
    private final BookChapterMapper chapterMapper;
    private final FileUtil fileUtil;

    @Override
    public EpubProcessResult processEpubFile(MultipartFile epubFile, Book book) throws IOException {
        log.info("Processing EPUB for book: {} (ID: {})", book.getTitle(), book.getId());

        EpubProcessResult result = new EpubProcessResult();

        try (InputStream is = epubFile.getInputStream()) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(is);

            // 1. Parse TOC structure
            Map<String, ChapterHierarchy> tocStructure = parseTocStructure(epubBook);

            // 2. Extract chapters with hierarchy (no cache needed for new book)
            List<BookChapter> chapters = extractAndSaveChaptersWithHierarchy(
                    epubBook, book.getId(), tocStructure, new HashMap<>()
            );

            result.setChapters(chapters);
            result.setTotalChapters(chapters.size());

            // 3. Calculate total words
            long totalWords = chapters.stream().mapToLong(BookChapter::getWordCount).sum();
            result.setTotalWords(totalWords);

            // 4. Extract cover
            Resource coverResource = epubBook.getCoverImage();
            if (coverResource != null) {
                String coverUrl = extractAndUploadCover(coverResource, book.getId(), book.getTitle());
                result.setCoverImageUrl(coverUrl);
            }

            // 5. Generate preview
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

    /**
     * ✅ UNIFIED METHOD: Extract chapters with optional image caching
     */
    private List<BookChapter> extractAndSaveChaptersWithHierarchy(nl.siegmann.epublib.domain.Book epubBook, Long bookId, Map<String, ChapterHierarchy> tocStructure, Map<String, String> imageCache) {

        List<BookChapter> chapters = new ArrayList<>();
        Map<String, Long> hrefToChapterId = new HashMap<>();

        log.info("TOC contains {} entries", tocStructure.size());

        // Process TOC in ORDER
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

                // Find the resource
                Resource resource = findResource(epubBook, fileName);
                if (resource == null) {
                    log.warn("Resource not found for: {}", fileName);
                    continue;
                }

                // Parse HTML
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(htmlContent);

                // ✅ OPTIMIZED: Process images with caching
                Elements imgElements = doc.select("img");
                for (Element img : imgElements) {
                    String imgSrc = img.attr("src");
                    if (!imgSrc.isEmpty()) {
                        try {
                            String cloudinaryUrl = extractAndUploadChapterImageOptimized(epubBook, imgSrc, bookId, imageCache);
                            if (cloudinaryUrl != null) {
                                img.attr("src", cloudinaryUrl);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to upload image {}: {}", imgSrc, e.getMessage());
                        }
                    }
                }

                String content;
                String htmlContentStr;

                // Extract content based on anchor
                if (anchorId != null) {
                    // This is a subchapter with anchor
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
                    if (startElement != null) {
                        StringBuilder sb = new StringBuilder();
                        StringBuilder htmlSb = new StringBuilder();

                        Element current = startElement;
                        boolean started = false;

                        while (current != null) {
                            if (!started && current.id().equals(anchorId)) {
                                started = true;
                            }

                            if (started) {
                                if (!current.id().isEmpty() && !current.id().equals(anchorId) && current.id().equals(nextAnchorInSameFile)) {
                                    log.debug("Stopping at next anchor: {}", nextAnchorInSameFile);
                                    break;
                                }

                                sb.append(current.text()).append("\n");
                                htmlSb.append(current.outerHtml());
                            }

                            current = current.nextElementSibling();
                        }

                        content = sb.toString().trim();
                        htmlContentStr = htmlSb.toString();
                    } else {
                        log.warn("Anchor not found: {}", anchorId);
                        content = doc.body().text().trim();
                        htmlContentStr = doc.body().html();
                    }
                } else {
                    // No anchor - parent chapter
                    String firstSubAnchor = null;
                    for (int j = i + 1; j < orderedToc.size(); j++) {
                        ChapterHierarchy next = orderedToc.get(j);
                        if (next.getHref().startsWith(fileName + "#")) {
                            firstSubAnchor = next.getHref().split("#")[1];
                            log.debug("Found sub-chapter anchor in same file: {}", firstSubAnchor);
                            break;
                        } else if (!next.getHref().startsWith(fileName)) {
                            break;
                        }
                    }

                    if (firstSubAnchor != null) {
                        Element stopElement = doc.getElementById(firstSubAnchor);

                        if (stopElement != null) {
                            Element sectionElement = doc.select("section.chapter").first();

                            if (sectionElement != null) {
                                StringBuilder sb = new StringBuilder();
                                StringBuilder htmlSb = new StringBuilder();

                                htmlSb.append("<section class=\"chapter\" epub:type=\"chapter\">\n");

                                for (Element child : sectionElement.children()) {
                                    if (child.id().equals(firstSubAnchor)) {
                                        log.debug("Stopped before sub-chapter anchor: {}", firstSubAnchor);
                                        break;
                                    }

                                    if (child.getElementById(firstSubAnchor) != null) {
                                        log.debug("Found anchor inside child, stopping here");
                                        break;
                                    }

                                    sb.append(child.text()).append("\n");
                                    htmlSb.append(child.outerHtml()).append("\n");
                                }

                                htmlSb.append("</section>");

                                content = sb.toString().trim();
                                htmlContentStr = htmlSb.toString();

                                log.info("Extracted parent chapter content, stopped before: {}", firstSubAnchor);
                            } else {
                                content = doc.body().text().trim();
                                htmlContentStr = doc.body().html();
                            }
                        } else {
                            log.warn("Sub-chapter anchor element not found: {}", firstSubAnchor);
                            content = doc.body().text().trim();
                            htmlContentStr = doc.body().html();
                        }
                    } else {
                        // No sub-chapters - use entire document
                        content = doc.body().text().trim();
                        htmlContentStr = doc.body().html();
                    }
                }

                int wordCount = fileUtil.countWords(content);

                // Find parent chapter ID
                Long parentChapterId = null;
                if (hierarchy.getParentHref() != null && !hierarchy.getParentHref().isEmpty()) {
                    parentChapterId = hrefToChapterId.get(hierarchy.getParentHref());
                    if (parentChapterId == null) {
                        log.warn("Parent chapter not found for href: {} (looking for parent: {})", fullHref, hierarchy.getParentHref());
                    }
                }

                // Create chapter
                BookChapter chapter = new BookChapter();
                chapter.setBookId(bookId);
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

                chapterMapper.insertChapter(chapter);
                chapters.add(chapter);

                hrefToChapterId.put(fullHref, chapter.getId());

                log.info("Saved chapter {} (Level {}): {} [Parent: {}]", chapter.getChapterNumber(), chapter.getChapterLevel(), hierarchy.getTitle(), parentChapterId != null ? "#" + parentChapterId : "Root");

            } catch (Exception e) {
                log.error("Failed to extract chapter '{}': {}", hierarchy.getTitle(), e.getMessage(), e);
            }
        }

        log.info("Successfully extracted and saved {} chapters", chapters.size());
        return chapters;
    }

    /**
     * ✅ BUILD IMAGE CACHE from existing chapters
     */
    private Map<String, String> buildExistingImageCache(List<BookChapter> existingChapters) {
        Map<String, String> imageCache = new HashMap<>();

        for (BookChapter chapter : existingChapters) {
            if (chapter.getHtmlContent() == null) continue;

            try {
                Document doc = Jsoup.parse(chapter.getHtmlContent());
                Elements images = doc.select("img");

                for (Element img : images) {
                    String src = img.attr("src");
                    String alt = img.attr("alt");

                    // Only cache Cloudinary URLs
                    if (src.contains("cloudinary.com")) {
                        String key = extractImageKey(src, alt);
                        if (key != null) {
                            imageCache.put(key, src);
                            log.debug("Cached existing image: {} -> {}", key, src);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse chapter HTML for image cache: {}", e.getMessage());
            }
        }

        log.info("Built image cache with {} entries", imageCache.size());
        return imageCache;
    }

    /**
     * Extract image key from cloudinary URL or alt text
     */
    private String extractImageKey(String cloudinaryUrl, String alt) {
        // Try to extract original filename from Cloudinary URL
        // Format: .../books/123/chapters/image-name.webp
        Pattern pattern = Pattern.compile("/chapters/([^/]+?)(?:\\.[^.]+)?$");
        Matcher matcher = pattern.matcher(cloudinaryUrl);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Fallback to alt text
        if (alt != null && !alt.isEmpty()) {
            return fileUtil.sanitizeFilename(alt);
        }

        return null;
    }

    /**
     * ✅ OPTIMIZED: Upload image only if not exists in cache
     */
    private String extractAndUploadChapterImageOptimized(
            nl.siegmann.epublib.domain.Book epubBook,
            String imagePath,
            Long bookId,
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
            String fileName = normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1);
            String imageKey = fileUtil.sanitizeFilename(fileName.replaceAll("\\.[^.]+$", ""));

            // ✅ CHECK CACHE FIRST
            String cachedUrl = imageCache.get(imageKey);
            if (cachedUrl != null) {
                log.info("♻️ Reusing existing image: {} -> {}", imagePath, cachedUrl);
                return cachedUrl;
            }

            // ✅ IMAGE NOT IN CACHE - UPLOAD NEW
            String cloudinaryUrl = fileUtil.uploadChapterImageFromBytes(imageData, bookId, fileName);

            // Add to cache for future use in same processing
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
            List<SpineReference> spine = epubBook.getSpine().getSpineReferences();

            for (SpineReference ref : spine) {
                Resource resource = ref.getResource();
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(htmlContent);

                Element nav = doc.select("nav[epub:type='toc']").first();
                if (nav == null) {
                    nav = doc.select("#toc").first();
                }

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

        Elements listItems = ol.select("> li");

        for (Element li : listItems) {
            Element link = li.select("> a").first();
            if (link == null) continue;

            String href = link.attr("href");
            String title = link.text();

            ChapterHierarchy ch = new ChapterHierarchy(href, title, level, parentHref);
            ordered.add(ch);

            log.debug("TOC Order #{}: Level {} - {} -> {} (parent: {})", ordered.size(), level, title, href, parentHref);

            Element nestedOl = li.select("> ol").first();
            if (nestedOl != null) {
                parseOrderedToc(nestedOl, ordered, level + 1, href);
            }
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
            if (r.getHref().endsWith(fileName)) {
                return r;
            }
        }

        return null;
    }

    private Map<String, ChapterHierarchy> parseTocStructure(nl.siegmann.epublib.domain.Book epubBook) {
        Map<String, ChapterHierarchy> structure = new HashMap<>();

        try {
            List<SpineReference> spine = epubBook.getSpine().getSpineReferences();

            for (SpineReference ref : spine) {
                Resource resource = ref.getResource();
                String htmlContent = new String(resource.getData(), StandardCharsets.UTF_8);
                Document doc = Jsoup.parse(htmlContent);

                Element nav = doc.select("nav[epub:type='toc']").first();
                if (nav == null) {
                    nav = doc.select("#toc").first();
                }

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

        Elements listItems = ol.select("> li");

        for (Element li : listItems) {
            Element link = li.select("> a").first();
            if (link == null) continue;

            String href = link.attr("href");
            String title = link.text();

            structure.put(href, new ChapterHierarchy(href, title, level, parentHref));

            Element nestedOl = li.select("> ol").first();
            if (nestedOl != null) {
                parseTocRecursive(nestedOl, structure, level + 1, href);
            }
        }
    }

    private String extractAndUploadCover(Resource coverResource, Long bookId, String bookTitle) {
        try {
            byte[] imageData = coverResource.getData();
            String coverUrl = fileUtil.uploadBookCoverFromBytes(imageData, bookTitle, bookId);
            log.info("Uploaded EPUB cover image: {}", coverUrl);
            return coverUrl;
        } catch (Exception e) {
            log.error("Failed to upload EPUB cover image: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public BookChapter getChapter(Long bookId, Integer chapterNumber) {
        BookChapter chapter = chapterMapper.findChapterByNumber(bookId, chapterNumber);
        if (chapter == null) {
            throw new DataNotFoundException();
        }
        return chapter;
    }

    @Override
    public List<BookChapter> getAllChapters(Long bookId) {
        return chapterMapper.findChaptersByBookId(bookId);
    }

    @Override
    public List<BookChapter> searchInBook(Long bookId, String query) {
        return chapterMapper.searchInBook(bookId, query);
    }

    @Override
    public void deleteChaptersByBookId(Long bookId) {
        chapterMapper.deleteChaptersByBookId(bookId);
        log.info("Deleted all chapters for book ID: {}", bookId);
    }

    // ==================== PROCESS EPUB FOR UPDATE ====================

    @Override
    public EpubProcessResult processEpubFileForUpdate(MultipartFile epubFile, Book book) throws IOException {
        log.info("Processing EPUB for UPDATE - book: {} (ID: {})", book.getTitle(), book.getId());

        EpubProcessResult result = new EpubProcessResult();

        try (InputStream is = epubFile.getInputStream()) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(is);

            // 1. Parse TOC structure
            Map<String, ChapterHierarchy> tocStructure = parseTocStructure(epubBook);

            // 2. Get existing chapters from database
            List<BookChapter> existingChapters = chapterMapper.findChaptersByBookId(book.getId());
            Map<Integer, BookChapter> existingChapterMap = new HashMap<>();
            for (BookChapter chapter : existingChapters) {
                existingChapterMap.put(chapter.getChapterNumber(), chapter);
            }

            // ✅ 3. BUILD IMAGE CACHE from existing chapters
            Map<String, String> imageCache = buildExistingImageCache(existingChapters);

            // 4. Extract and update/insert chapters WITH IMAGE CACHE
            List<BookChapter> processedChapters = updateOrInsertChaptersWithHierarchy(epubBook, book.getId(), tocStructure, existingChapterMap, imageCache);

            result.setChapters(processedChapters);
            result.setTotalChapters(processedChapters.size());

            // 5. Delete chapters that no longer exist in new EPUB
            Set<Integer> processedChapterNumbers = processedChapters.stream()
                    .map(BookChapter::getChapterNumber)
                    .collect(Collectors.toSet());

            for (BookChapter existingChapter : existingChapters) {
                if (!processedChapterNumbers.contains(existingChapter.getChapterNumber())) {
                    chapterMapper.deleteChapterById(existingChapter.getId());
                    log.info("Deleted obsolete chapter: {} (ID: {})",
                            existingChapter.getTitle(), existingChapter.getId());
                }
            }

            // 6. Calculate total words
            long totalWords = processedChapters.stream()
                    .mapToLong(BookChapter::getWordCount)
                    .sum();
            result.setTotalWords(totalWords);

            // 7. Extract cover
            Resource coverResource = epubBook.getCoverImage();
            if (coverResource != null) {
                String coverUrl = extractAndUploadCover(coverResource, book.getId(), book.getTitle());
                result.setCoverImageUrl(coverUrl);
            }

            // 8. Generate preview
            if (!processedChapters.isEmpty()) {
                String preview = fileUtil.generatePreviewText(processedChapters.getFirst().getContent(), 500);
                result.setPreviewText(preview);
            }

            log.info("EPUB update completed: {} chapters updated/inserted, {} words",
                    processedChapters.size(), totalWords);

            return result;

        } catch (Exception e) {
            log.error("Failed to update EPUB: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ✅ UPDATE OR INSERT chapters with IMAGE CACHE support
     */
    private List<BookChapter> updateOrInsertChaptersWithHierarchy(
            nl.siegmann.epublib.domain.Book epubBook,
            Long bookId,
            Map<String, ChapterHierarchy> tocStructure,
            Map<Integer, BookChapter> existingChapterMap,
            Map<String, String> imageCache) {

        List<BookChapter> chapters = new ArrayList<>();
        Map<String, Long> hrefToChapterId = new HashMap<>();

        log.info("TOC contains {} entries", tocStructure.size());

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

                // ✅ OPTIMIZED: Process images with caching
                Elements imgElements = doc.select("img");
                for (Element img : imgElements) {
                    String imgSrc = img.attr("src");
                    if (!imgSrc.isEmpty()) {
                        try {
                            String cloudinaryUrl = extractAndUploadChapterImageOptimized(
                                    epubBook, imgSrc, bookId, imageCache
                            );
                            if (cloudinaryUrl != null) {
                                img.attr("src", cloudinaryUrl);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to upload image {}: {}", imgSrc, e.getMessage());
                        }
                    }
                }

                String content;
                String htmlContentStr;

                // Extract content (same logic as before)
                if (anchorId != null) {
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
                    if (startElement != null) {
                        StringBuilder sb = new StringBuilder();
                        StringBuilder htmlSb = new StringBuilder();
                        Element current = startElement;
                        boolean started = false;

                        while (current != null) {
                            if (!started && current.id().equals(anchorId)) {
                                started = true;
                            }

                            if (started) {
                                if (!current.id().isEmpty() && !current.id().equals(anchorId)
                                        && current.id().equals(nextAnchorInSameFile)) {
                                    break;
                                }
                                sb.append(current.text()).append("\n");
                                htmlSb.append(current.outerHtml());
                            }
                            current = current.nextElementSibling();
                        }

                        content = sb.toString().trim();
                        htmlContentStr = htmlSb.toString();
                    } else {
                        content = doc.body().text().trim();
                        htmlContentStr = doc.body().html();
                    }
                } else {
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

                    if (firstSubAnchor != null) {
                        Element stopElement = doc.getElementById(firstSubAnchor);
                        if (stopElement != null) {
                            Element sectionElement = doc.select("section.chapter").first();
                            if (sectionElement != null) {
                                StringBuilder sb = new StringBuilder();
                                StringBuilder htmlSb = new StringBuilder();
                                htmlSb.append("<section class=\"chapter\" epub:type=\"chapter\">\n");

                                for (Element child : sectionElement.children()) {
                                    if (child.id().equals(firstSubAnchor)) {
                                        break;
                                    }
                                    if (child.getElementById(firstSubAnchor) != null) {
                                        break;
                                    }
                                    sb.append(child.text()).append("\n");
                                    htmlSb.append(child.outerHtml()).append("\n");
                                }

                                htmlSb.append("</section>");
                                content = sb.toString().trim();
                                htmlContentStr = htmlSb.toString();
                            } else {
                                content = doc.body().text().trim();
                                htmlContentStr = doc.body().html();
                            }
                        } else {
                            content = doc.body().text().trim();
                            htmlContentStr = doc.body().html();
                        }
                    } else {
                        content = doc.body().text().trim();
                        htmlContentStr = doc.body().html();
                    }
                }

                int wordCount = fileUtil.countWords(content);

                Long parentChapterId = null;
                if (hierarchy.getParentHref() != null && !hierarchy.getParentHref().isEmpty()) {
                    parentChapterId = hrefToChapterId.get(hierarchy.getParentHref());
                }

                // ✅ UPDATE OR INSERT LOGIC
                BookChapter chapter = existingChapterMap.get(chapterNumber);

                if (chapter != null) {
                    // UPDATE existing chapter
                    chapter.setTitle(fileUtil.toTitleCase(hierarchy.getTitle()));
                    chapter.setSlug(fileUtil.sanitizeFilename(hierarchy.getTitle()));
                    chapter.setContent(content);
                    chapter.setHtmlContent(htmlContentStr);
                    chapter.setWordCount(wordCount);
                    chapter.setParentChapterId(parentChapterId);
                    chapter.setChapterLevel(hierarchy.getLevel());
                    chapter.setUpdatedAt(LocalDateTime.now());

                    chapterMapper.updateChapter(chapter);
                    log.info("✏️ Updated chapter {} (ID: {}): {}", chapter.getChapterNumber(), chapter.getId(), hierarchy.getTitle());
                } else {
                    // INSERT new chapter
                    chapter = new BookChapter();
                    chapter.setBookId(bookId);
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

                    chapterMapper.insertChapter(chapter);
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
}