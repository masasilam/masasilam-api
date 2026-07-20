package com.masasilam.app.service.social;

import com.masasilam.app.model.dto.response.*;
import com.masasilam.app.model.dto.response.social.*;

public interface ReadingTwinService {
    DataResponse<java.util.List<ReadingTwinResponse>> getMyTwins(int page, int limit);
    DataResponse<ReadingTwinResponse> getTwinWith(Long otherUserId);
    void calculateTwinsForUser(Long userId);  // called async/scheduled
}