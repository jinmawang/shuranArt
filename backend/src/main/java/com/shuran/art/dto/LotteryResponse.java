package com.shuran.art.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LotteryResponse {
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer prizeLevel;  // 奖品等级
    private Integer prizeValue;
    private String icon;
    private Boolean needClaim;
    private String claimCode;
    private Boolean isGuarantee;  // 是否为保底奖品
    private Integer lotteryCount;  // 当前抽奖次数
    private Integer maxLottery;    // 最大抽奖次数
}
