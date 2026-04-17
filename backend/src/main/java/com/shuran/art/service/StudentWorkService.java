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

    public StudentChild addChild(Long userId, String name) {
        StudentChild child = new StudentChild();
        child.setUserId(userId);
        child.setName(name);
        childMapper.insert(child);
        return child;
    }

    // === 作品上传 ===

    @Transactional
    public void uploadWork(Long userId, Long childId, String imageUrl, String description) {
        // 验证孩子属于该用户
        StudentChild child = childMapper.selectById(childId);
        if (child == null || !child.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }

        // 检查上传间隔
        int intervalDays = getUploadIntervalDays();
        StudentWork lastWork = workMapper.selectOne(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getChildId, childId)
                .eq(StudentWork::getUserId, userId)
                .orderByDesc(StudentWork::getCreatedAt)
                .last("LIMIT 1")
        );
        if (lastWork != null && lastWork.getCreatedAt().plusDays(intervalDays).isAfter(LocalDateTime.now())) {
            throw new RuntimeException("每" + intervalDays + "天只能上传一张作品");
        }

        // 检查是否超过24张，超过则删除最早的
        long count = workMapper.selectCount(
            new LambdaQueryWrapper<StudentWork>()
                .eq(StudentWork::getChildId, childId)
        );
        if (count >= 24) {
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

    public void rejectWork(Long workId) {
        StudentWork work = workMapper.selectById(workId);
        if (work == null) throw new RuntimeException("作品不存在");
        work.setStatus("rejected");
        workMapper.updateById(work);
    }

    public void deleteWork(Long workId) {
        workMapper.deleteById(workId);
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

    private int getUploadIntervalDays() {
        Map<String, String> config = adminService.getStudioConfig();
        String val = config.get("work_upload_interval_days");
        try {
            return val != null ? Integer.parseInt(val) : 30;
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    private String getShareText() {
        Map<String, String> config = adminService.getStudioConfig();
        String val = config.get("work_share_text");
        return val != null && !val.isBlank() ? val : "快来看我在书染美术的作品～";
    }
}
