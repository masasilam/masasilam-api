package com.masasilam.app.service.social.impl;

import com.masasilam.app.exception.custom.*;
import com.masasilam.app.mapper.UserMapper;
import com.masasilam.app.mapper.social.ReadingTwinMapper;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.model.entity.User;
import com.masasilam.app.model.entity.social.ReadingTwin;
import com.masasilam.app.service.social.ReadingTwinService;
import com.masasilam.app.util.interceptor.HeaderHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingTwinServiceImpl implements ReadingTwinService {
    private final ReadingTwinMapper twinMapper;
    private final UserMapper userMapper;
    private final HeaderHolder headerHolder;

    private static final String SUCCESS = "Success";

    private User requireAuth() {
        String username = headerHolder.getUsername();
        if (username == null || username.isBlank()) throw new UnauthorizedException();
        User user = userMapper.findUserByUsername(username);
        if (user == null) throw new UnauthorizedException();
        return user;
    }

    @Override
    public DataResponse<List<ReadingTwinResponse>> getMyTwins(int page, int limit) {
        User me = requireAuth();
        int offset = (page - 1) * limit;
        List<ReadingTwinResponse> twins = twinMapper.findTwinsForUser(me.getId(), me.getId(), offset, limit);
        int total = twinMapper.countTwinsForUser(me.getId());
        return new DataResponse<>(SUCCESS, "Reading twins retrieved", HttpStatus.OK.value(), twins);
    }

    @Override
    public DataResponse<ReadingTwinResponse> getTwinWith(Long otherUserId) {
        User me = requireAuth();
        ReadingTwin twin = twinMapper.findByUsers(me.getId(), otherUserId);
        if (twin == null) {
            calculateTwinsForUser(me.getId());
            twin = twinMapper.findByUsers(me.getId(), otherUserId);
        }
        if (twin == null) throw new DataNotFoundException();

        List<ReadingTwinResponse> twins = twinMapper.findTwinsForUser(me.getId(), me.getId(), 0, 100);
        ReadingTwinResponse response = twins.stream()
                .filter(t -> t.getUserId().equals(otherUserId))
                .findFirst().orElseThrow(DataNotFoundException::new);
        return new DataResponse<>(SUCCESS, "Twin info retrieved", HttpStatus.OK.value(), response);
    }

    @Override
    @Async
    public void calculateTwinsForUser(Long userId) {
        try {
            List<Long> compareIds = twinMapper.findUsersToCompare(userId);
            twinMapper.deleteStaleFor(userId);

            for (Long otherId : compareIds) {
                if (otherId.equals(userId)) continue;
                try {
                    List<Long> myReads = twinMapper.findUsersToCompare(userId);
                    List<Long> theirReads = twinMapper.findUsersToCompare(otherId);

                    long myTotal = myReads.size();
                    long theirTotal = theirReads.size();
                    long common = myReads.stream().filter(theirReads::contains).count();

                    if (common == 0) continue;

                    long union = myTotal + theirTotal - common;
                    double similarity = union > 0 ? (double) common / union : 0;

                    ReadingTwin twin = new ReadingTwin();
                    twin.setUserIdA(Math.min(userId, otherId));
                    twin.setUserIdB(Math.max(userId, otherId));
                    twin.setSimilarityScore(similarity);
                    twin.setCommonCount((int) common);
                    twinMapper.upsert(twin);
                } catch (Exception inner) {
                    log.debug("Twin calc failed for pair ({}, {}): {}", userId, otherId, inner.getMessage());
                }
            }
            log.info("Reading twins calculated for user={}: {} candidates", userId, compareIds.size());
        } catch (Exception e) {
            log.warn("calculateTwinsForUser failed for user={}: {}", userId, e.getMessage());
        }
    }
}