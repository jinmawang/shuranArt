package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.entity.StudentChild;
import com.shuran.art.entity.StudentWork;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.StudentChildMapper;
import com.shuran.art.mapper.StudentWorkMapper;
import com.shuran.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentWorkService {

    private final StudentChildMapper childMapper;
    private final StudentWorkMapper workMapper;
    private final UserMapper userMapper;
    private final AdminService adminService;

    // === 孩子管理 ===

    public List<StudentChild> getMyChildren(Long userId) {
        return childMapper.selectList(
            new LambdaQueryWrapper<StudentChild>()
                .eq(StudentChild::getUserId, userId)
                .orderByAsc(StudentChild::getCreatedAt)
        );
    }

    public List<StudentChild> getMyApprovedChildren(Long userId) {
        return childMapper.selectList(
            new LambdaQueryWrapper<StudentChild>()
                .eq(StudentChild::getUserId, userId)
                .eq(StudentChild::getStatus, "approved")
                .orderByAsc(StudentChild::getCreatedAt)
        );
    }

    public StudentChild addChild(Long userId, String name, String reason) {
        // 每个用户最多3个孩子
        long count = childMapper.selectCount(
            new LambdaQueryWrapper<StudentChild>()
                .eq(StudentChild::getUserId, userId)
        );
        if (count >= 3) {
            throw new RuntimeException("每个账号最多添加3个孩子");
        }
        StudentChild child = new StudentChild();
        child.setUserId(userId);
        child.setName(name);
        child.setStatus("pending");
        child.setReason(reason);
        childMapper.insert(child);
        return child;
    }

    // === 作品上传 ===

    @Transactional
    public void uploadWork(Long userId, Long childId, String imageUrl, String description) {
        // 验证孩子属于该用户且已审核通过
        StudentChild child = childMapper.selectById(childId);
        if (child == null || !child.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"approved".equals(child.getStatus())) {
            throw new RuntimeException("该孩子尚未审核通过");
        }

        // 检查是否超过30张，超过则删除最早的
        long count = workMapper.selectCount(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getChildId, childId)
        );
        if (count >= 30) {
            StudentWork oldest = workMapper.selectOne(
                new LambdaQueryWrapper<StudentWork>()
                    .eq(StudentWork::getChildId, childId)
                    .orderByAsc(StudentWork::getCreatedAt)
                    .last("LIMIT 1")
            );
            if (oldest != null) {
                workMapper.deleteById(oldest.getId());
            }
        }

        StudentWork work = new StudentWork();
        work.setChildId(childId);
        work.setUserId(userId);
        work.setImageUrl(imageUrl);
        work.setDescription(description);
        work.setStatus("pending");
        workMapper.insert(work);
    }

    // === 作品墙（公开） ===

    public List<Map<String, Object>> getApprovedWorks(int page, int size) {
        List<StudentWork> works = workMapper.selectList(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getStatus, "approved")
                .orderByDesc(StudentWork::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size)
        );
        return enrichWorks(works);
    }

    // === 孩子时间线 ===

    public Map<String, Object> getChildTimeline(Long childId) {
        StudentChild child = childMapper.selectById(childId);
        if (child == null) {
            throw new RuntimeException("学员不存在");
        }
        List<StudentWork> works = workMapper.selectList(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getChildId, childId)
                .eq(StudentWork::getStatus, "approved")
                .orderByDesc(StudentWork::getCreatedAt)
        );
        Map<String, Object> result = new HashMap<>();
        result.put("child", child);
        result.put("works", works);
        result.put("shareText", getShareText());
        return result;
    }

    // === 管理端 ===

    public List<Map<String, Object>> getPendingWorks() {
        List<StudentWork> works = workMapper.selectList(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getStatus, "pending")
                .orderByDesc(StudentWork::getCreatedAt)
        );
        return enrichWorks(works);
    }

    public List<Map<String, Object>> getAllWorks() {
        List<StudentWork> works = workMapper.selectList(
            new LambdaQueryWrapper<StudentWork>()
                .orderByDesc(StudentWork::getCreatedAt)
        );
        return enrichWorks(works);
    }

    public void approveWork(Long workId) {
        StudentWork work = workMapper.selectById(workId);
        if (work == null) throw new RuntimeException("作品不存在");
        work.setStatus("approved");
        workMapper.updateById(work);
    }

    public void approveAndFeatureWork(Long workId) {
        StudentWork work = workMapper.selectById(workId);
        if (work == null) throw new RuntimeException("作品不存在");
        work.setStatus("approved");
        work.setFeatured(true);
        workMapper.updateById(work);
    }

    public List<Map<String, Object>> getFeaturedWorks() {
        List<StudentWork> works = workMapper.selectList(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getStatus, "approved")
                .eq(StudentWork::getFeatured, true)
                .orderByDesc(StudentWork::getCreatedAt)
                .last("LIMIT 10")
        );
        return enrichWorks(works);
    }

    public void unfeatureWork(Long workId) {
        StudentWork work = workMapper.selectById(workId);
        if (work == null) throw new RuntimeException("作品不存在");
        work.setFeatured(false);
        workMapper.updateById(work);
    }

    public void rejectWork(Long workId) {
        StudentWork work = workMapper.selectById(workId);
        if (work == null) throw new RuntimeException("作品不存在");
        work.setStatus("rejected");
        workMapper.updateById(work);
    }

    public void deleteWork(Long workId) {
        workMapper.deleteById(workId);
    }

    // === 孩子审核（管理端） ===

    public List<Map<String, Object>> getPendingChildren() {
        List<StudentChild> children = childMapper.selectList(
            new LambdaQueryWrapper<StudentChild>()
                .eq(StudentChild::getStatus, "pending")
                .orderByDesc(StudentChild::getCreatedAt)
        );
        return enrichChildren(children);
    }

    public List<Map<String, Object>> getAllChildren() {
        List<StudentChild> children = childMapper.selectList(
            new LambdaQueryWrapper<StudentChild>()
                .orderByDesc(StudentChild::getCreatedAt)
        );
        return enrichChildren(children);
    }

    public void approveChild(Long childId) {
        StudentChild child = childMapper.selectById(childId);
        if (child == null) throw new RuntimeException("学员不存在");
        child.setStatus("approved");
        childMapper.updateById(child);
    }

    public void rejectChild(Long childId) {
        StudentChild child = childMapper.selectById(childId);
        if (child == null) throw new RuntimeException("学员不存在");
        child.setStatus("rejected");
        childMapper.updateById(child);
    }

    private List<Map<String, Object>> enrichChildren(List<StudentChild> children) {
        if (children.isEmpty()) return new ArrayList<>();
        Set<Long> userIds = children.stream().map(StudentChild::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentChild c : children) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("status", c.getStatus());
            item.put("reason", c.getReason());
            item.put("createdAt", c.getCreatedAt());
            User user = userMap.get(c.getUserId());
            item.put("parentName", user != null ? user.getNickName() : "未知");
            item.put("parentAvatar", user != null ? user.getAvatarUrl() : "");
            result.add(item);
        }
        return result;
    }

    // === 辅助方法 ===

    private List<Map<String, Object>> enrichWorks(List<StudentWork> works) {
        if (works.isEmpty()) return new ArrayList<>();
        Set<Long> childIds = works.stream().map(StudentWork::getChildId).collect(Collectors.toSet());
        Map<Long, StudentChild> childMap = new HashMap<>();
        if (!childIds.isEmpty()) {
            childMapper.selectBatchIds(childIds).forEach(c -> childMap.put(c.getId(), c));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentWork w : works) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", w.getId());
            item.put("imageUrl", w.getImageUrl());
            item.put("description", w.getDescription());
            item.put("status", w.getStatus());
            item.put("createdAt", w.getCreatedAt());
            StudentChild child = childMap.get(w.getChildId());
            item.put("childId", w.getChildId());
            item.put("childName", child != null ? child.getName() : "未知");
            item.put("childAvatar", child != null ? child.getAvatar() : "");
            result.add(item);
        }
        return result;
    }

    private String getShareText() {
        Map<String, String> config = adminService.getStudioConfig();
        String val = config.get("work_share_text");
        return val != null && !val.isBlank() ? val : "快来看我在书染美术的作品～";
    }
}
