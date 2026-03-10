package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.Prize;
import com.shuran.art.entity.Teacher;
import com.shuran.art.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 画室配置
    @GetMapping("/config")
    public Result<Map<String, String>> getStudioConfig() {
        return Result.success(adminService.getStudioConfig());
    }

    @PostMapping("/config")
    public Result<Void> updateStudioConfig(@RequestBody Map<String, String> config) {
        config.forEach(adminService::updateStudioConfig);
        return Result.success();
    }

    // 老师管理
    @GetMapping("/teachers")
    public Result<List<Teacher>> getTeachers() {
        return Result.success(adminService.getTeachers());
    }

    @PostMapping("/teacher")
    public Result<Void> saveTeacher(@RequestBody Teacher teacher) {
        adminService.saveTeacher(teacher);
        return Result.success();
    }

    @DeleteMapping("/teacher/{id}")
    public Result<Void> deleteTeacher(@PathVariable Long id) {
        adminService.deleteTeacher(id);
        return Result.success();
    }

    // 活动管理
    @GetMapping("/activities")
    public Result<List<Activity>> getActivities() {
        return Result.success(adminService.getActivities());
    }

    @PostMapping("/activity")
    public Result<Void> saveActivity(@RequestBody Activity activity) {
        adminService.saveActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/activity/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        adminService.deleteActivity(id);
        return Result.success();
    }

    // 奖品管理
    @GetMapping("/prizes")
    public Result<List<Prize>> getPrizes() {
        return Result.success(adminService.getPrizes());
    }

    @PostMapping("/prize")
    public Result<Void> savePrize(@RequestBody Prize prize) {
        adminService.savePrize(prize);
        return Result.success();
    }

    @DeleteMapping("/prize/{id}")
    public Result<Void> deletePrize(@PathVariable Long id) {
        adminService.deletePrize(id);
        return Result.success();
    }
}
