package com.shuran.art.dto;

import lombok.Data;

@Data
public class ShareRequest {
    private Long sharerId;
    private Long activityId;
    private String shareCode;
}
