package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.ShareRecord;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.ShareRecordMapper;
import com.shuran.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordMapper shareRecordMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

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
        int totalLimit = activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 5;
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

        // 5. 确认分享
        record.setVisitorId(visitorId);
        record.setConfirmed(1);
        record.setConfirmedAt(LocalDateTime.now());

        // 6. 检查分享者的已确认分享次数是否已达上限
        Activity activity = activityMapper.selectById(record.getActivityId());
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 5;
        int confirmedCount = shareRecordMapper.countConfirmedShares(record.getSharerId(), record.getActivityId());

        if (confirmedCount < totalLimit) {
            // 发放抽奖机会
            record.setLotteryGranted(1);
            User sharer = userMapper.selectById(record.getSharerId());
            sharer.setLotteryChances(sharer.getLotteryChances() + 1);
            userMapper.updateById(sharer);
            result.put("lotteryAdded", true);
            result.put("msg", "助力成功！对方获得1次抽奖机会");
        } else {
            record.setLotteryGranted(0);
            result.put("lotteryAdded", false);
            result.put("msg", "助力成功！但对方抽奖机会已达上限");
        }

        shareRecordMapper.updateById(record);

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
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 5;
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
        int totalLimit = activity != null && activity.getTotalShareLimit() != null ? activity.getTotalShareLimit() : 5;

        int totalShares = shareRecordMapper.countTotalSharesByActivity(userId, activityId);
        int confirmedShares = shareRecordMapper.countConfirmedShares(userId, activityId);

        result.put("totalLimit", totalLimit);
        result.put("totalShares", totalShares);
        result.put("confirmedShares", confirmedShares);
        result.put("remainingShares", Math.max(0, totalLimit - totalShares));
        result.put("canShare", totalShares < totalLimit);

        return result;
    }

    private String generateShareCode() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
