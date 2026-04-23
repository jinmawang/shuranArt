package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.entity.GroupBuyActivity;
import com.shuran.art.entity.GroupBuyMember;
import com.shuran.art.entity.GroupBuyTeam;
import com.shuran.art.service.GroupBuyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groupbuy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyService groupBuyService;

    /**
     * 获取进行中的拼团活动列表（无需登录）
     */
    @GetMapping("/activities")
    public Result<List<GroupBuyActivity>> getActivities() {
        return Result.success(groupBuyService.getActiveActivities());
    }

    /**
     * 获取拼团活动详情+团列表（无需登录）
     * 注意：公开接口，需要隐藏成员手机号
     */
    @GetMapping("/activity")
    public Result<Map<String, Object>> getActivityDetail(@RequestParam Long id) {
        Map<String, Object> detail = groupBuyService.getActivityDetail(id);
        if (detail == null) {
            return Result.error("活动不存在");
        }
        // 公开接口隐藏手机号
        stripPhoneFromTeams(detail);
        return Result.success(detail);
    }

    /**
     * 开团
     */
    @PostMapping("/create-team")
    public Result<Map<String, Object>> createTeam(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (body.get("activityId") == null) {
            return Result.error("活动ID不能为空");
        }
        Long activityId = Long.valueOf(body.get("activityId").toString());
        String phone = (String) body.getOrDefault("phone", "");
        String nickname = (String) body.getOrDefault("nickname", "");
        String avatarUrl = (String) body.getOrDefault("avatarUrl", "");

        Map<String, Object> result = groupBuyService.createTeam(userId, activityId, phone, nickname, avatarUrl);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("msg"));
    }

    /**
     * 加入团
     */
    @PostMapping("/join-team")
    public Result<Map<String, Object>> joinTeam(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (body.get("teamId") == null) {
            return Result.error("团ID不能为空");
        }
        Long teamId = Long.valueOf(body.get("teamId").toString());
        String phone = (String) body.getOrDefault("phone", "");
        String nickname = (String) body.getOrDefault("nickname", "");
        String avatarUrl = (String) body.getOrDefault("avatarUrl", "");

        Map<String, Object> result = groupBuyService.joinTeam(userId, teamId, phone, nickname, avatarUrl);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("msg"));
    }

    /**
     * 获取团详情（隐藏手机号）
     */
    @GetMapping("/team")
    public Result<GroupBuyTeam> getTeamDetail(@RequestParam Long id) {
        GroupBuyTeam team = groupBuyService.getTeamDetail(id);
        if (team == null) {
            return Result.error("团不存在");
        }
        stripPhoneFromTeam(team);
        return Result.success(team);
    }

    /**
     * 我参与的团（隐藏手机号）
     */
    @GetMapping("/my-teams")
    public Result<List<Map<String, Object>>> getMyTeams(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> list = groupBuyService.getMyTeams(userId);
        for (Map<String, Object> item : list) {
            Object t = item.get("team");
            if (t instanceof GroupBuyTeam) {
                stripPhoneFromTeam((GroupBuyTeam) t);
            }
        }
        return Result.success(list);
    }

    /**
     * 非管理员接口隐藏成员手机号（隐私保护）
     */
    @SuppressWarnings("unchecked")
    private void stripPhoneFromTeams(Map<String, Object> detail) {
        List<GroupBuyTeam> teams = (List<GroupBuyTeam>) detail.get("teams");
        if (teams == null) return;
        for (GroupBuyTeam team : teams) {
            stripPhoneFromTeam(team);
        }
    }

    private void stripPhoneFromTeam(GroupBuyTeam team) {
        if (team.getMembers() == null) return;
        for (GroupBuyMember member : team.getMembers()) {
            member.setPhone(null);
        }
    }
}
