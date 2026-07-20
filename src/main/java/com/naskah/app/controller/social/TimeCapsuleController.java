package com.naskah.app.controller.social;

import com.naskah.app.mapper.NewspaperMapper;
import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/time-capsule")
@RequiredArgsConstructor
public class TimeCapsuleController {
    private final NewspaperMapper newspaperMapper;

    private static final String SUCCESS = "Success";
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    @GetMapping
    public ResponseEntity<DataResponse<TimeCapsuleResponse>> getTimeCapsule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                                            @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                            @RequestParam(defaultValue = "20") @Min(1) int limit) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int month = targetDate.getMonthValue();
        int day = targetDate.getDayOfMonth();

        List<com.naskah.app.model.dto.newspaper.NewspaperArticleResponse> articles = newspaperMapper.getArticlesOnThisDay(month, day, (page - 1) * limit, limit);
        int totalCount = newspaperMapper.countArticlesOnThisDay(month, day);

        List<TimeCapsuleContentResponse> contentList = articles.stream()
                .map(a -> {
                    TimeCapsuleContentResponse c = new TimeCapsuleContentResponse();
                    c.setEntityId(a.getId());
                    c.setEntityType("NEWSPAPER");
                    c.setEntityTitle(a.getTitle());
                    c.setEntitySlug(a.getSlug());
                    c.setCategory(a.getCategory());
                    c.setExcerpt(a.getExcerpt());
                    c.setReaderCount(0);
                    return c;
                }).toList();

        TimeCapsuleResponse response = new TimeCapsuleResponse();
        response.setHistoricalDate(targetDate);
        response.setFormattedDate(targetDate.format(ID_FORMAT));
        response.setYearDifference(LocalDate.now().getYear() - targetDate.getYear());
        response.setArticles(contentList);
        response.setTotalReaders(totalCount);

        return ResponseEntity.ok(new DataResponse<>(SUCCESS, "Time capsule retrieved", HttpStatus.OK.value(), response));
    }

    @GetMapping("/today")
    public ResponseEntity<DataResponse<TimeCapsuleResponse>> getTodayInHistory(@RequestParam(defaultValue = "78") int yearsAgo,
                                                                               @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                               @RequestParam(defaultValue = "20") @Min(1) int limit) {
        LocalDate today = LocalDate.now();
        LocalDate historicalDate;
        try {
            historicalDate = LocalDate.of(today.getYear() - yearsAgo, today.getMonthValue(), today.getDayOfMonth());
        } catch (Exception e) {
            historicalDate = LocalDate.of(today.getYear() - yearsAgo, today.getMonthValue(), 28);
        }
        return getTimeCapsule(historicalDate, page, limit);
    }
}