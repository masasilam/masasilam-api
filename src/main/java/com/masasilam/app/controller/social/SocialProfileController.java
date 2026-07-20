package com.masasilam.app.controller.social;

import com.masasilam.app.model.dto.request.social.UpdateSocialProfileRequest;
import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;
import com.masasilam.app.service.social.SocialProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/social/profiles")
@RequiredArgsConstructor
public class SocialProfileController {
    private final SocialProfileService profileService;

    @GetMapping("/by-id/{userId}")
    public ResponseEntity<DataResponse<UserPublicProfileResponse>> getProfileById(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getPublicProfile(userId));
    }

    @GetMapping("/{username}")
    public ResponseEntity<DataResponse<UserPublicProfileResponse>> getProfileByUsername(@PathVariable String username) {
        return ResponseEntity.ok(profileService.getPublicProfileByUsername(username));
    }

    @PutMapping("/me")
    public ResponseEntity<DataResponse<UserPublicProfileResponse>> updateMyProfile(@Valid @RequestBody UpdateSocialProfileRequest request) {
        return ResponseEntity.ok(profileService.updateMyProfile(request));
    }

    @GetMapping("/me/stats")
    public ResponseEntity<DataResponse<SocialStatsResponse>> getMyStats() {
        return ResponseEntity.ok(profileService.getMyStats());
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<DataResponse<Void>> followUser(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.followUser(userId));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<DataResponse<Void>> unfollowUser(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.unfollowUser(userId));
    }

    @GetMapping("/{userId}/is-following")
    public ResponseEntity<DataResponse<Boolean>> checkIsFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.checkIsFollowing(userId));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<DataResponse<FollowStatsResponse>> getFollowers(@PathVariable Long userId,
                                                                          @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                          @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(profileService.getFollowers(userId, page, limit));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<DataResponse<FollowStatsResponse>> getFollowing(@PathVariable Long userId,
                                                                          @RequestParam(defaultValue = "1") @Min(1) int page,
                                                                          @RequestParam(defaultValue = "20") @Min(1) int limit) {
        return ResponseEntity.ok(profileService.getFollowing(userId, page, limit));
    }
}