package com.masasilam.app.model.dto.request.social;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class UpdateSocialProfileRequest {
    @Size(max = 100)
    private String displayName;

    @Size(max = 255)
    private String tagline;

    @Size(max = 100)
    private String location;

    @Size(max = 500)
    private String websiteUrl;

    private Map<String, String> socialLinks; // {twitter, instagram, goodreads}

    private String readingVisibility;    // public, followers, private
    private String annotationVisibility; // public, followers, private
    private String profileTheme;
}