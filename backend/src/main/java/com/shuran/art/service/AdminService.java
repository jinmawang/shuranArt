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
}
