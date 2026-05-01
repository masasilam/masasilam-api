package com.naskah.demo.service.social;

import com.naskah.demo.model.dto.response.*;
import com.naskah.demo.model.dto.response.social.*;

public interface ReadingTwinService {
    DataResponse<java.util.List<ReadingTwinResponse>> getMyTwins(int page, int limit);
    DataResponse<ReadingTwinResponse> getTwinWith(Long otherUserId);
    void calculateTwinsForUser(Long userId);  // called async/scheduled
}