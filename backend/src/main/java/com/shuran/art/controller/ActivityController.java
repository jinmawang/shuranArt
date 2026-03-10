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
        List<Activity> activities = activityMapper.selectList(
            new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, 1)
                .le(Activity::getStartTime, now)
                .ge(Activity::getEndTime, now)
                .orderByDesc(Activity::getCreatedAt)
        );
        return Result.success(activities);
    }

    @GetMapping("/{id}")
    public Result<Activity> getActivity(@PathVariable Long id) {
        Activity activity = activityMapper.selectById(id);
        return Result.success(activity);
    }
}
