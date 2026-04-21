package com.shuran.art.controller;

import com.shuran.art.dto.LoginRequest;
import com.shuran.art.dto.LoginResponse;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.User;
import com.shuran.art.service.AdminService;
import com.shuran.art.service.UserService;
import com.shuran.art.service.WxAccessTokenManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminService adminService;
    private final WxAccessTokenManager wxAccessTokenManager;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @GetMapping("/is-admin")
    public Result<Map<String, Boolean>> isAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.success(Map.of("isAdmin", false));
        }
        boolean admin = adminService.isAdmin(user.getOpenid());
        return Result.success(Map.of("isAdmin", admin));
    }

    @PutMapping("/update")
    public Result<Void> updateUser(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 通过微信 getPhoneNumber code 获取手机号
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/phone")
    public Result<Map<String, String>> getPhone(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return Result.error("code不能为空");
        }
        try {
            String accessToken = wxAccessTokenManager.getAccessToken();
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;

            RestTemplate restTemplate = new RestTemplate();
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")
            ));
            restTemplate.getMessageConverters().add(0, converter);

            Map<String, Object> wxResult = restTemplate.postForObject(url, Map.of("code", code), Map.class);

            if (wxResult != null && Integer.valueOf(0).equals(wxResult.get("errcode"))) {
                Map<String, Object> phoneInfo = (Map<String, Object>) wxResult.get("phone_info");
                String phone = (String) phoneInfo.get("phoneNumber");

                // 同时更新用户表的手机号
                Long userId = (Long) request.getAttribute("userId");
                if (userId != null && phone != null && !phone.isBlank()) {
                    User user = userService.getUserById(userId);
                    if (user != null && (user.getPhone() == null || user.getPhone().isBlank())) {
                        user.setPhone(phone);
                        userService.updateUser(user);
                    }
                }

                return Result.success(Map.of("phone", phone != null ? phone : ""));
            } else {
                log.error("获取手机号失败: {}", wxResult);
                return Result.error("获取手机号失败");
            }
        } catch (Exception e) {
            log.error("获取手机号异常: ", e);
            return Result.error("获取手机号失败");
        }
    }
}
