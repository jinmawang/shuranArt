package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.User;
import com.shuran.art.service.AdminService;
import com.shuran.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/invite")
@RequiredArgsConstructor
public class AdminInviteController {

    private final AdminService adminService;
    private final UserService userService;

    @PostMapping("/accept")
    public Result<Void> acceptInvite(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return Result.error("邀请令牌不能为空");
        }
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        adminService.acceptInvite(token, user.getOpenid(), user.getNickName());
        return Result.success();
    }
}
