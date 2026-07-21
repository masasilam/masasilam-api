package com.masasilam.app.model.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AchievementsResponse {
    private List<Object> list;
    private Integer total;
    private Integer unlocked;
    private List<Object> categories;
}