package com.shuran.art.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.entity.GroupBuyActivity;
import com.shuran.art.entity.GroupBuyMember;
import com.shuran.art.entity.GroupBuyTeam;
import com.shuran.art.entity.User;
import com.shuran.art.mapper.GroupBuyActivityMapper;
import com.shuran.art.mapper.GroupBuyMemberMapper;
import com.shuran.art.mapper.GroupBuyTeamMapper;
import com.shuran.art.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyActivityMapper activityMapper;
    private final GroupBuyTeamMapper teamMapper;
    private final GroupBuyMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final WxAccessTokenManager wxAccessTokenManager;

    private static final String SUBSCRIBE_TEMPLATE_ID = "0-vjVkBwh0ddliu6ZBx8Wav0eIT7AWvUXGxJqLc41CI";

    /**
     * 获取进行中的拼团活动列表
     */
    public List<GroupBuyActivity> getActiveActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<GroupBuyActivity> list = activityMapper.selectList(
            new LambdaQueryWrapper<GroupBuyActivity>()
                .eq(GroupBuyActivity::getStatus, 1)
                .ge(GroupBuyActivity::getEndTime, now)
                .orderByDesc(GroupBuyActivity::getCreatedAt)
        );
        for (GroupBuyActivity a : list) {
            a.setStarted(a.getStartTime() != null && !now.isBefore(a.getStartTime()));
            a.setEnded(a.getEndTime() != null && now.isAfter(a.getEndTime()));
        }
        return list;
    }

    /**
     * 获取活动详情 + 进行中的团列表
     */
    public Map<String, Object> getActivityDetail(Long activityId) {
        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null) return null;

        LocalDateTime now = LocalDateTime.now();
        activity.setStarted(activity.getStartTime() != null && !now.isBefore(activity.getStartTime()));
        activity.setEnded(activity.getEndTime() != null && now.isAfter(activity.getEndTime()));

        // 获取进行中的团
        List<GroupBuyTeam> teams = teamMapper.selectList(
            new LambdaQueryWrapper<GroupBuyTeam>()
                .eq(GroupBuyTeam::getActivityId, activityId)
                .in(GroupBuyTeam::getStatus, 0, 1)
                .orderByDesc(GroupBuyTeam::getCreatedAt)
        );

        // 填充每个团的成员列表
        for (GroupBuyTeam team : teams) {
            fillTeamMembers(team);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activity", activity);
        result.put("teams", teams);
        return result;
    }

    /**
     * 开团
     */
    @Transactional
    public Map<String, Object> createTeam(Long userId, Long activityId, String phone, String nickname, String avatarUrl) {
        Map<String, Object> result = new HashMap<>();

        GroupBuyActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            result.put("success", false);
            result.put("msg", "活动不存在或已结束");
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            result.put("success", false);
            result.put("msg", "活动已结束");
            return result;
        }

        // 检查用户是否已在该活动的某个团中（防止重复开团/参团）
        if (isUserInActivity(userId, activityId)) {
            result.put("success", false);
            result.put("msg", "您已参与该活动的拼团");
            return result;
        }

        // 创建团
        GroupBuyTeam team = new GroupBuyTeam();
        team.setActivityId(activityId);
        team.setLeaderUserId(userId);
        team.setStatus(0);
        team.setMemberCount(1);
        team.setCreatedAt(now);
        teamMapper.insert(team);

        // 团长加入成员表
        GroupBuyMember member = new GroupBuyMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setPhone(phone);
        member.setNickname(nickname);
        member.setAvatarUrl(avatarUrl);
        member.setJoinedAt(now);
        // 如果前端没传昵称/头像，从User表补全
        fillMemberFromUser(member, userId);
        memberMapper.insert(member);

        // 检查是否1人即可成团（虽然不太可能）
        if (activity.getGroupSize() <= 1) {
            completeTeam(team, activity);
        }

        result.put("success", true);
        result.put("teamId", team.getId());
        return result;
    }

    /**
     * 加入团
     */
    @Transactional
    public Map<String, Object> joinTeam(Long userId, Long teamId, String phone, String nickname, String avatarUrl) {
        Map<String, Object> result = new HashMap<>();

        GroupBuyTeam team = teamMapper.selectById(teamId);
        if (team == null || team.getStatus() != 0) {
            result.put("success", false);
            result.put("msg", "该团不存在或已成团");
            return result;
        }

        // 检查活动是否有效
        GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
        if (activity == null || activity.getStatus() != 1) {
            result.put("success", false);
            result.put("msg", "活动已结束");
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            result.put("success", false);
            result.put("msg", "活动已过期");
            return result;
        }

        // 检查是否已经在团中
        Long existCount = memberMapper.selectCount(
            new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getTeamId, teamId)
                .eq(GroupBuyMember::getUserId, userId)
        );
        if (existCount > 0) {
            result.put("success", false);
            result.put("msg", "您已在该团中");
            return result;
        }

        // 检查用户是否已在该活动的其他团中
        if (isUserInActivity(userId, team.getActivityId())) {
            result.put("success", false);
            result.put("msg", "您已参与该活动的拼团");
            return result;
        }

        // 检查团是否已满（原子递增 memberCount 防并发超员）
        int affected = teamMapper.atomicIncrementMemberCount(teamId, activity.getGroupSize());
        if (affected == 0) {
            result.put("success", false);
            result.put("msg", "该团已满或已成团");
            return result;
        }

        // 加入
        GroupBuyMember member = new GroupBuyMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setPhone(phone);
        member.setNickname(nickname);
        member.setAvatarUrl(avatarUrl);
        member.setJoinedAt(now);
        // 如果前端没传昵称/头像，从User表补全
        fillMemberFromUser(member, userId);
        memberMapper.insert(member);

        // 重新读取团信息，检查是否成团
        GroupBuyTeam freshTeam = teamMapper.selectById(teamId);
        if (freshTeam != null && freshTeam.getMemberCount() >= activity.getGroupSize()) {
            completeTeam(freshTeam, activity);
        }

        result.put("success", true);
        result.put("teamId", teamId);
        result.put("completed", freshTeam.getStatus() == 1);
        return result;
    }

    /**
     * 获取团详情
     */
    public GroupBuyTeam getTeamDetail(Long teamId) {
        GroupBuyTeam team = teamMapper.selectById(teamId);
        if (team == null) return null;
        fillTeamMembers(team);
        return team;
    }

    /**
     * 获取用户参与的所有团
     */
    public List<Map<String, Object>> getMyTeams(Long userId) {
        List<GroupBuyMember> myMembers = memberMapper.selectList(
            new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getUserId, userId)
                .orderByDesc(GroupBuyMember::getJoinedAt)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupBuyMember m : myMembers) {
            GroupBuyTeam team = teamMapper.selectById(m.getTeamId());
            if (team == null) continue;
            GroupBuyActivity activity = activityMapper.selectById(team.getActivityId());
            fillTeamMembers(team);

            Map<String, Object> item = new HashMap<>();
            item.put("team", team);
            item.put("activity", activity);
            result.add(item);
        }
        return result;
    }

    // ─── Admin 方法 ─────────────────────────────────

    public List<GroupBuyActivity> getAllActivities() {
        return activityMapper.selectList(
            new LambdaQueryWrapper<GroupBuyActivity>()
                .orderByDesc(GroupBuyActivity::getCreatedAt)
        );
    }

    public void saveActivity(GroupBuyActivity activity) {
        if (activity.getId() != null) {
            activityMapper.updateById(activity);
        } else {
            activityMapper.insert(activity);
        }
    }

    public void deleteActivity(Long id) {
        activityMapper.deleteById(id);
    }

    /**
     * 获取某活动的所有团 + 成员信息
     */
    public List<GroupBuyTeam> getTeamsByActivity(Long activityId) {
        List<GroupBuyTeam> teams = teamMapper.selectList(
            new LambdaQueryWrapper<GroupBuyTeam>()
                .eq(GroupBuyTeam::getActivityId, activityId)
                .orderByDesc(GroupBuyTeam::getCreatedAt)
        );
        for (GroupBuyTeam team : teams) {
            fillTeamMembers(team);
        }
        return teams;
    }

    // ─── 内部方法 ─────────────────────────────────

    /**
     * 检查用户是否已在该活动的任意团中
     */
    private boolean isUserInActivity(Long userId, Long activityId) {
        // 获取该活动的所有团ID
        List<GroupBuyTeam> teams = teamMapper.selectList(
            new LambdaQueryWrapper<GroupBuyTeam>()
                .eq(GroupBuyTeam::getActivityId, activityId)
                .select(GroupBuyTeam::getId)
        );
        if (teams.isEmpty()) return false;
        List<Long> teamIds = teams.stream().map(GroupBuyTeam::getId).toList();
        Long count = memberMapper.selectCount(
            new LambdaQueryWrapper<GroupBuyMember>()
                .in(GroupBuyMember::getTeamId, teamIds)
                .eq(GroupBuyMember::getUserId, userId)
        );
        return count > 0;
    }

    private void fillMemberFromUser(GroupBuyMember member, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        if (member.getNickname() == null || member.getNickname().isBlank()) {
            member.setNickname(user.getNickName() != null ? user.getNickName() : "");
        }
        if (member.getAvatarUrl() == null || member.getAvatarUrl().isBlank()) {
            member.setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        }
        if (member.getPhone() == null || member.getPhone().isBlank()) {
            member.setPhone(user.getPhone() != null ? user.getPhone() : "");
        }
    }

    private void fillTeamMembers(GroupBuyTeam team) {
        List<GroupBuyMember> members = memberMapper.selectList(
            new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getTeamId, team.getId())
                .orderByAsc(GroupBuyMember::getJoinedAt)
        );
        team.setMembers(members);
        if (!members.isEmpty()) {
            team.setLeaderNickname(members.get(0).getNickname());
            team.setLeaderAvatar(members.get(0).getAvatarUrl());
        }
    }

    private void completeTeam(GroupBuyTeam team, GroupBuyActivity activity) {
        team.setStatus(1);
        team.setCompletedAt(LocalDateTime.now());
        teamMapper.updateById(team);
        log.info("拼团成功: activityId={}, teamId={}", activity.getId(), team.getId());

        // 异步发送订阅消息，不阻塞事务
        final Long teamId = team.getId();
        final Long activityIdFinal = activity.getId();
        final String title = activity.getTitle();
        final String completedTime = team.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        new Thread(() -> sendGroupCompleteNotifications(teamId, activityIdFinal, title, completedTime)).start();
    }

    private void sendGroupCompleteNotifications(Long teamId, Long activityId, String title, String completedTime) {
        try {
            List<GroupBuyMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<GroupBuyMember>()
                    .eq(GroupBuyMember::getTeamId, teamId)
            );

            String accessToken = wxAccessTokenManager.getAccessToken();
            RestTemplate restTemplate = new RestTemplate();
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")
            ));
            restTemplate.getMessageConverters().add(0, converter);

            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

            // thing类型字段限制20字符，超出截断
            String safeTitle = title != null && title.length() > 20 ? title.substring(0, 20) : title;

            for (GroupBuyMember member : members) {
                try {
                    String openid = getOpenidByUserId(member.getUserId());
                    if (openid == null || openid.isEmpty()) continue;

                    Map<String, Object> msg = new HashMap<>();
                    msg.put("touser", openid);
                    msg.put("template_id", SUBSCRIBE_TEMPLATE_ID);
                    msg.put("page", "pages/groupbuy/groupbuy?id=" + activityId + "&teamId=" + teamId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("thing1", Map.of("value", safeTitle));
                    data.put("thing2", Map.of("value", "拼团成功！请联系老师线下缴费"));
                    data.put("time3", Map.of("value", completedTime));
                    data.put("character_string4", Map.of("value", String.valueOf(teamId)));
                    msg.put("data", data);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> resp = restTemplate.postForObject(url, msg, Map.class);
                    if (resp != null && !Integer.valueOf(0).equals(resp.get("errcode"))) {
                        log.warn("订阅消息发送失败, userId: {}, errcode: {}, errmsg: {}",
                            member.getUserId(), resp.get("errcode"), resp.get("errmsg"));
                    } else {
                        log.info("拼团成功通知已发送给用户: {}", member.getUserId());
                    }
                } catch (Exception e) {
                    log.warn("发送订阅消息失败, userId: {}", member.getUserId(), e);
                }
            }
        } catch (Exception e) {
            log.error("发送拼团成功通知异常", e);
        }
    }

    private String getOpenidByUserId(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getOpenid() : "";
    }
}
