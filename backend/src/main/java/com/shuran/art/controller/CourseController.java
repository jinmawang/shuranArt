package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Course;
import com.shuran.art.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CRS 公开端点 Controller
// 参照 TeacherController.java 模式（L2 CRS-api-detail.md Section 2, 3）
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;

    // EP-01: GET /api/course/list -- 获取课程列表
    // 需求来源: EARS-CRS-001, EARS-CRS-002, EARS-CRS-004, AC-CRS-001, AC-CRS-002
    @GetMapping("/list")
    public Result<List<Course>> getCourses(@RequestParam(required = false) String category) {
        // V-001: category 为可选参数，空字符串视为不传
        if (category != null && category.isBlank()) {
            category = null;
        }

        // STEP-01: 构建查询条件 -- 仅上架课程，按 sort_order 升序
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 1)
                .orderByAsc(Course::getSortOrder);

        // STEP-02: 应用分类筛选（条件）
        if (category != null) {
            queryWrapper.eq(Course::getCategory, category);
        }

        // STEP-03: 执行查询 (RM-001)
        List<Course> courses = courseMapper.selectList(queryWrapper);

        // STEP-04: 返回结果（空时返回空数组 []）
        return Result.success(courses);
    }

    // EP-02: GET /api/course/{id} -- 获取课程详情
    // 需求来源: EARS-CRS-003, AC-CRS-003
    @GetMapping("/{id}")
    public Result<Course> getCourse(@PathVariable Long id) {
        // V-002: id 为 Long 类型，Spring 自动转换，非法格式由框架返回 400
        // STEP-01: 根据 ID 查询课程 (RM-002)
        Course course = courseMapper.selectById(id);

        // STEP-02: 返回结果（course 为 null 时 data=null）
        // 不额外过滤 status，已下架课程仍可通过 ID 访问
        return Result.success(course);
    }
}
