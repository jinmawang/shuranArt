package com.shuran.art.controller;

import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.service.LotteryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    @GetMapping("/prizes")
    public Result<List<Prize>> getPrizes() {
        return Result.success(lotteryService.getPrizes());
    }

    @PostMapping("/draw")
    public Result<LotteryResponse> draw(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LotteryResponse response = lotteryService.draw(userId);
        return Result.success(response);
    }

    @GetMapping("/records")
    public Result<List<LotteryRecord>> getRecords(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(lotteryService.getUserRecords(userId));
    }
}
