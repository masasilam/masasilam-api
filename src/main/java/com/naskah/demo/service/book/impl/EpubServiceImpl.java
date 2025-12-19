package com.naskah.demo.service.book.impl;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EpubServiceImpl implements EpubService {

    private final BookChapterMapper chapterMapper;

    @Override
    public EpubProcessResult processEpubFile(MultipartFile epubFile, Book book) throws IOException {
        log.info("Processing EPUB for book: {} (ID: {})", book.getTitle(), book.getId());

        EpubProcessResult result = new EpubProcessResult();

        try (InputStream is = epubFile.getInputStream()) {
            EpubReader reader = new EpubReader();
            nl.siegmann.epublib.domain.Book epubBook = reader.readEpub(is);

            // 1. Parse TOC structure
            Map<String, ChapterHierarchy> tocStructure = parseTocStructure(epubBook);

            // 2. Extract chapters with hierarchy
            List<BookChapter> chapters = extractAndSaveChaptersWithHierarchy(
                    epubBook, book.getId(), tocStructure
            );

            result.setChapters(chapters);
            result.setTotalChapters(chapters.size());

            // 3. Calculate total words
            long totalWords = chapters.stream()
                    .mapToLong(BookChapter::getWordCount)
                    .sum();
            result.setTotalWords(totalWords);

            // 4. Extract cover
            Resource coverResource = epubBook.getCoverImage();
            if (coverResource != null) {
                String coverUrl = extractAndUploadCover(coverResource, book.getId(), book.getTitle());
                result.setCoverImageUrl(coverUrl);
            }

            // 5. Generate preview
            if (!chapters.isEmpty()) {
                String preview = FileUtil.generatePreviewText(chapters.get(0).getContent(), 500);
                result.setPreviewText(preview);
            }

            log.info("EPUB processing completed: {} chapters, {} words",
                    chapters.size(), totalWords);

            return result;

        } catch (Exception e) {
            log.error("Failed to process EPUB: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static class ChapterHierarchy {
        String href;
        String title;
        int level;
        String parentHref;

        public ChapterHierarchy(String href, String title, int level, String parentHref) {
            this.href = href;
            this.title = title;
            this.level = level;
            this.parentHref = parentHref;
        }
    }

    /**
     * ✅ FIX 3: Extract chapters WITHOUT sub-chapter content
     */
    private List<BookChapter> extractAndSaveChaptersWithHierarchy(
            nl.siegmann.epublib.domain.Book epubBook,
            Long bookId,
            Map<String, ChapterHierarchy> tocStructure) {

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

                String fullHref = hierarchy.href;
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

                // Process images
                Elements imgElements = doc.select("img");
                for (Element img : imgElements) {
                    String imgSrc = img.attr("src");
                    if (!imgSrc.isEmpty()) {
                        try {
                            String cloudinaryUrl = extractAndUploadChapterImage(epubBook, imgSrc, bookId);
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

                // ✅ FIX 3: Stop at next TOC entry in same file
                if (anchorId != null) {
                    // This is a sub-chapter with anchor (e.g., #sigil_toc_id_1)
                    // Get next TOC entry to know where to stop
                    String nextAnchorInSameFile = null;
                    for (int j = i + 1; j < orderedToc.size(); j++) {
                        ChapterHierarchy next = orderedToc.get(j);
                        if (next.href.startsWith(fileName + "#")) {
                            nextAnchorInSameFile = next.href.split("#")[1];
                            break;
                        } else if (!next.href.startsWith(fileName)) {
                            // Next chapter is in different file
                            break;
                        }
                    }

                    // Extract content from anchor to next anchor (or end)
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
                                // ✅ Stop if we hit the next anchor
                                if (!current.id().isEmpty()
                                        && !current.id().equals(anchorId)
                                        && current.id().equals(nextAnchorInSameFile)) {
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
                    // No anchor - this is a parent chapter
                    // Check if there are sub-chapters (anchors) in this file
                    String firstSubAnchor = null;
                    for (int j = i + 1; j < orderedToc.size(); j++) {
                        ChapterHierarchy next = orderedToc.get(j);
                        if (next.href.startsWith(fileName + "#")) {
                            // Found a sub-chapter in the same file
                            firstSubAnchor = next.href.split("#")[1];
                            log.debug("Found sub-chapter anchor in same file: {}", firstSubAnchor);
                            break;
                        } else if (!next.href.startsWith(fileName)) {
                            // Next TOC entry is in different file
                            break;
                        }
                    }

                    if (firstSubAnchor != null) {
                        // ✅ Parent chapter: Extract ONLY content BEFORE first sub-chapter
                        Element stopElement = doc.getElementById(firstSubAnchor);

                        if (stopElement != null) {
                            // Extract elements inside <section> but before the anchor
                            Element sectionElement = doc.select("section.chapter").first();

                            if (sectionElement != null) {
                                StringBuilder sb = new StringBuilder();
                                StringBuilder htmlSb = new StringBuilder();

                                // Add section opening tag
                                htmlSb.append("<section class=\"chapter\" epub:type=\"chapter\">\n");

                                // Iterate through children and stop at anchor
                                for (Element child : sectionElement.children()) {
                                    // Stop when we reach the anchor element
                                    if (child.id().equals(firstSubAnchor)) {
                                        log.debug("Stopped before sub-chapter anchor: {}", firstSubAnchor);
                                        break;
                                    }

                                    // Check if any descendant has the anchor ID
                                    if (child.getElementById(firstSubAnchor) != null) {
                                        log.debug("Found anchor inside child, stopping here");
                                        break;
                                    }

                                    sb.append(child.text()).append("\n");
                                    htmlSb.append(child.outerHtml()).append("\n");
                                }

                                // Add section closing tag
                                htmlSb.append("</section>");

                                content = sb.toString().trim();
                                htmlContentStr = htmlSb.toString();

                                log.info("Extracted parent chapter content, stopped before: {}", firstSubAnchor);
                            } else {
                                // Fallback if section not found
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

                int wordCount = FileUtil.countWords(content);

                // Find parent chapter ID
                Long parentChapterId = null;
                if (hierarchy.parentHref != null && !hierarchy.parentHref.isEmpty()) {
                    parentChapterId = hrefToChapterId.get(hierarchy.parentHref);
                    if (parentChapterId == null) {
                        log.warn("Parent chapter not found for href: {} (looking for parent: {})",
                                fullHref, hierarchy.parentHref);
                    }
                }

                // Create chapter
                BookChapter chapter = new BookChapter();
                chapter.setBookId(bookId);
                chapter.setChapterNumber(chapterNumber);
                chapter.setTitle(hierarchy.title);
                chapter.setSlug(FileUtil.sanitizeFilename(hierarchy.title));
                chapter.setContent(content);
                chapter.setHtmlContent(htmlContentStr);
                chapter.setWordCount(wordCount);
                chapter.setParentChapterId(parentChapterId);
                chapter.setChapterLevel(hierarchy.level);
                chapter.setCreatedAt(LocalDateTime.now());
                chapter.setUpdatedAt(LocalDateTime.now());

                chapterMapper.insertChapter(chapter);
                chapters.add(chapter);

                hrefToChapterId.put(fullHref, chapter.getId());

                log.info("Saved chapter {} (Level {}): {} [Parent: {}]",
                        chapter.getChapterNumber(),
                        chapter.getChapterLevel(),
                        hierarchy.title,
                        parentChapterId != null ? "#" + parentChapterId : "Root");

            } catch (Exception e) {
                log.error("Failed to extract chapter '{}': {}", hierarchy.title, e.getMessage(), e);
            }
        }

        log.info("Successfully extracted and saved {} chapters", chapters.size());
        return chapters;
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

    private void parseOrderedToc(Element ol, List<ChapterHierarchy> ordered,
                                 int level, String parentHref) {
        if (ol == null) return;

        Elements listItems = ol.select("> li");

        for (Element li : listItems) {
            Element link = li.select("> a").first();
            if (link == null) continue;

            String href = link.attr("href");
            String title = link.text();

            ChapterHierarchy ch = new ChapterHierarchy(href, title, level, parentHref);
            ordered.add(ch);

            log.debug("TOC Order #{}: Level {} - {} -> {} (parent: {})",
                    ordered.size(), level, title, href, parentHref);

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

    private void parseTocRecursive(Element ol, Map<String, ChapterHierarchy> structure,
                                   int level, String parentHref) {
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

    private String extractAndUploadChapterImage(nl.siegmann.epublib.domain.Book epubBook,
                                                String imagePath, Long bookId) {
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

            String cloudinaryUrl = FileUtil.uploadChapterImageFromBytes(
                    imageData,
                    bookId,
                    fileName
            );

            log.info("Uploaded chapter image: {} -> {}", imagePath, cloudinaryUrl);
            return cloudinaryUrl;

        } catch (Exception e) {
            log.error("Failed to upload chapter image {}: {}", imagePath, e.getMessage());
            return null;
        }
    }

    private String extractAndUploadCover(Resource coverResource, Long bookId, String bookTitle) {
        try {
            byte[] imageData = coverResource.getData();
            String coverUrl = FileUtil.uploadBookCoverFromBytes(imageData, bookTitle, bookId);
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
            throw new RuntimeException("Chapter not found");
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
}