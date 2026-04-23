package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.StudentChild;
import com.shuran.art.service.StudentWorkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class StudentWorkController {

    private final StudentWorkService studentWorkService;

    // 获取我的孩子列表
    @GetMapping("/children")
    public Result<List<StudentChild>> getMyChildren(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(studentWorkService.getMyChildren(userId));
    }

    // 添加孩子（需审核）
    @PostMapping("/child")
    public Result<StudentChild> addChild(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String name = body.get("name");
        String reason = body.get("reason");
        if (name == null || name.isBlank()) {
            return Result.error("请输入孩子姓名");
        }
        return Result.success(studentWorkService.addChild(userId, name.trim(), reason));
    }

    // 上传作品
    @PostMapping("/upload")
    public Result<Void> uploadWork(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (body.get("childId") == null) {
            return Result.error("请选择孩子");
        }
        Long childId = Long.valueOf(body.get("childId").toString());
        String imageUrl = (String) body.get("imageUrl");
        String description = (String) body.get("description");
        if (imageUrl == null || imageUrl.isBlank()) {
            return Result.error("请上传作品图片");
        }
        studentWorkService.uploadWork(userId, childId, imageUrl, description);
        return Result.success();
    }

    // 作品墙（公开，已审核作品）
    @GetMapping("/wall")
    public Result<List<Map<String, Object>>> getWorksWall(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(studentWorkService.getApprovedWorks(page, size));
    }

    // 孩子时间线
    @GetMapping("/timeline/{childId}")
    public Result<Map<String, Object>> getTimeline(@PathVariable Long childId) {
        return Result.success(studentWorkService.getChildTimeline(childId));
    }

    // 精选作品（公开）
    @GetMapping("/featured")
    public Result<List<Map<String, Object>>> getFeaturedWorks() {
        return Result.success(studentWorkService.getFeaturedWorks());
    }
}
