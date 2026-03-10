package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.LotteryResponse;
import com.shuran.art.entity.LotteryRecord;
import com.shuran.art.entity.Prize;
import com.shuran.art.entity.User;
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

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final UserMapper userMapper;
    private final PrizeMapper prizeMapper;
    private final LotteryRecordMapper lotteryRecordMapper;
    private final Random random = new Random();

    @Transactional
    public LotteryResponse draw(Long userId) {
        // 1. 检查抽奖机会
        User user = userMapper.selectById(userId);
        if (user == null || user.getLotteryChances() <= 0) {
            throw new RuntimeException("没有抽奖机会");
        }

        // 2. 扣减抽奖机会
        user.setLotteryChances(user.getLotteryChances() - 1);
        userMapper.updateById(user);

        // 3. 获取奖品池
        List<Prize> prizes = prizeMapper.selectList(
            new LambdaQueryWrapper<Prize>().eq(Prize::getStatus, 1)
        );

        // 4. 按概率抽奖
        Prize selectedPrize = selectPrize(prizes);

        // 5. 检查并扣减库存
        if (selectedPrize.getStock() != -1) {
            if (selectedPrize.getStock() <= 0) {
                // 库存不足，降级到积分奖品
                selectedPrize = prizes.stream()
                    .filter(p -> "points".equals(p.getType()))
                    .findFirst()
                    .orElse(prizes.get(0));
            } else {
                selectedPrize.setStock(selectedPrize.getStock() - 1);
                prizeMapper.updateById(selectedPrize);
            }
        }

        // 6. 创建抽奖记录
        LotteryRecord record = new LotteryRecord();
        record.setUserId(userId);
        record.setPrizeId(selectedPrize.getId());
        record.setPrizeName(selectedPrize.getName());
        record.setPrizeType(selectedPrize.getType());
        record.setPrizeValue(selectedPrize.getValue());

        boolean needClaim = selectedPrize.getNeedClaim() == 1;
        if (needClaim) {
            record.setStatus("pending");
            record.setClaimCode(CodeGenerator.generateClaimCode());
            record.setExpireAt(LocalDateTime.now().plusDays(30));
        } else {
            record.setStatus("claimed");
            record.setClaimedAt(LocalDateTime.now());
        }

        lotteryRecordMapper.insert(record);

        // 7. 如果是积分，直接发放
        if ("points".equals(selectedPrize.getType())) {
            user.setPoints(user.getPoints() + selectedPrize.getValue());
            userMapper.updateById(user);
        }

        return LotteryResponse.builder()
                .prizeId(selectedPrize.getId())
                .prizeName(selectedPrize.getName())
                .prizeType(selectedPrize.getType())
                .prizeValue(selectedPrize.getValue())
                .icon(selectedPrize.getIcon())
                .needClaim(needClaim)
                .claimCode(record.getClaimCode())
                .build();
    }

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
}
