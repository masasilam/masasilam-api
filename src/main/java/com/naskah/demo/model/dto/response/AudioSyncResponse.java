package com.naskah.demo.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AudioSyncResponse {
    private Long syncId;
    private Integer page;
    private List<SyncPoint> syncPoints;
    private String status;

    @Data
    public static class SyncPoint {
        private String textPosition;
        private Double audioTimestamp;
        private String text;
        private Boolean isVerified;
    }
}
