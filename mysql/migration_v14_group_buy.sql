-- 拼团活动表
CREATE TABLE IF NOT EXISTS group_buy_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NOT NULL COMMENT '课程名称（暑期课/寒假课/体验课）',
  description TEXT COMMENT '活动描述',
  cover_img VARCHAR(500) COMMENT '活动图片',
  share_image VARCHAR(500) COMMENT '分享图片',
  share_title VARCHAR(100) COMMENT '分享标题',
  group_size INT NOT NULL DEFAULT 3 COMMENT '几人成团',
  price VARCHAR(50) COMMENT '价格描述',
  start_time DATETIME COMMENT '活动开始时间',
  end_time DATETIME COMMENT '活动结束时间',
  status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 拼团小组表
CREATE TABLE IF NOT EXISTS group_buy_team (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  leader_user_id BIGINT NOT NULL COMMENT '团长用户ID',
  status TINYINT DEFAULT 0 COMMENT '0拼团中 1已成团 2已过期',
  member_count INT DEFAULT 1 COMMENT '当前人数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME COMMENT '成团时间',
  INDEX idx_activity (activity_id),
  INDEX idx_leader (leader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 拼团成员表
CREATE TABLE IF NOT EXISTS group_buy_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  team_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  phone VARCHAR(20) COMMENT '微信授权手机号',
  nickname VARCHAR(100) COMMENT '微信昵称',
  avatar_url VARCHAR(500) COMMENT '头像',
  joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_team_user (team_id, user_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
