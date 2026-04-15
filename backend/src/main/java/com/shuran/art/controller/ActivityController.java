package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.ActivityVisit;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.ActivityMapper;
import com.shuran.art.mapper.ActivityVisitMapper;
import com.shuran.art.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityMapper activityMapper;
    private final ActivityVisitMapper activityVisitMapper;
    private final UserMapper userMapper;

    @GetMapping("/list")
    public Result<List<Activity>> getActivities() {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有有效活动（未结束的，包括未开始的和进行中的）
        List<Activity> activities = activityMapper.selectList(
            new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 1)
                .ge(Activity::getEndTime, now)  // 结束时间 >= 当前时间（未结束）
                .orderByAsc(Activity::getStartTime)
        );

        // 设置活动状态标志
        for (Activity activity : activities) {
            activity.setStarted(activity.getStartTime() == null || activity.getStartTime().isBefore(now) || activity.getStartTime().isEqual(now));
            activity.setEnded(activity.getEndTime() != null && activity.getEndTime().isBefore(now));
        }

        return Result.success(activities);
    }

    @GetMapping("/detail")
    public Result<Activity> getActivity(@RequestParam Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity != null) {
            LocalDateTime now = LocalDateTime.now();
            activity.setStarted(activity.getStartTime() == null || activity.getStartTime().isBefore(now) || activity.getStartTime().isEqual(now));
            activity.setEnded(activity.getEndTime() != null && activity.getEndTime().isBefore(now));
        }
        return Result.success(activity);
    }

    /**
     * 记录用户访问活动，首次访问自动获得1次抽奖机会
     */
    @PostMapping("/visit")
    public Result<Map<String, Object>> visitActivity(
            HttpServletRequest httpRequest,
            @RequestBody Map<String, Long> request) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long activityId = request.get("activityId");

        Map<String, Object> result = new HashMap<>();

        if (activityId == null) {
            result.put("success", false);
            result.put("msg", "活动ID不能为空");
            return Result.success(result);
        }

        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            result.put("success", false);
            result.put("msg", "活动不存在");
            return Result.success(result);
        }

        // 检查是否已访问过
        ActivityVisit existingVisit = activityVisitMapper.selectOne(
            new LambdaQueryWrapper<ActivityVisit>()
                .eq(ActivityVisit::getUserId, userId)
                .eq(ActivityVisit::getActivityId, activityId)
        );

        if (existingVisit != null) {
            result.put("success", true);
            result.put("lotteryAdded", false);
            result.put("msg", "已访问过该活动");
            return Result.success(result);
        }

        // 记录首次访问
        ActivityVisit visit = new ActivityVisit();
        visit.setUserId(userId);
        visit.setActivityId(activityId);
        visit.setLotteryGranted(1);
        activityVisitMapper.insert(visit);

        // 增加抽奖机会
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLotteryChances(user.getLotteryChances() + 1);
            userMapper.updateById(user);
        }

        result.put("success", true);
        result.put("lotteryAdded", true);
        result.put("msg", "获得1次抽奖机会");
        return Result.success(result);
    }
}
