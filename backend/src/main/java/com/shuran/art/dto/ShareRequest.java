package com.shuran.art.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareRequest {
    @NotNull
    private Long sharerId;
    @NotNull
    private Long activityId;
}
