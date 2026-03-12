package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Activity;
import com.shuran.art.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityMapper activityMapper;

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

    @GetMapping("/{id}")
    public Result<Activity> getActivity(@PathVariable Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity != null) {
            LocalDateTime now = LocalDateTime.now();
            activity.setStarted(activity.getStartTime() == null || activity.getStartTime().isBefore(now) || activity.getStartTime().isEqual(now));
            activity.setEnded(activity.getEndTime() != null && activity.getEndTime().isBefore(now));
        }
        return Result.success(activity);
    }
}
