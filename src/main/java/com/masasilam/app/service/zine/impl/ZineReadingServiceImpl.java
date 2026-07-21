package com.masasilam.app.service.zine.impl;

import com.masasilam.app.exception.custom.DataNotFoundException;
import com.masasilam.app.exception.custom.UnauthorizedException;
import com.masasilam.app.mapper.user.UserMapper;
import com.masasilam.app.mapper.zine.ZineMapper;
import com.masasilam.app.mapper.zine.ZineReadingProgressMapper;
import com.masasilam.app.mapper.zine.ZineReadingSessionMapper;
import com.masasilam.app.model.dto.request.ZineReadingProgressRequest;
import com.masasilam.app.model.dto.request.ZineReadingSessionRequest;
import com.masasilam.app.model.dto.response.DataResponse;
import com.masasilam.app.model.dto.response.ZineReadingProgressResponse;
import com.masasilam.app.model.dto.response.ZineReadingSessionResponse;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.Zine;
import com.masasilam.app.model.entity.ZineReadingProgress;
import com.masasilam.app.model.entity.ZineReadingSession;
import com.masasilam.app.service.zine.ZineReadingService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZineReadingServiceImpl implements ZineReadingService {
    private final ZineReadingSessionMapper sessionMapper;
    private final ZineReadingProgressMapper progressMapper;
    private final ZineMapper zineMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";
    private static final String EPUB = "EPUB";

    @Override
    @Transactional
    public DataResponse<ZineReadingSessionResponse> saveReadingSession(String slug, ZineReadingSessionRequest request) {
        User user = requireUser();
        Zine zine = requireZine(slug);

        ZineReadingSession session = new ZineReadingSession();
        session.setUserId(user.getId());
        session.setZineId(zine.getId());
        session.setSessionType(request.getSessionType() != null ? request.getSessionType() : EPUB);
        session.setStartedAt(request.getStartedAt());
        session.setEndedAt(request.getEndedAt() != null ? request.getEndedAt() : LocalDateTime.now());
        session.setTotalDurationSeconds(request.getTotalDurationSeconds() != null ? request.getTotalDurationSeconds() : 0);
        session.setChaptersRead(request.getChaptersRead() != null ? request.getChaptersRead() : 1);
        session.setStartChapter(request.getStartChapter());
        session.setEndChapter(request.getEndChapter());
        session.setCreatedAt(LocalDateTime.now());

        sessionMapper.insert(session);
        log.info("Zine reading session saved: userId={}, zineId={}, duration={}s",
                user.getId(), zine.getId(), session.getTotalDurationSeconds());

        return new DataResponse<>(SUCCESS, "Sesi baca zine tersimpan",
                HttpStatus.CREATED.value(), toSessionResponse(session, zine));
    }

    @Override
    @Transactional
    public DataResponse<ZineReadingProgressResponse> saveOrUpdateProgress(String slug, ZineReadingProgressRequest request) {
        User user = requireUser();
        Zine zine = requireZine(slug);

        ZineReadingProgress existing = progressMapper.findByUserAndZine(user.getId(), zine.getId());

        if (existing != null) {
            existing.setCurrentPosition(request.getCurrentPosition());
            existing.setCurrentPage(request.getCurrentPage());
            existing.setTotalPages(request.getTotalPages());
            existing.setPercentageCompleted(
                    request.getPercentageCompleted() != null
                            ? request.getPercentageCompleted()
                            : calculatePercentage(request.getCurrentPage(), request.getTotalPages()));
            existing.setLastReadAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            progressMapper.update(existing);

            log.info("Zine progress updated: userId={}, zineId={}, pct={}%",
                    user.getId(), zine.getId(), existing.getPercentageCompleted());

            return new DataResponse<>(SUCCESS, "Progress zine diperbarui",
                    HttpStatus.OK.value(), toProgressResponse(existing, zine));

        } else {
            ZineReadingProgress progress = new ZineReadingProgress();
            progress.setUserId(user.getId());
            progress.setZineId(zine.getId());
            progress.setCurrentPosition(request.getCurrentPosition());
            progress.setCurrentPage(request.getCurrentPage());
            progress.setTotalPages(request.getTotalPages());
            progress.setPercentageCompleted(
                    request.getPercentageCompleted() != null
                            ? request.getPercentageCompleted()
                            : calculatePercentage(request.getCurrentPage(), request.getTotalPages()));
            progress.setLastReadAt(LocalDateTime.now());
            progress.setCreatedAt(LocalDateTime.now());
            progress.setUpdatedAt(LocalDateTime.now());
            progressMapper.insert(progress);

            log.info("Zine progress created: userId={}, zineId={}, pct={}%",
                    user.getId(), zine.getId(), progress.getPercentageCompleted());

            return new DataResponse<>(SUCCESS, "Progress zine tersimpan",
                    HttpStatus.CREATED.value(), toProgressResponse(progress, zine));
        }
    }

    @Override
    public DataResponse<ZineReadingProgressResponse> getProgress(String slug) {
        User user = requireUser();
        Zine zine = requireZine(slug);

        ZineReadingProgress progress = progressMapper.findByUserAndZine(user.getId(), zine.getId());

        if (progress == null) {
            return new DataResponse<>(SUCCESS, "Belum ada progress",
                    HttpStatus.OK.value(), null);
        }

        return new DataResponse<>(SUCCESS, "Progress zine berhasil diambil",
                HttpStatus.OK.value(), toProgressResponse(progress, zine));
    }

    private User requireUser() {
        String username = headerHolder.getUsername();
        if (username == null || username.isEmpty()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    private Zine requireZine(String slug) {
        Zine zine = zineMapper.findZineBySlug(slug);
        if (zine == null) throw new DataNotFoundException();
        return zine;
    }

    private BigDecimal calculatePercentage(Integer currentPage, Integer totalPages) {
        if (totalPages == null || totalPages == 0 || currentPage == null) return BigDecimal.ZERO;
        double pct = (currentPage * 100.0) / totalPages;
        return BigDecimal.valueOf(Math.min(100.0, pct));
    }

    private ZineReadingSessionResponse toSessionResponse(ZineReadingSession session, Zine zine) {
        ZineReadingSessionResponse r = new ZineReadingSessionResponse();
        r.setId(session.getId());
        r.setZineId(zine.getId());
        r.setZineSlug(zine.getSlug());
        r.setZineTitle(zine.getTitle());
        r.setSessionType(session.getSessionType());
        r.setStartedAt(session.getStartedAt());
        r.setEndedAt(session.getEndedAt());
        r.setTotalDurationSeconds(session.getTotalDurationSeconds());
        r.setChaptersRead(session.getChaptersRead());
        r.setStartChapter(session.getStartChapter());
        r.setEndChapter(session.getEndChapter());
        r.setCreatedAt(session.getCreatedAt());
        return r;
    }

    private ZineReadingProgressResponse toProgressResponse(ZineReadingProgress progress, Zine zine) {
        ZineReadingProgressResponse r = new ZineReadingProgressResponse();
        r.setId(progress.getId());
        r.setUserId(progress.getUserId());
        r.setZineId(zine.getId());
        r.setZineSlug(zine.getSlug());
        r.setZineTitle(zine.getTitle());
        r.setCoverImageUrl(zine.getCoverImageUrl());
        r.setCurrentPosition(progress.getCurrentPosition());
        r.setCurrentPage(progress.getCurrentPage());
        r.setTotalPages(progress.getTotalPages());
        r.setPercentageCompleted(progress.getPercentageCompleted());
        r.setLastReadAt(progress.getLastReadAt());
        r.setUpdatedAt(progress.getUpdatedAt());
        return r;
    }
}