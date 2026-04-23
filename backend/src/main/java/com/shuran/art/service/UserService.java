package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.LoginRequest;
import com.shuran.art.dto.LoginResponse;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.UserMapper;
import com.shuran.art.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    @SuppressWarnings("unchecked")
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 调用微信接口获取 openid
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, request.getCode()
        );

        RestTemplate restTemplate = new RestTemplate();
        // 添加支持 text/plain 的 JSON 转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN,
            new MediaType("application", "*+json")
        ));
        restTemplate.getMessageConverters().add(0, converter);

        Map<String, Object> wxResult = restTemplate.getForObject(url, Map.class);

        if (wxResult == null || wxResult.containsKey("errcode")) {
            log.error("微信登录失败: {}", wxResult);
            throw new RuntimeException("微信登录失败");
        }

        String openid = (String) wxResult.get("openid");

        // 查找或创建用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setPoints(0);
            user.setLotteryChances(0);
            userMapper.insert(user);
        }

        // 生成 token
        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickName(user.getNickName())
                .avatarUrl(user.getAvatarUrl())
                .points(user.getPoints())
                .lotteryChances(user.getLotteryChances())
                .build();
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }
}
