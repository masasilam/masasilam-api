package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CollaborativeNoteResponse {
    private Long id;
    private Long bookId;
    private Integer page;
    private String position;
    private String title;
    private String content;
    private String authorName;
    private List<String> collaborators;
    private String visibility;
    private Integer editCount;
    private LocalDateTime lastEditedAt;
    private String lastEditedBy;
    private LocalDateTime createdAt;
    private List<EditHistory> editHistory;

    @Data
    public static class EditHistory {
        private String editorName;
        private String action; // CREATE, UPDATE, DELETE
        private LocalDateTime timestamp;
        private String changes; // Brief description of changes
    }
}