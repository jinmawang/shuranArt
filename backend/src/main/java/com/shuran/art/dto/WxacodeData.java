package com.shuran.art.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序码响应数据 DTO
 * L2 设计: PST-data-detail.md 6.1 WxacodeData
 * L1 契约: PST.openapi.yml WxacodeData schema
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WxacodeData {
    /**
     * 小程序码图片 Base64 编码字符串（PNG 格式）
     * 不含 data:image/png;base64, 前缀，前端自行拼接
     */
    private String wxacodeBase64;
}
