package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.dto.ShareRequest;
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
            @RequestParam Long activityId) {
        Long sharerId = (Long) request.getAttribute("userId");
        Map<String, Object> result = shareService.createShare(sharerId, activityId);
        return Result.success(result);
    }

    /**
     * 确认分享（被分享者点击链接时调用）
     */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirmShare(
            HttpServletRequest request,
            @RequestParam String shareCode) {
        Long visitorId = (Long) request.getAttribute("userId");
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
