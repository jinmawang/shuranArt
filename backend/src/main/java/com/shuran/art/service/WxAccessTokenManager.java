package com.shuran.art.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

/**
 * 微信 access_token 缓存管理器
 *
 * L2 设计: PST-data-detail.md 2.x WxAccessTokenManager
 * 决策 5: 后端新增 access_token 获取和缓存机制
 *
 * 线程安全: synchronized + volatile 双重检查模式
 * 缓存策略: 2h 有效期，提前 5 分钟刷新
 */
@Slf4j
@Component
public class WxAccessTokenManager {

    // L2 STEP: @Value 注入 wx.appid / wx.secret（复用 application.yml 已有配置）
    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    // L2 设计: cachedToken volatile，保证多线程可见性
    private volatile String cachedToken;

    // L2 设计: expireTime volatile，过期时间戳（毫秒）
    private volatile long expireTime;

    // L2 设计: TOKEN_URL 常量
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    // L2 设计: 提前 5 分钟（300 秒）刷新，避免边界过期
    private static final int ADVANCE_REFRESH_SECONDS = 300;

    /**
     * 获取有效的 access_token
     * L2 设计: PST-data-detail.md 2.3 getAccessToken()
     *
     * 线程安全: synchronized + 双重检查，避免并发刷新
     * 缓存有效时直接返回；过期时同步获取新 token
     */
    public String getAccessToken() {
        // L2: 先检查缓存（无锁路径，高性能）
        if (cachedToken != null && System.currentTimeMillis() < expireTime) {
            return cachedToken;
        }

        // L2: 缓存过期或不存在，加锁刷新
        synchronized (this) {
            // L2: 双重检查，避免并发重复刷新
            if (cachedToken != null && System.currentTimeMillis() < expireTime) {
                return cachedToken;
            }
            refreshAccessToken();
            return cachedToken;
        }
    }

    /**
     * 从微信 API 获取新的 access_token 并更新缓存
     * L2 设计: PST-data-detail.md 2.3 refreshAccessToken()
     *
     * 仅在 synchronized 块内调用，或在 STEP-07-RETRY 重试逻辑中主动调用
     */
    @SuppressWarnings("unchecked")
    public void refreshAccessToken() {
        // L2 设计: URL 构造（参照 PST-data-detail.md 4.4）
        String url = TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;

        try {
            // 参照 UserService 中 RestTemplate 使用模式：每次新建，添加 text/plain 支持
            RestTemplate restTemplate = new RestTemplate();
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")
            ));
            restTemplate.getMessageConverters().add(0, converter);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("access_token")) {
                // L2: 成功，更新缓存
                cachedToken = (String) response.get("access_token");
                int expiresIn = (int) response.get("expires_in"); // 通常 7200 秒
                // L2: 提前 ADVANCE_REFRESH_SECONDS 刷新，避免边界过期
                expireTime = System.currentTimeMillis() + (long)(expiresIn - ADVANCE_REFRESH_SECONDS) * 1000;
                log.info("access_token 刷新成功，有效期 {}s", expiresIn);
            } else {
                // L2: 失败，记录错误并抛出异常
                Object errcode = response != null ? response.get("errcode") : "null";
                Object errmsg = response != null ? response.get("errmsg") : "null";
                log.error("access_token 获取失败: errcode={}, errmsg={}", errcode, errmsg);
                throw new RuntimeException("微信 access_token 获取失败");
            }
        } catch (RuntimeException e) {
            // L2: 如果是已封装的 RuntimeException 直接上抛
            if ("微信 access_token 获取失败".equals(e.getMessage())) {
                throw e;
            }
            // L2: 网络等其他异常
            log.error("access_token 获取异常: ", e);
            throw new RuntimeException("微信 access_token 获取失败");
        }
    }
}
