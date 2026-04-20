package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.Activity;
import com.shuran.art.entity.Banner;
import com.shuran.art.entity.Course;
import com.shuran.art.entity.Prize;
import com.shuran.art.entity.Teacher;
import com.shuran.art.entity.User;
import com.shuran.art.service.AdminService;
import com.shuran.art.service.StudentWorkService;
import com.shuran.art.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StudentWorkService studentWorkService;
    private final UserService userService;

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

    // 课程管理 (CRS: EARS-CRS-005, AC-CRS-004)

    // EP-03: GET /api/admin/courses -- 管理员获取全部课程（含已下架）
    @GetMapping("/courses")
    public Result<List<Course>> getCourses() {
        return Result.success(adminService.getCourses());
    }

    // EP-04: POST /api/admin/course -- 新增或编辑课程
    @PostMapping("/course")
    public Result<Void> saveCourse(@RequestBody Course course) {
        adminService.saveCourse(course);
        return Result.success();
    }

    // EP-05: DELETE /api/admin/course/{id} -- 删除课程
    @DeleteMapping("/course/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        adminService.deleteCourse(id);
        return Result.success();
    }

    // EP-06: PUT /api/admin/course/{id}/status -- 课程上下架切换
    @PutMapping("/course/{id}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        adminService.updateCourseStatus(id, status);
        return Result.success();
    }

    // 轮播图管理
    @GetMapping("/banners")
    public Result<List<Banner>> getBanners() {
        return Result.success(adminService.getBanners());
    }

    @PostMapping("/banner")
    public Result<Void> saveBanner(@RequestBody Banner banner) {
        adminService.saveBanner(banner);
        return Result.success();
    }

    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        adminService.deleteBanner(id);
        return Result.success();
    }

    // 管理员管理
    @GetMapping("/admins")
    public Result<List<Map<String, Object>>> getAdmins() {
        return Result.success(adminService.getAdminList());
    }

    @PostMapping("/invite")
    public Result<Map<String, String>> generateInvite(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        String token = adminService.generateInviteToken(user.getOpenid());
        return Result.success(Map.of("token", token));
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        adminService.deleteAdmin(id, user.getOpenid());
        return Result.success();
    }

    // 奖品核销
    @GetMapping("/lottery-records")
    public Result<List<Map<String, Object>>> getPendingLotteryRecords() {
        return Result.success(adminService.getPendingLotteryRecords());
    }

    @PostMapping("/lottery-record/{id}/claim")
    public Result<Void> claimLotteryRecord(@PathVariable Long id) {
        adminService.claimLotteryRecord(id);
        return Result.success();
    }

    @PostMapping("/lottery-record/{id}/void")
    public Result<Void> voidLotteryRecord(@PathVariable Long id) {
        adminService.voidLotteryRecord(id);
        return Result.success();
    }

    @GetMapping("/points-summary")
    public Result<List<Map<String, Object>>> getPointsSummary() {
        return Result.success(adminService.getPointsSummary());
    }

    @PostMapping("/points-exchange")
    public Result<Void> exchangePoints(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        adminService.exchangePoints(userId);
        return Result.success();
    }

    // 学生作品管理
    @GetMapping("/works/pending")
    public Result<List<Map<String, Object>>> getPendingWorks() {
        return Result.success(studentWorkService.getPendingWorks());
    }

    @GetMapping("/works/all")
    public Result<List<Map<String, Object>>> getAllWorks() {
        return Result.success(studentWorkService.getAllWorks());
    }

    @PostMapping("/works/{id}/approve")
    public Result<Void> approveWork(@PathVariable Long id) {
        studentWorkService.approveWork(id);
        return Result.success();
    }

    @PostMapping("/works/{id}/feature")
    public Result<Void> approveAndFeatureWork(@PathVariable Long id) {
        studentWorkService.approveAndFeatureWork(id);
        return Result.success();
    }

    @PostMapping("/works/{id}/unfeature")
    public Result<Void> unfeatureWork(@PathVariable Long id) {
        studentWorkService.unfeatureWork(id);
        return Result.success();
    }

    @GetMapping("/works/featured")
    public Result<List<Map<String, Object>>> getAdminFeaturedWorks() {
        return Result.success(studentWorkService.getFeaturedWorks());
    }

    @PostMapping("/works/{id}/reject")
    public Result<Void> rejectWork(@PathVariable Long id) {
        studentWorkService.rejectWork(id);
        return Result.success();
    }

    @DeleteMapping("/works/{id}")
    public Result<Void> deleteWork(@PathVariable Long id) {
        studentWorkService.deleteWork(id);
        return Result.success();
    }

    // 学员审核
    @GetMapping("/children/pending")
    public Result<List<Map<String, Object>>> getPendingChildren() {
        return Result.success(studentWorkService.getPendingChildren());
    }

    @GetMapping("/children/all")
    public Result<List<Map<String, Object>>> getAllChildren() {
        return Result.success(studentWorkService.getAllChildren());
    }

    @PostMapping("/children/{id}/approve")
    public Result<Void> approveChild(@PathVariable Long id) {
        studentWorkService.approveChild(id);
        return Result.success();
    }

    @PostMapping("/children/{id}/reject")
    public Result<Void> rejectChild(@PathVariable Long id) {
        studentWorkService.rejectChild(id);
        return Result.success();
    }
}
