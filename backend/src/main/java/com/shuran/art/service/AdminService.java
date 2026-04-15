package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.entity.*;
import com.shuran.art.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminWhitelistMapper adminWhitelistMapper;
    private final StudioConfigMapper studioConfigMapper;
    private final TeacherMapper teacherMapper;
    private final ActivityMapper activityMapper;
    private final PrizeMapper prizeMapper;
    private final CourseMapper courseMapper;
    private final BannerMapper bannerMapper;

    public boolean isAdmin(String openid) {
        return adminWhitelistMapper.selectCount(
            new LambdaQueryWrapper<AdminWhitelist>().eq(AdminWhitelist::getOpenid, openid)
        ) > 0;
    }

    // 画室配置
    public Map<String, String> getStudioConfig() {
        List<StudioConfig> configs = studioConfigMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (StudioConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    public void updateStudioConfig(String key, String value) {
        StudioConfig config = studioConfigMapper.selectOne(
            new LambdaQueryWrapper<StudioConfig>().eq(StudioConfig::getConfigKey, key)
        );
        if (config != null) {
            config.setConfigValue(value);
            studioConfigMapper.updateById(config);
        } else {
            config = new StudioConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("text");
            studioConfigMapper.insert(config);
        }
    }

    // 老师管理
    public List<Teacher> getTeachers() {
        return teacherMapper.selectList(
            new LambdaQueryWrapper<Teacher>().orderByAsc(Teacher::getSortOrder)
        );
    }

    public void saveTeacher(Teacher teacher) {
        if (teacher.getId() == null) {
            teacherMapper.insert(teacher);
        } else {
            teacherMapper.updateById(teacher);
        }
    }

    public void deleteTeacher(Long id) {
        teacherMapper.deleteById(id);
    }

    // 活动管理
    public List<Activity> getActivities() {
        return activityMapper.selectList(
            new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getCreatedAt)
        );
    }

    public void saveActivity(Activity activity) {
        if (activity.getId() == null) {
            activityMapper.insert(activity);
        } else {
            activityMapper.updateById(activity);
        }
    }

    public void deleteActivity(Long id) {
        activityMapper.deleteById(id);
    }

    // 奖品管理
    public List<Prize> getPrizes() {
        return prizeMapper.selectList(null);
    }

    public void savePrize(Prize prize) {
        if (prize.getId() == null) {
            prizeMapper.insert(prize);
        } else {
            prizeMapper.updateById(prize);
        }
    }

    public void deletePrize(Long id) {
        prizeMapper.deleteById(id);
    }

    // 课程管理 (CRS: EP-03 ~ EP-06, EARS-CRS-005, AC-CRS-004)

    // EP-03 STEP-01: 查询所有课程（含已下架），按 sortOrder 升序 (RM-003)
    public List<Course> getCourses() {
        return courseMapper.selectList(
            new LambdaQueryWrapper<Course>().orderByAsc(Course::getSortOrder)
        );
    }

    // EP-04 STEP-01~03: 新增或编辑课程 (RM-004, RM-005)
    // 验证逻辑: V-003 ~ V-012, CV-001 ~ CV-006
    public void saveCourse(Course course) {
        if (course.getName() == null || course.getName().isBlank()) {
            throw new RuntimeException("课程名称不能为空");
        }

        if (course.getCategory() == null || course.getCategory().isBlank()) {
            throw new RuntimeException("课程类别不能为空");
        }

        if (course.getSuitableFor() == null || course.getSuitableFor().isBlank()) {
            throw new RuntimeException("适合人群不能为空");
        }

        // status 校验（0 或 1）
        if (course.getStatus() != null && course.getStatus() != 0 && course.getStatus() != 1) {
            throw new RuntimeException("状态值无效");
        }

        // V-011: sortOrder 默认 0
        if (course.getSortOrder() == null) {
            course.setSortOrder(0);
        }

        // V-012: status 新增默认 1
        if (course.getStatus() == null) {
            course.setStatus(1);
        }

        // STEP-02/03: 判断新增/编辑 (V-003)
        if (course.getId() == null) {
            courseMapper.insert(course);
        } else {
            courseMapper.updateById(course);
        }
    }

    // EP-05 STEP-01: 删除课程（物理删除，幂等处理）(RM-006)
    public void deleteCourse(Long id) {
        courseMapper.deleteById(id);
    }

    // EP-06 STEP-01~02: 课程上下架切换 (RM-005)
    // 验证逻辑: V-014, V-015, CV-006, CV-007
    public void updateCourseStatus(Long id, Integer status) {
        // V-015: status 不能为空
        if (status == null) {
            throw new RuntimeException("状态值不能为空");
        }

        // V-015 / CV-006: status 必须为 0 或 1
        if (status != 0 && status != 1) {
            throw new RuntimeException("状态值无效");
        }

        // STEP-01: 验证课程存在 (CV-007)
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        // STEP-02: 更新状态
        course.setStatus(status);
        courseMapper.updateById(course);
    }

    // 轮播图管理

    public List<Banner> getBanners() {
        return bannerMapper.selectList(
            new LambdaQueryWrapper<Banner>().orderByAsc(Banner::getSortOrder)
        );
    }

    public void saveBanner(Banner banner) {
        if (banner.getImageUrl() == null || banner.getImageUrl().isBlank()) {
            throw new RuntimeException("图片不能为空");
        }
        if (banner.getDescription() != null && banner.getDescription().length() > 20) {
            throw new RuntimeException("描述不能超过20个字");
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getId() == null) {
            long count = bannerMapper.selectCount(null);
            if (count >= 5) {
                throw new RuntimeException("最多只能添加5张轮播图");
            }
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
    }

    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }
}
