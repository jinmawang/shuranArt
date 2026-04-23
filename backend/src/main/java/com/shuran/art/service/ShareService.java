package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.dto.WxacodeData;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.ShareRecord;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.ShareRecordMapper;
import com.shuran.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordMapper shareRecordMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

    // L2 设计: PST-data-detail.md 7.2 ShareService 新增依赖
    private final WxAccessTokenManager wxAccessTokenManager;
    private final WxacodeCacheManager wxacodeCacheManager;

    /**
     * 创建分享记录（用户发起分享时调用）
     * 返回分享码，用于追踪谁点击了链接
     */
    @Transactional
    public Map<String, Object> createShare(Long sharerId, Long activityId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 检查活动是否存在且有效
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            result.put("success", false);
            result.put("msg", "活动不存在或已结束");
            return result;
        }

        // 2. 检查是否已达到总分享次数上限
        int totalLimit = activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 6;
        int totalShares = shareRecordMapper.countTotalSharesByActivity(sharerId, activityId);

        if (totalShares >= totalLimit) {
            result.put("success", false);
            result.put("msg", "该活动分享次数已达上限（" + totalLimit + "次）");
            return result;
        }

        // 3. 创建分享记录
        ShareRecord record = new ShareRecord();
        record.setSharerId(sharerId);
        record.setActivityId(activityId);
        record.setShareCode(generateShareCode());
        record.setConfirmed(0);
        record.setLotteryGranted(0);
        shareRecordMapper.insert(record);

        result.put("success", true);
        result.put("shareCode", record.getShareCode());
        result.put("shareTitle", activity.getShareTitle() != null ? activity.getShareTitle() : activity.getTitle());
        result.put("shareImage", activity.getShareImage() != null ? activity.getShareImage() : activity.getCoverImg());
        result.put("remainingShares", totalLimit - totalShares - 1);
        return result;
    }

    /**
     * 确认分享（被分享者点击链接时调用）
     * 只有当被分享者点击链接后，分享才算成功
     */
    @Transactional
    public Map<String, Object> confirmShare(Long visitorId, String shareCode) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查找分享记录
        ShareRecord record = shareRecordMapper.selectOne(
            new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getShareCode, shareCode)
        );

        if (record == null) {
            result.put("success", false);
            result.put("msg", "分享链接无效");
            return result;
        }

        // 2. 检查是否已确认
        if (record.getConfirmed() != null && record.getConfirmed() == 1) {
            result.put("success", false);
            result.put("msg", "该链接已被使用");
            return result;
        }

        // 3. 不能给自己助力
        if (record.getSharerId().equals(visitorId)) {
            result.put("success", false);
            result.put("msg", "不能给自己助力");
            return result;
        }

        // 4. 检查访问者是否已经帮该分享者助力过该活动
        ShareRecord existing = shareRecordMapper.selectOne(
            new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getSharerId, record.getSharerId())
                .eq(ShareRecord::getVisitorId, visitorId)
                .eq(ShareRecord::getActivityId, record.getActivityId())
                .eq(ShareRecord::getConfirmed, 1)
        );

        if (existing != null) {
            result.put("success", false);
            result.put("msg", "您已经帮TA助力过了");
            return result;
        }

        // 5. 原子确认分享（防并发重复确认）
        int affected = shareRecordMapper.atomicConfirm(record.getId(), visitorId);
        if (affected == 0) {
            result.put("success", false);
            result.put("msg", "该链接已被使用");
            return result;
        }

        // 6. 检查分享者的已确认分享次数是否已达上限
        Activity activity = activityMapper.selectById(record.getActivityId());
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 6;
        int confirmedCount = shareRecordMapper.countConfirmedShares(record.getSharerId(), record.getActivityId());

        if (confirmedCount < totalLimit) {
            // 发放抽奖机会
            record.setLotteryGranted(1);
            User sharer = userMapper.selectById(record.getSharerId());
            if (sharer != null) {
                sharer.setLotteryChances(sharer.getLotteryChances() + 1);
                userMapper.updateById(sharer);
            }
            // 更新 lottery_granted 字段
            ShareRecord updateRecord = new ShareRecord();
            updateRecord.setId(record.getId());
            updateRecord.setLotteryGranted(1);
            shareRecordMapper.updateById(updateRecord);
            result.put("lotteryAdded", true);
            result.put("msg", "助力成功！对方获得1次抽奖机会");
        } else {
            // 显式标记未授予抽奖机会
            ShareRecord noLotteryRecord = new ShareRecord();
            noLotteryRecord.setId(record.getId());
            noLotteryRecord.setLotteryGranted(0);
            shareRecordMapper.updateById(noLotteryRecord);
            result.put("lotteryAdded", false);
            result.put("msg", "助力成功！但对方抽奖机会已达上限");
        }

        result.put("success", true);
        return result;
    }

    /**
     * 旧的分享记录方法（兼容旧接口）
     */
    @Transactional
    public Map<String, Object> recordShare(Long visitorId, ShareRequest request) {
        // 如果有 shareCode，使用新的确认逻辑
        if (request.getShareCode() != null && !request.getShareCode().isEmpty()) {
            return confirmShare(visitorId, request.getShareCode());
        }

        // 否则使用旧逻辑（兼容）
        Map<String, Object> result = new HashMap<>();
        Long sharerId = request.getSharerId();
        Long activityId = request.getActivityId();

        if (sharerId.equals(visitorId)) {
            result.put("success", false);
            result.put("msg", "不能给自己助力");
            return result;
        }

        // 检查是否已记录过
        ShareRecord existing = shareRecordMapper.selectOne(
            new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getSharerId, sharerId)
                .eq(ShareRecord::getVisitorId, visitorId)
                .eq(ShareRecord::getActivityId, activityId)
                .eq(ShareRecord::getConfirmed, 1)
        );

        if (existing != null) {
            result.put("success", false);
            result.put("msg", "已助力过");
            return result;
        }

        Activity activity = activityMapper.selectById(activityId);
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 6;
        int confirmedCount = shareRecordMapper.countConfirmedShares(sharerId, activityId);

        ShareRecord record = new ShareRecord();
        record.setSharerId(sharerId);
        record.setVisitorId(visitorId);
        record.setActivityId(activityId);
        record.setShareCode(generateShareCode());
        record.setConfirmed(1);
        record.setConfirmedAt(LocalDateTime.now());

        if (confirmedCount >= totalLimit) {
            record.setLotteryGranted(0);
            shareRecordMapper.insert(record);
            result.put("success", true);
            result.put("msg", "助力成功！但对方抽奖机会已达上限");
            result.put("lotteryAdded", false);
            return result;
        }

        record.setLotteryGranted(1);
        shareRecordMapper.insert(record);

        User sharer = userMapper.selectById(sharerId);
        sharer.setLotteryChances(sharer.getLotteryChances() + 1);
        userMapper.updateById(sharer);

        result.put("success", true);
        result.put("msg", "助力成功！对方获得1次抽奖机会");
        result.put("lotteryAdded", true);
        return result;
    }

    /**
     * 获取用户在某活动的分享状态
     */
    public Map<String, Object> getShareStatus(Long userId, Long activityId) {
        Map<String, Object> result = new HashMap<>();

        Activity activity = activityMapper.selectById(activityId);
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 6;

        int totalShares = shareRecordMapper.countTotalSharesByActivity(userId, activityId);
        int confirmedShares = shareRecordMapper.countConfirmedShares(userId, activityId);

        result.put("totalLimit", totalLimit);
        result.put("totalShares", totalShares);
        result.put("confirmedShares", confirmedShares);
        result.put("remainingShares", Math.max(0, totalLimit - totalShares));
        result.put("canShare", totalShares < totalLimit);

        return result;
    }

    /**
     * 生成活动小程序码
     * L2 设计: PST-api-detail.md 2.2 EP-01 业务逻辑 STEP-01 ~ STEP-08
     * 需求来源: REQ-PST-009 ~ REQ-PST-011, REQ-PST-022 ~ REQ-PST-023
     *
     * 流程: 缓存检查 -> token 获取 -> 微信 API -> Base64 编码 -> 缓存写入
     */
    @SuppressWarnings("unchecked")
    public WxacodeData getWxacode(Long userId, Long activityId) {
        // STEP-02: 验证活动存在（L2: 不检查活动状态，BS-015/BS-016）
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return null; // Controller 层判断 null 返回 Result.error("活动不存在")
        }

        // STEP-03: 检查缓存（L2: cacheKey = userId + "_" + activityId）
        String cacheKey = userId + "_" + activityId;
        String cachedBase64 = wxacodeCacheManager.get(cacheKey);
        if (cachedBase64 != null) {
            // 缓存命中，直接返回（响应时间 < 200ms，REQ-PST-022）
            return new WxacodeData(cachedBase64);
        }

        // STEP-04: 获取 access_token
        String accessToken = wxAccessTokenManager.getAccessToken();

        // STEP-05: 构造 scene 参数（L2: 缩写格式 BS-013）
        String scene = "s=" + userId + "&a=" + activityId;
        // L2: 极端情况检查，scene 不超过 32 字节
        if (scene.getBytes(StandardCharsets.UTF_8).length > 32) {
            // L2: 降级处理，仅携带 activityId
            scene = "a=" + activityId;
        }

        // STEP-06: 调用微信 wxacode.getUnlimited API（含 STEP-07-RETRY 重试逻辑）
        byte[] imageBytes = callWxacodeApi(accessToken, scene, false);

        // STEP-07: Base64 编码并缓存
        String base64String = Base64.getEncoder().encodeToString(imageBytes);
        wxacodeCacheManager.put(cacheKey, base64String);

        // STEP-08: 返回结果
        return new WxacodeData(base64String);
    }

    /**
     * 调用微信 wxacode.getUnlimited API
     * L2 设计: PST-api-detail.md STEP-06 + STEP-07-RETRY
     * L2 设计: PST-data-detail.md 4.1 请求映射
     *
     * @param accessToken  微信 access_token
     * @param scene        scene 参数（如 "s=5&a=1"）
     * @param alreadyRetried 是否已重试过（最多重试 1 次）
     * @return 小程序码图片二进制数据
     */
    @SuppressWarnings("unchecked")
    private byte[] callWxacodeApi(String accessToken, String scene, boolean alreadyRetried) {
        // L2 设计: POST URL（PST-data-detail.md 4.1）
        String wxApiUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        // L2 设计: 请求体构造（PST-api-detail.md STEP-06）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("scene", scene);
        requestBody.put("page", "pages/activity/activity");    // REQ-PST-010
        requestBody.put("width", 280);                         // L2: 280px 源尺寸保证清晰度
        requestBody.put("auto_color", false);
        // L2: line_color RGB(99,102,241) = #6366F1（主色一致）
        Map<String, Integer> lineColor = new HashMap<>();
        lineColor.put("r", 99);
        lineColor.put("g", 102);
        lineColor.put("b", 241);
        requestBody.put("line_color", lineColor);

        try {
            // 参照 UserService RestTemplate 创建模式
            RestTemplate restTemplate = new RestTemplate();
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")
            ));
            restTemplate.getMessageConverters().add(0, converter);

            // L2: 微信 API 成功返回图片二进制，失败返回 JSON
            // 使用 byte[] 接收响应，然后判断内容类型
            byte[] responseBytes = restTemplate.postForObject(wxApiUrl, requestBody, byte[].class);

            if (responseBytes == null || responseBytes.length == 0) {
                throw new RuntimeException("小程序码生成失败，请稍后重试");
            }

            // L2: 判断返回内容是否为 JSON 错误响应
            // 微信 API 成功时返回图片二进制（以 PNG/JPEG 魔术字节开头）
            // 失败时返回 JSON 文本（以 '{' 开头）
            if (responseBytes[0] == '{') {
                // L2: 返回了 JSON 错误
                String errorJson = new String(responseBytes, StandardCharsets.UTF_8);
                log.warn("微信 wxacode API 返回错误: {}", errorJson);

                // 解析 errcode
                try {
                    RestTemplate jsonParser = new RestTemplate();
                    // 简单解析：从 JSON 字符串提取 errcode
                    Map<String, Object> errorMap = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(errorJson, Map.class);
                    Object errcodeObj = errorMap.get("errcode");
                    int errcode = errcodeObj != null ? ((Number) errcodeObj).intValue() : 0;
                    String errmsg = (String) errorMap.get("errmsg");

                    log.warn("微信 wxacode API 调用失败: errcode={}, errmsg={}", errcode, errmsg);

                    // L2 STEP-07-RETRY: errcode=40001 时刷新 token 重试
                    if (errcode == 40001 && !alreadyRetried) {
                        wxAccessTokenManager.refreshAccessToken();
                        String newToken = wxAccessTokenManager.getAccessToken();
                        return callWxacodeApi(newToken, scene, true);
                    }
                } catch (Exception parseEx) {
                    log.error("解析微信错误响应失败", parseEx);
                }

                throw new RuntimeException("小程序码生成失败，请稍后重试");
            }

            // L2: 成功，返回图片二进制数据
            return responseBytes;

        } catch (RuntimeException e) {
            // L2: 如果是已封装的业务异常直接上抛
            if ("小程序码生成失败，请稍后重试".equals(e.getMessage())) {
                throw e;
            }
            // L2 STEP-07-RETRY: 网络异常，重试 1 次
            if (!alreadyRetried) {
                log.warn("微信 wxacode API 网络异常，重试中: {}", e.getMessage());
                return callWxacodeApi(accessToken, scene, true);
            }
            log.error("微信 wxacode API 重试仍失败", e);
            throw new RuntimeException("小程序码生成失败，请稍后重试");
        }
    }

    private String generateShareCode() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
