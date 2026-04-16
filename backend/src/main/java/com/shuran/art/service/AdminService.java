package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.entity.*;
import com.shuran.art.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminWhitelistMapper adminWhitelistMapper;
    private final AdminInviteMapper adminInviteMapper;
    private final UserMapper userMapper;
    private final LotteryRecordMapper lotteryRecordMapper;
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

    // 管理员管理

    public List<Map<String, Object>> getAdminList() {
        List<AdminWhitelist> admins = adminWhitelistMapper.selectList(
            new LambdaQueryWrapper<AdminWhitelist>().orderByAsc(AdminWhitelist::getId)
        );
        List<String> openids = admins.stream().map(AdminWhitelist::getOpenid).collect(Collectors.toList());
        Map<String, User> userMap = new HashMap<>();
        if (!openids.isEmpty()) {
            List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getOpenid, openids)
            );
            for (User u : users) {
                userMap.put(u.getOpenid(), u);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminWhitelist admin : admins) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", admin.getId());
            item.put("openid", admin.getOpenid());
            item.put("isSuper", Boolean.TRUE.equals(admin.getIsSuper()));
            item.put("createdAt", admin.getCreatedAt());
            User user = userMap.get(admin.getOpenid());
            if (user != null) {
                item.put("nickName", user.getNickName());
                item.put("avatarUrl", user.getAvatarUrl());
            } else {
                item.put("nickName", admin.getName() != null ? admin.getName() : "未知用户");
                item.put("avatarUrl", "");
            }
            result.add(item);
        }
        return result;
    }

    public String generateInviteToken(String inviterOpenid) {
        String token = UUID.randomUUID().toString().replace("-", "");
        AdminInvite invite = new AdminInvite();
        invite.setToken(token);
        invite.setInviterOpenid(inviterOpenid);
        invite.setExpiresAt(LocalDateTime.now().plusHours(24));
        invite.setUsed(false);
        adminInviteMapper.insert(invite);
        return token;
    }

    @Transactional
    public void acceptInvite(String token, String openid, String nickName) {
        AdminInvite invite = adminInviteMapper.selectOne(
            new LambdaQueryWrapper<AdminInvite>().eq(AdminInvite::getToken, token)
        );
        if (invite == null) {
            throw new RuntimeException("邀请链接无效");
        }
        if (Boolean.TRUE.equals(invite.getUsed())) {
            throw new RuntimeException("该邀请链接已被使用");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("邀请链接已过期");
        }
        if (isAdmin(openid)) {
            throw new RuntimeException("您已经是管理员");
        }
        // 加入白名单
        AdminWhitelist admin = new AdminWhitelist();
        admin.setOpenid(openid);
        admin.setName(nickName);
        admin.setIsSuper(false);
        adminWhitelistMapper.insert(admin);
        // 标记token已使用
        invite.setUsed(true);
        invite.setUsedByOpenid(openid);
        adminInviteMapper.updateById(invite);
    }

    public void deleteAdmin(Long id, String operatorOpenid) {
        // 仅超级管理员可删除其他管理员
        AdminWhitelist operator = adminWhitelistMapper.selectOne(
            new LambdaQueryWrapper<AdminWhitelist>().eq(AdminWhitelist::getOpenid, operatorOpenid)
        );
        if (operator == null || !Boolean.TRUE.equals(operator.getIsSuper())) {
            throw new RuntimeException("仅超级管理员可执行此操作");
        }
        AdminWhitelist admin = adminWhitelistMapper.selectById(id);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        if (Boolean.TRUE.equals(admin.getIsSuper())) {
            throw new RuntimeException("无法删除超级管理员");
        }
        adminWhitelistMapper.deleteById(id);
    }

    // 奖品核销

    public List<Map<String, Object>> getPendingLotteryRecords() {
        List<LotteryRecord> records = lotteryRecordMapper.selectList(
            new LambdaQueryWrapper<LotteryRecord>()
                .eq(LotteryRecord::getStatus, "pending")
                .gt(LotteryRecord::getExpireAt, LocalDateTime.now())
                .orderByDesc(LotteryRecord::getCreatedAt)
        );
        // 关联用户信息
        Set<Long> userIds = records.stream().map(LotteryRecord::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (LotteryRecord r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("prizeName", r.getPrizeName());
            item.put("prizeType", r.getPrizeType());
            item.put("prizeLevel", r.getPrizeLevel());
            item.put("prizeValue", r.getPrizeValue());
            item.put("claimCode", r.getClaimCode());
            item.put("createdAt", r.getCreatedAt());
            item.put("expireAt", r.getExpireAt());
            User user = userMap.get(r.getUserId());
            if (user != null) {
                item.put("nickName", user.getNickName());
                item.put("avatarUrl", user.getAvatarUrl());
            } else {
                item.put("nickName", "未知用户");
                item.put("avatarUrl", "");
            }
            result.add(item);
        }
        return result;
    }

    public void claimLotteryRecord(Long recordId) {
        LotteryRecord record = lotteryRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("记录不存在");
        }
        if ("claimed".equals(record.getStatus())) {
            throw new RuntimeException("该奖品已核销");
        }
        if (record.getExpireAt() != null && record.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("该奖品已过期");
        }
        record.setStatus("claimed");
        record.setClaimedAt(LocalDateTime.now());
        lotteryRecordMapper.updateById(record);
    }
}
