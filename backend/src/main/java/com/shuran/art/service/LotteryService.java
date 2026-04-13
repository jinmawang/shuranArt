package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.LotteryRecordMapper;
import com.shuran.art.mapper.PrizeMapper;
import com.shuran.art.mapper.UserMapper;
import com.shuran.art.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final UserMapper userMapper;
    private final PrizeMapper prizeMapper;
    private final LotteryRecordMapper lotteryRecordMapper;
    private final ActivityMapper activityMapper;
    private final Random random = new Random();

    /**
     * 抽奖（关联活动）
     * @param userId 用户ID
     * @param activityId 活动ID
     */
    @Transactional
    public LotteryResponse draw(Long userId, Long activityId) {
        // 1. 检查用户和抽奖机会
        User user = userMapper.selectById(userId);
        if (user == null || user.getLotteryChances() <= 0) {
            throw new RuntimeException("没有抽奖机会");
        }

        // 2. 检查活动是否有效
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new RuntimeException("活动不存在或已结束");
        }

        // 检查活动时间
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new RuntimeException("活动尚未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new RuntimeException("活动已结束");
        }

        // 2.5. 检查用户在该活动的抽奖次数是否已达上限
        int maxLottery = activity.getMaxLotteryPerUser() != null ? activity.getMaxLotteryPerUser() : 10;
        long userActivityLotteryCount = lotteryRecordMapper.selectCount(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .eq(LotteryRecord::getActivityId, activityId)
        );
        if (userActivityLotteryCount >= maxLottery) {
            throw new RuntimeException("该活动抽奖次数已达上限（" + maxLottery + "次）");
        }

        // 3. 扣减抽奖机会
        user.setLotteryChances(user.getLotteryChances() - 1);
        userMapper.updateById(user);

        // 4. 获取奖品池
        List<Prize> prizes = prizeMapper.selectList(
            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );

        // 5. 判断是否需要保底（每8次抽奖必中一或二等奖，不按活动区分）
        boolean needGuarantee = checkNeedGuarantee(userId);

        // 6. 选择奖品
        Prize selectedPrize;
        if (needGuarantee) {
            // 保底机制：80%概率二等奖，20%概率一等奖
            selectedPrize = selectGuaranteePrize(prizes);
        } else {
            // 正常按概率抽奖
            selectedPrize = selectPrize(prizes);
        }

        // 7. 检查并扣减库存
        if (selectedPrize.getStock() != null && selectedPrize.getStock() != -1) {
            if (selectedPrize.getStock() <= 0) {
                // 库存不足，降级到参与奖（积分）
                selectedPrize = prizes.stream()
                    .filter(p -> "points".equals(p.getType()) && (p.getLevel() == null || p.getLevel() >= 3))
                    .findFirst()
                    .orElse(prizes.get(0));
            } else {
                selectedPrize.setStock(selectedPrize.getStock() - 1);
                prizeMapper.updateById(selectedPrize);
            }
        }

        // 8. 创建抽奖记录
        LotteryRecord record = new LotteryRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setPrizeId(selectedPrize.getId());
        record.setPrizeName(selectedPrize.getName());
        record.setPrizeType(selectedPrize.getType());
        record.setPrizeLevel(selectedPrize.getLevel());
        record.setPrizeValue(selectedPrize.getValue());

        boolean needClaim = selectedPrize.getNeedClaim() != null && selectedPrize.getNeedClaim() == 1;
        if (needClaim) {
            record.setStatus("pending");
            record.setClaimCode(CodeGenerator.generateClaimCode());
            record.setExpireAt(LocalDateTime.now().plusDays(30));
        } else {
            record.setStatus("claimed");
            record.setClaimedAt(LocalDateTime.now());
        }

        lotteryRecordMapper.insert(record);

        // 10. 如果是积分，直接发放
        if ("points".equals(selectedPrize.getType())) {
            user.setPoints(user.getPoints() + selectedPrize.getValue());
            userMapper.updateById(user);
        }

        // 计算距离保底的进度
        int lotteryCountSinceLastWin = calculateLotteryCountSinceLastWin(userId);

        return LotteryResponse.builder()
                .prizeId(selectedPrize.getId())
                .prizeName(selectedPrize.getName())
                .prizeType(selectedPrize.getType())
                .prizeLevel(selectedPrize.getLevel())
                .prizeValue(selectedPrize.getValue())
                .icon(selectedPrize.getIcon())
                .needClaim(needClaim)
                .claimCode(record.getClaimCode())
                .isGuarantee(needGuarantee)
                .lotteryCount(lotteryCountSinceLastWin)
                .maxLottery(8)  // 每8次保底
                .build();
    }

    /**
     * 计算用户自上次中大奖以来的抽奖次数
     */
    private int calculateLotteryCountSinceLastWin(Long userId) {
        LotteryRecord lastTopPrize = lotteryRecordMapper.selectOne(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .le(LotteryRecord::getPrizeLevel, 2)
                .orderByDesc(LotteryRecord::getId)
                .last("LIMIT 1")
        );

        if (lastTopPrize != null) {
            return lotteryRecordMapper.selectCount(
                new LambdaQueryWrapper<LotteryRecord>()
                    .eq(LotteryRecord::getUserId, userId)
                    .gt(LotteryRecord::getId, lastTopPrize.getId())
            ).intValue();
        } else {
            return lotteryRecordMapper.selectCount(
                new LambdaQueryWrapper<LotteryRecord>()
                    .eq(LotteryRecord::getUserId, userId)
            ).intValue();
        }
    }

    /**
     * 检查用户在该活动中是否中过一等奖或二等奖
     */
    private boolean hasWonTopPrize(Long userId, Long activityId) {
        Long count = lotteryRecordMapper.selectCount(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .eq(LotteryRecord::getActivityId, activityId)
                .le(LotteryRecord::getPrizeLevel, 2)  // level <= 2 表示一等奖或二等奖
        );
        return count > 0;
    }

    /**
     * 检查是否需要保底（每8次抽奖必中一或二等奖）
     * 统计用户自上次中大奖以来的抽奖次数
     */
    private boolean checkNeedGuarantee(Long userId) {
        // 获取用户最近一次中大奖的记录
        LotteryRecord lastTopPrize = lotteryRecordMapper.selectOne(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .le(LotteryRecord::getPrizeLevel, 2)
                .orderByDesc(LotteryRecord::getId)
                .last("LIMIT 1")
        );

        // 统计自上次中大奖以来的抽奖次数
        long lotteryCountSinceLastWin;
        if (lastTopPrize != null) {
            lotteryCountSinceLastWin = lotteryRecordMapper.selectCount(
                new LambdaQueryWrapper<LotteryRecord>()
                    .eq(LotteryRecord::getUserId, userId)
                    .gt(LotteryRecord::getId, lastTopPrize.getId())
            );
        } else {
            // 从未中过大奖，统计总抽奖次数
            lotteryCountSinceLastWin = lotteryRecordMapper.selectCount(
                new LambdaQueryWrapper<LotteryRecord>()
                    .eq(LotteryRecord::getUserId, userId)
            );
        }

        // 每8次抽奖必中（即第8次触发保底）
        return lotteryCountSinceLastWin >= 7;
    }

    /**
     * 保底抽奖：80%二等奖，20%一等奖
     */
    private Prize selectGuaranteePrize(List<Prize> prizes) {
        // 获取一等奖和二等奖
        List<Prize> firstPrizes = prizes.stream()
            .filter(p -> p.getLevel() != null && p.getLevel() == 1 && (p.getStock() == null || p.getStock() == -1 || p.getStock() > 0))
            .collect(Collectors.toList());

        List<Prize> secondPrizes = prizes.stream()
            .filter(p -> p.getLevel() != null && p.getLevel() == 2 && (p.getStock() == null || p.getStock() == -1 || p.getStock() > 0))
            .collect(Collectors.toList());

        // 20%概率抽中一等奖
        int randomValue = random.nextInt(100);
        if (randomValue < 20 && !firstPrizes.isEmpty()) {
            return firstPrizes.get(random.nextInt(firstPrizes.size()));
        }

        // 80%概率抽中二等奖
        if (!secondPrizes.isEmpty()) {
            return secondPrizes.get(random.nextInt(secondPrizes.size()));
        }

        // 如果没有一二等奖，返回一等奖（如果有）或第一个奖品
        if (!firstPrizes.isEmpty()) {
            return firstPrizes.get(0);
        }

        return prizes.get(0);
    }

    /**
     * 正常概率抽奖
     */
    private Prize selectPrize(List<Prize> prizes) {
        int randomValue = random.nextInt(100);
        int cumulative = 0;

        for (Prize prize : prizes) {
            cumulative += prize.getProbability();
            if (randomValue < cumulative) {
                return prize;
            }
        }
        return prizes.get(0);
    }

    /**
     * 获取用户的抽奖状态（保底进度）
     */
    public LotteryStatusResponse getLotteryStatus(Long userId, Long activityId) {
        // 计算距离保底的进度
        int currentCount = calculateLotteryCountSinceLastWin(userId);
        int maxLottery = 8;  // 每8次保底

        // 检查用户是否中过大奖
        boolean hasWonTopPrize = currentCount == 0;  // 如果计数为0，说明刚中过大奖

        User user = userMapper.selectById(userId);
        int chances = user != null ? user.getLotteryChances() : 0;

        // 检查该活动的抽奖次数限制
        Activity activity = activityMapper.selectById(activityId);
        int activityMaxLottery = activity != null && activity.getMaxLotteryPerUser() != null ? activity.getMaxLotteryPerUser() : 10;
        long userActivityLotteryCount = lotteryRecordMapper.selectCount(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .eq(LotteryRecord::getActivityId, activityId)
        );
        boolean reachedLimit = userActivityLotteryCount >= activityMaxLottery;

        return new LotteryStatusResponse(
            currentCount,
            maxLottery,
            chances,
            hasWonTopPrize,
            reachedLimit
        );
    }

    public List<Prize> getPrizes() {
        return prizeMapper.selectList(
            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );
    }

    public List<LotteryRecord> getUserRecords(Long userId) {
        return lotteryRecordMapper.selectList(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .orderByDesc(LotteryRecord::getCreatedAt)
        );
    }

    public List<LotteryRecord> getUserRecordsByActivity(Long userId, Long activityId) {
        return lotteryRecordMapper.selectList(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getUserId, userId)
                .eq(LotteryRecord::getActivityId, activityId)
                .orderByDesc(LotteryRecord::getCreatedAt)
        );
    }

    /**
     * 抽奖状态响应类
     */
    public record LotteryStatusResponse(
        int currentCount,
        int maxLottery,
        int chances,
        boolean hasWonTopPrize,
        boolean reachedLimit
    ) {}
}
