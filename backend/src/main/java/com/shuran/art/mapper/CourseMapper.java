package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.Course;
import org.apache.ibatis.annotations.Mapper;

// CRS Mapper: 课程数据访问层
// 参照 TeacherMapper.java 模式（L2 CRS-data-detail.md Section 2.1）
// 全部使用 BaseMapper 内置方法：selectList, selectById, insert, updateById, deleteById
// 对应 RM-001 ~ RM-006
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
