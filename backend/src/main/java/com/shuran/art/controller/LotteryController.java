package com.shuran.art.controller;

import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.service.LotteryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    @GetMapping("/prizes")
    public Result<List<Prize>> getPrizes() {
        return Result.success(lotteryService.getPrizes());
    }

    /**
     * 抽奖接口
     * @param body 请求体，包含 activityId
     */
    @PostMapping("/draw")
    public Result<LotteryResponse> draw(
            HttpServletRequest request,
            @RequestBody Map<String, Long> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long activityId = body.get("activityId");
        if (activityId == null) {
            return Result.error("活动ID不能为空");
        }
        LotteryResponse response = lotteryService.draw(userId, activityId);
        return Result.success(response);
    }

    /**
     * 获取用户在某活动的抽奖状态
     * @param activityId 活动ID
     */
    @GetMapping("/status")
    public Result<LotteryService.LotteryStatusResponse> getStatus(
            HttpServletRequest request,
            @RequestParam Long activityId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(lotteryService.getLotteryStatus(userId, activityId));
    }

    /**
     * 获取用户所有抽奖记录
     */
    @GetMapping("/records")
    public Result<List<LotteryRecord>> getRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(lotteryService.getUserRecords(userId));
    }

    /**
     * 获取用户在某活动的抽奖记录
     * @param activityId 活动ID
     */
    @GetMapping("/records/activity")
    public Result<List<LotteryRecord>> getRecordsByActivity(
            HttpServletRequest request,
            @RequestParam Long activityId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(lotteryService.getUserRecordsByActivity(userId, activityId));
    }
}
