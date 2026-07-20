package com.naskah.app.service.social;

import com.naskah.app.model.dto.response.*;
import com.naskah.app.model.dto.response.social.*;

public interface ReadingTwinService {
    DataResponse<java.util.List<ReadingTwinResponse>> getMyTwins(int page, int limit);
    DataResponse<ReadingTwinResponse> getTwinWith(Long otherUserId);
    void calculateTwinsForUser(Long userId);  // called async/scheduled
}