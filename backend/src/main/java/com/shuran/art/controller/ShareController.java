package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.dto.WxacodeData;
import com.shuran.art.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * 创建分享（用户发起分享时调用）
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createShare(
            HttpServletRequest request,
            @RequestBody Map<String, Long> body) {
        Long sharerId = (Long) request.getAttribute("userId");
        Long activityId = body.get("activityId");
        if (activityId == null) {
            return Result.error("活动ID不能为空");
        }
        Map<String, Object> result = shareService.createShare(sharerId, activityId);
        return Result.success(result);
    }

    /**
     * 确认分享（被分享者点击链接时调用）
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirmShare(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long visitorId = (Long) request.getAttribute("userId");
        String shareCode = body.get("shareCode");
        if (shareCode == null || shareCode.isEmpty()) {
            return Result.error("分享码不能为空");
        }
        Map<String, Object> result = shareService.confirmShare(visitorId, shareCode);
        return Result.success(result);
    }

    /**
     * 获取分享状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getShareStatus(
            HttpServletRequest request,
            @RequestParam Long activityId) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = shareService.getShareStatus(userId, activityId);
        return Result.success(result);
    }

    /**
     * 生成活动小程序码
     * L2 设计: PST-api-detail.md EP-01 GET /api/share/wxacode
     * L1 契约: PST.openapi.yml /api/share/wxacode
     * 需求来源: REQ-PST-009 ~ REQ-PST-011, REQ-PST-022 ~ REQ-PST-023
     *
     * 认证: JWT Bearer Token（需 userId 生成 scene 参数，V-002）
     */
    @GetMapping("/wxacode")
    public Result<WxacodeData> getWxacode(
            HttpServletRequest request,
            @RequestParam Long activityId) {
        // V-001: activityId 由 @RequestParam required=true 默认行为验证（缺失时 Spring 400）
        // V-002: JWT 认证由 AuthInterceptor 统一处理

        // STEP-01: 提取用户 ID（参照 ShareController 现有端点模式）
        Long userId = (Long) request.getAttribute("userId");

        // STEP-02 ~ STEP-08: 委托 ShareService.getWxacode()
        WxacodeData wxacodeData = shareService.getWxacode(userId, activityId);

        if (wxacodeData == null) {
            // L2 设计: 活动不存在时返回 Result.error（AC-4.4）
            return Result.error("活动不存在");
        }

        return Result.success(wxacodeData);
    }

    /**
     * 兼容旧接口
     */
    @PostMapping("/record")
    public Result<Map<String, Object>> recordShare(
            HttpServletRequest request,
            @RequestBody ShareRequest shareRequest) {
        Long visitorId = (Long) request.getAttribute("userId");
        Map<String, Object> result = shareService.recordShare(visitorId, shareRequest);
        return Result.success(result);
    }
}
