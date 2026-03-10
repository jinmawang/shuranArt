package com.shuran.art.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickName;
    private String avatarUrl;
    private Integer points;
    private Integer lotteryChances;
}
