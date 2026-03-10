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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordMapper shareRecordMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;

    @Transactional
    public Map<String, Object> recordShare(Long visitorId, ShareRequest request) {
        Map<String, Object> result = new HashMap<>();
        Long sharerId = request.getSharerId();
        Long activityId = request.getActivityId();

        // 1. 不能给自己加抽奖机会
        if (sharerId.equals(visitorId)) {
            result.put("success", false);
            result.put("msg", "不能给自己助力");
            return result;
        }

        // 2. 检查是否已记录过
        ShareRecord existing = shareRecordMapper.selectOne(
            new LambdaQueryWrapper<ShareRecord>()
                .eq(ShareRecord::getSharerId, sharerId)
                .eq(ShareRecord::getVisitorId, visitorId)
                .eq(ShareRecord::getActivityId, activityId)
        );

        if (existing != null) {
            result.put("success", false);
            result.put("msg", "已助力过");
            return result;
        }

        // 3. 检查今日是否达到上限
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int todayCount = shareRecordMapper.countTodayShares(sharerId, todayStart);

        Activity activity = activityMapper.selectById(activityId);
        int dailyLimit = activity != null ? activity.getDailyShareLimit() : 5;

        ShareRecord record = new ShareRecord();
        record.setSharerId(sharerId);
        record.setVisitorId(visitorId);
        record.setActivityId(activityId);

        if (todayCount >= dailyLimit) {
            record.setLotteryGranted(0);
            shareRecordMapper.insert(record);
            result.put("success", true);
            result.put("msg", "今日抽奖机会已达上限");
            result.put("lotteryAdded", false);
            return result;
        }

        // 4. 发放抽奖机会
        record.setLotteryGranted(1);
        shareRecordMapper.insert(record);

        User sharer = userMapper.selectById(sharerId);
        sharer.setLotteryChances(sharer.getLotteryChances() + 1);
        userMapper.updateById(sharer);

        result.put("success", true);
        result.put("msg", "获得1次抽奖机会");
        result.put("lotteryAdded", true);
        return result;
    }
}
