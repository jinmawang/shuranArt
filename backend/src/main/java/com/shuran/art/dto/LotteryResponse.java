package com.shuran.art.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotteryResponse {
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer prizeValue;
    private String icon;
    private Boolean needClaim;
    private String claimCode;
}
