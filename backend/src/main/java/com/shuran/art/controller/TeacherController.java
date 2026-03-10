package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Teacher;
import com.shuran.art.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherMapper teacherMapper;

    @GetMapping("/list")
    public Result<List<Teacher>> getTeachers() {
        List<Teacher> teachers = teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getStatus, 1)
                .orderByAsc(Teacher::getSortOrder)
        );
        return Result.success(teachers);
    }
}
