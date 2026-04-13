# Homepage Enhancement - Studio Environment & Student Works

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enhance the WeChat mini-program homepage by adding a studio environment photo gallery and a student artwork showcase section, making the page more visually rich and informative.

**Architecture:** Add two new visual sections to the existing homepage: (1) a horizontal-scrolling environment gallery using `studio_config` for image storage, (2) a student artwork grid backed by a new `student_work` database table with full CRUD API. Both sections follow the existing Claymorphism design system (indigo/purple gradients, rounded cards, soft shadows).

**Tech Stack:** WeChat Mini Program (WXML/WXSS/JS), Spring Boot + MyBatis Plus (Java), MySQL

---

## Page Layout Prototype

```
┌──────────────────────────────┐
│       Banner Swiper          │  ← keep existing
│    (studio hero images)      │
└──────────────────────────────┘

┌──────────────────────────────┐
│     舒然画室                  │  ← keep, compact
│   用艺术点亮生活              │
│   画室简介文字...             │
└──────────────────────────────┘

┌──────────────────────────────┐
│  ┌─────┐┌─────┐┌─────┐┌────┐│
│  │ 书  ││ 染  ││ 美  ││ 术 ││  ← keep feature grid
│  │师资 ││活动 ││抽奖 ││分享 ││
│  └─────┘└─────┘└─────┘└────┘│
└──────────────────────────────┘

┌──────────────────────────────┐
│ ▍画室环境           [NEW]    │
│                              │
│ ┌────────┐┌────────┐┌──────┐ │
│ │        ││        ││      │←─── scroll-view horizontal
│ │ 教室   ││ 画廊   ││ 休息 │ │   (square images, rounded)
│ │ 环境   ││ 展示   ││ 区域 │ │
│ │        ││        ││      │ │
│ └────────┘└────────┘└──────┘ │
│  caption    caption  caption │
└──────────────────────────────┘

┌──────────────────────────────┐
│ ▍学生作品           [NEW]    │
│                              │
│ ┌──────────┐ ┌──────────┐   │
│ │          │ │          │   │
│ │  artwork │ │  artwork │   │   waterfall / 2-column grid
│ │  image   │ │  image   │   │   each card: image + title
│ │          │ │          │   │   + author name
│ ├──────────┤ ├──────────┤   │
│ │ 《作品名》│ │ 《作品名》│   │
│ │  学生姓名 │ │  学生姓名 │   │
│ └──────────┘ └──────────┘   │
│ ┌──────────┐ ┌──────────┐   │
│ │          │ │          │   │
│ │  artwork │ │  artwork │   │
│ │  image   │ │  image   │   │
│ ├──────────┤ ├──────────┤   │
│ │ 《作品名》│ │ 《作品名》│   │
│ │  学生姓名 │ │  学生姓名 │   │
│ └──────────┘ └──────────┘   │
│                              │
│        [ 查看更多 ]          │  ← optional, if > 4 works
└──────────────────────────────┘

┌──────────────────────────────┐
│ ▍热门活动                    │  ← keep existing
│ ┌────────────────────────┐   │
│ │ cover │ title / status │   │
│ └────────────────────────┘   │
└──────────────────────────────┘

┌──────────────────────────────┐
│ 画室地址              导航   │  ← keep existing
│ 详细地址...                  │
│ ┌────────────────────────┐   │
│ │        map             │   │
│ └────────────────────────┘   │
└──────────────────────────────┘

┌──────────────────────────────┐
│     联系我们                  │  ← keep existing
│    [QR code]                 │
│  长按识别二维码添加老师微信   │
└──────────────────────────────┘
```

## Design Specs

**Color palette** (existing Claymorphism system):
- Primary gradient: `#6366F1` → `#8B5CF6` (indigo to purple)
- Card background: `rgba(255,255,255,0.9)` with gradient overlay
- Card shadow: `10rpx 10rpx 30rpx rgba(99,102,241,0.15)`
- Section title: `#1E1B4B` with left gradient bar
- Body text: `#4B5563` / `#6B7280`
- Border radius: `28rpx` (cards), `16rpx` (inner elements)

**Environment gallery**:
- Horizontal `scroll-view` with snap alignment
- Image cards: `320rpx × 240rpx`, border-radius `20rpx`
- Optional text caption below each image
- Gap between items: `20rpx`

**Student works grid**:
- 2-column grid, gap `20rpx`
- Image: `width: fill`, aspect ratio maintained via `mode="widthFix"`
- Title in `《》` format, `28rpx`, bold, `#1E1B4B`
- Author name: `24rpx`, `#8B5CF6`
- Card: white bg, rounded, Claymorphism shadow

---

## Task 1: Database Migration - Add student_work table

**Files:**
- Create: `mysql/migration_v5.sql`

**Step 1: Write the migration SQL**

```sql
-- migration_v5.sql: Add student_work table for student artwork showcase

CREATE TABLE IF NOT EXISTS `student_work` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL COMMENT '作品名称',
    `author_name` VARCHAR(50) NOT NULL COMMENT '学生姓名',
    `image_url` VARCHAR(500) NOT NULL COMMENT '作品图片URL',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '作品描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序 (越小越前)',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0=隐藏, 1=显示',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生作品';
```

**Step 2: Update init.sql to include the table for fresh installs**

Add the same `CREATE TABLE` statement at the end of `mysql/init.sql`.

**Step 3: Commit**

```bash
git add mysql/migration_v5.sql mysql/init.sql
git commit -m "feat: add student_work table for artwork showcase"
```

---

## Task 2: Backend Entity & Mapper for StudentWork

**Files:**
- Create: `backend/src/main/java/com/shuran/art/entity/StudentWork.java`
- Create: `backend/src/main/java/com/shuran/art/mapper/StudentWorkMapper.java`

**Step 1: Create the entity**

```java
package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_work")
public class StudentWork {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String authorName;
    private String imageUrl;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
```

**Step 2: Create the mapper**

```java
package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.StudentWork;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentWorkMapper extends BaseMapper<StudentWork> {
}
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/shuran/art/entity/StudentWork.java \
       backend/src/main/java/com/shuran/art/mapper/StudentWorkMapper.java
git commit -m "feat: add StudentWork entity and mapper"
```

---

## Task 3: Backend API - Public endpoint for student works

**Files:**
- Modify: `backend/src/main/java/com/shuran/art/controller/StudioController.java`

**Step 1: Add student works list endpoint**

Add a new endpoint to `StudioController` (since it already handles public studio content):

```java
@Autowired
private StudentWorkMapper studentWorkMapper;

@GetMapping("/works")
public Object getStudentWorks() {
    QueryWrapper<StudentWork> wrapper = new QueryWrapper<>();
    wrapper.eq("status", 1)
           .orderByAsc("sort_order")
           .orderByDesc("created_at");
    return studentWorkMapper.selectList(wrapper);
}
```

This endpoint returns all active student works, sorted by sort_order then newest first.

**Step 2: Register the path as public (no auth required)**

Check `WebConfig.java` or the auth interceptor to ensure `/studio/works` is excluded from auth, same as `/studio/config`. The existing `StudioController` has `noAuth` or is already public - verify and match the pattern.

**Step 3: Commit**

```bash
git add backend/src/main/java/com/shuran/art/controller/StudioController.java
git commit -m "feat: add public API endpoint for student works"
```

---

## Task 4: Backend API - Admin CRUD for student works

**Files:**
- Modify: `backend/src/main/java/com/shuran/art/controller/AdminController.java`

**Step 1: Add admin endpoints for student work management**

Add to `AdminController`:

```java
@Autowired
private StudentWorkMapper studentWorkMapper;

// List all student works (including hidden)
@GetMapping("/works")
public Object listWorks() {
    QueryWrapper<StudentWork> wrapper = new QueryWrapper<>();
    wrapper.orderByAsc("sort_order").orderByDesc("created_at");
    return studentWorkMapper.selectList(wrapper);
}

// Add or update a student work
@PostMapping("/work")
public Object saveWork(@RequestBody StudentWork work) {
    if (work.getId() != null) {
        studentWorkMapper.updateById(work);
    } else {
        if (work.getSortOrder() == null) work.setSortOrder(0);
        if (work.getStatus() == null) work.setStatus(1);
        studentWorkMapper.insert(work);
    }
    return work;
}

// Delete a student work
@DeleteMapping("/work/{id}")
public Object deleteWork(@PathVariable Long id) {
    studentWorkMapper.deleteById(id);
    return "ok";
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/shuran/art/controller/AdminController.java
git commit -m "feat: add admin CRUD for student works"
```

---

## Task 5: Backend - Add environment images config support

**Files:**
- No code changes needed - `studio_config` is a key-value store

The existing `studio_config` table already supports arbitrary keys. The admin can add a new key `studio_environment_images` with a JSON array value like:

```json
[
  {"url": "https://example.com/classroom.jpg", "caption": "宽敞明亮的教室"},
  {"url": "https://example.com/gallery.jpg", "caption": "学生画廊展示区"},
  {"url": "https://example.com/rest.jpg", "caption": "休息交流区"}
]
```

This is handled by the existing `POST /admin/config` API. No backend changes needed.

**Step 1: Document the config key in a comment**

Add a comment to the admin config page or just note it here - the key is `studio_environment_images`, value is a JSON array of `{url, caption}` objects.

---

## Task 6: Frontend - Add environment gallery section to homepage WXML

**Files:**
- Modify: `miniprogram/pages/index/index.wxml`

**Step 1: Add the environment gallery section**

Insert after the feature-grid section (line 65) and before the activity list section (line 68):

```xml
<!-- 画室环境 -->
<view class="section" wx:if="{{environmentImages.length > 0}}">
  <view class="section-title">画室环境</view>
  <scroll-view class="env-gallery" scroll-x enhanced show-scrollbar="{{false}}">
    <view class="env-gallery-inner">
      <view class="env-card" wx:for="{{environmentImages}}" wx:key="index" bindtap="previewEnvImage" data-index="{{index}}">
        <image src="{{item.url}}" mode="aspectFill" class="env-img"/>
        <view class="env-caption" wx:if="{{item.caption}}">{{item.caption}}</view>
      </view>
    </view>
  </scroll-view>
</view>
```

**Step 2: Commit**

```bash
git add miniprogram/pages/index/index.wxml
git commit -m "feat: add environment gallery section to homepage"
```

---

## Task 7: Frontend - Add student works section to homepage WXML

**Files:**
- Modify: `miniprogram/pages/index/index.wxml`

**Step 1: Add the student works grid section**

Insert after the environment gallery and before the activity list:

```xml
<!-- 学生作品 -->
<view class="section" wx:if="{{studentWorks.length > 0}}">
  <view class="section-title">学生作品</view>
  <view class="works-grid">
    <view class="work-card" wx:for="{{displayWorks}}" wx:key="id" bindtap="previewWorkImage" data-index="{{index}}">
      <image src="{{item.imageUrl}}" mode="widthFix" class="work-img"/>
      <view class="work-info">
        <view class="work-title">{{item.title}}</view>
        <view class="work-author">{{item.authorName}}</view>
      </view>
    </view>
  </view>
  <view class="show-more-btn" wx:if="{{studentWorks.length > 4 && !showAllWorks}}" bindtap="toggleShowAllWorks">
    查看更多作品
  </view>
  <view class="show-more-btn" wx:if="{{showAllWorks && studentWorks.length > 4}}" bindtap="toggleShowAllWorks">
    收起
  </view>
</view>
```

**Step 2: Commit**

```bash
git add miniprogram/pages/index/index.wxml
git commit -m "feat: add student works grid section to homepage"
```

---

## Task 8: Frontend - Add CSS styles for new sections

**Files:**
- Modify: `miniprogram/pages/index/index.wxss`

**Step 1: Add environment gallery styles**

Append to `index.wxss`:

```css
/* ============================================
   画室环境 - 横向滚动画廊
   ============================================ */
.env-gallery {
  width: 100%;
  white-space: nowrap;
}

.env-gallery-inner {
  display: inline-flex;
  gap: 20rpx;
  padding: 0 0 16rpx;
}

.env-card {
  display: inline-flex;
  flex-direction: column;
  width: 320rpx;
  flex-shrink: 0;
}

.env-img {
  width: 320rpx;
  height: 240rpx;
  border-radius: 20rpx;
  box-shadow:
    6rpx 6rpx 18rpx rgba(99, 102, 241, 0.15),
    -3rpx -3rpx 10rpx rgba(255, 255, 255, 0.8);
}

.env-caption {
  font-size: 24rpx;
  color: #4B5563;
  margin-top: 12rpx;
  text-align: center;
  white-space: normal;
}
```

**Step 2: Add student works grid styles**

```css
/* ============================================
   学生作品 - 双列网格
   ============================================ */
.works-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.work-card {
  width: calc(50% - 10rpx);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow:
    6rpx 6rpx 18rpx rgba(99, 102, 241, 0.12),
    -3rpx -3rpx 10rpx rgba(255, 255, 255, 0.9);
  border: 2rpx solid rgba(99, 102, 241, 0.08);
  transition: all 200ms ease-out;
}

.work-card:active {
  transform: scale(0.98);
}

.work-img {
  width: 100%;
  display: block;
}

.work-info {
  padding: 16rpx 20rpx 20rpx;
}

.work-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1E1B4B;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-author {
  font-size: 22rpx;
  color: #8B5CF6;
  margin-top: 6rpx;
}

.show-more-btn {
  text-align: center;
  padding: 20rpx 0;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #6366F1;
  background: rgba(99, 102, 241, 0.06);
  border-radius: 16rpx;
  font-weight: 500;
}

.show-more-btn:active {
  background: rgba(99, 102, 241, 0.12);
}
```

**Step 3: Commit**

```bash
git add miniprogram/pages/index/index.wxss
git commit -m "feat: add styles for environment gallery and student works"
```

---

## Task 9: Frontend - Add JS logic for new sections

**Files:**
- Modify: `miniprogram/pages/index/index.js`

**Step 1: Update data and loadData**

Add to `data` object:

```javascript
environmentImages: [],
studentWorks: [],
displayWorks: [],
showAllWorks: false
```

Update `loadData()` to load the new data:

```javascript
loadData() {
  return Promise.all([
    this.loadConfig(),
    this.loadActivities(),
    this.loadStudentWorks()
  ]);
},
```

**Step 2: Add loadStudentWorks method**

```javascript
loadStudentWorks() {
  return app.request({
    url: '/studio/works',
    noAuth: true
  }).then(data => {
    const works = data || [];
    this.setData({
      studentWorks: works,
      displayWorks: works.slice(0, 4)
    });
  }).catch(() => {});
},
```

**Step 3: Parse environment images in loadConfig**

In the existing `loadConfig()` method, after parsing `studio_images`, add:

```javascript
const envImages = data.studio_environment_images
  ? JSON.parse(data.studio_environment_images)
  : [];
```

And include in the `setData` call:

```javascript
this.setData({
  // ...existing fields...
  environmentImages: envImages
});
```

**Step 4: Add interaction methods**

```javascript
// Preview environment image
previewEnvImage(e) {
  const index = e.currentTarget.dataset.index;
  const urls = this.data.environmentImages.map(img => img.url);
  wx.previewImage({
    urls: urls,
    current: urls[index]
  });
},

// Preview student work image
previewWorkImage(e) {
  const index = e.currentTarget.dataset.index;
  const source = this.data.showAllWorks ? this.data.studentWorks : this.data.displayWorks;
  const urls = source.map(work => work.imageUrl);
  wx.previewImage({
    urls: urls,
    current: urls[index]
  });
},

// Toggle show all works
toggleShowAllWorks() {
  const showAll = !this.data.showAllWorks;
  this.setData({
    showAllWorks: showAll,
    displayWorks: showAll ? this.data.studentWorks : this.data.studentWorks.slice(0, 4)
  });
},
```

**Step 5: Commit**

```bash
git add miniprogram/pages/index/index.js
git commit -m "feat: add data loading and interaction for new homepage sections"
```

---

## Task 10: Admin Page - Add student work management

**Files:**
- Create: `miniprogram/pages/admin/works/works.wxml`
- Create: `miniprogram/pages/admin/works/works.wxss`
- Create: `miniprogram/pages/admin/works/works.js`
- Create: `miniprogram/pages/admin/works/works.json`
- Modify: `miniprogram/app.json` (add page route)
- Modify: `miniprogram/pages/admin/index/index.wxml` (add entry link)

**Step 1: Create the admin works management page**

Follow the same pattern as the existing admin pages (teachers/activities/prizes management). The page should support:
- List all student works
- Add new work (title, author name, image upload, description)
- Delete a work
- Toggle visibility (status)

Use `wx.chooseImage` + upload to server for image handling, matching the pattern used in the existing admin config page for image uploads.

**Step 2: Register the page in app.json**

Add `"pages/admin/works/works"` to the pages array in `app.json`.

**Step 3: Add entry link in admin index page**

Add a new menu item in the admin index page linking to the works management page.

**Step 4: Commit**

```bash
git add miniprogram/pages/admin/works/ miniprogram/app.json miniprogram/pages/admin/index/index.wxml
git commit -m "feat: add admin page for student work management"
```

---

## Task 11: End-to-end verification

**Step 1: Run the backend**

```bash
cd backend && mvn spring-boot:run
```

Verify no compilation errors.

**Step 2: Run the database migration**

```bash
mysql -u root -p shuranart < mysql/migration_v5.sql
```

**Step 3: Test the API endpoints**

```bash
# Test public student works endpoint
curl http://localhost:8080/api/studio/works

# Test admin add work
curl -X POST http://localhost:8080/api/admin/work \
  -H "Content-Type: application/json" \
  -d '{"title":"山水画","authorName":"张同学","imageUrl":"https://example.com/test.jpg"}'
```

**Step 4: Verify the mini-program renders correctly**

Open in WeChat Developer Tools, check:
- Environment gallery scrolls horizontally
- Student works grid displays in 2 columns
- Image preview works for both sections
- "查看更多" toggle works
- Empty states are handled (sections hidden when no data)

**Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: address any issues found during verification"
```

---

## Summary of Changes

| Area | Files Changed | Description |
|------|--------------|-------------|
| Database | `migration_v5.sql`, `init.sql` | New `student_work` table |
| Backend | `StudentWork.java`, `StudentWorkMapper.java` | Entity + Mapper |
| Backend | `StudioController.java` | Public `/works` API |
| Backend | `AdminController.java` | Admin CRUD for works |
| Frontend | `index.wxml` | Environment gallery + works grid HTML |
| Frontend | `index.wxss` | Claymorphism styles for new sections |
| Frontend | `index.js` | Data loading + interactions |
| Admin | `works/*` | Student works management page |
| Config | `app.json` | Register admin works page |
