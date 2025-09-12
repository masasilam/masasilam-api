package com.naskah.demo.model.dto.request;

import com.naskah.demo.model.entity.Tag;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookRequest {
    @NotBlank
    private String title;

    @NotNull
    private MultipartFile coverImage;

    @NotNull
    private MultipartFile bookFile;

    @NotBlank
    private String language;

    @NotBlank
    private String publisher;

    @NotNull
    private Integer publicationYear;

    @NotBlank
    private String description;

    @NotBlank
    private String copyrightStatus;

    @NotBlank
    private String category;

    @NotEmpty
    private List<Long> genreIds;

    @NotEmpty
    private List<Tag> tagIds;

    @NotEmpty
    private List<AuthorRequest> authors;

    private String isbn;
    private String subtitle;
    private Integer seriesId;
    private Integer seriesOrder;
    private String summary;
    private LocalDateTime publishedAt;
}
