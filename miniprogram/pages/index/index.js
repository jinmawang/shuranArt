const app = getApp();

// ─── 重力球物理常量 ───────────────────────────────
var BALL_R   = 26;    // 球半径 px（与 CSS 52px/2 对应）
var G_FORCE  = 1.2;   // 重力加速度系数（响应速度更快）
var FRICTION = 0.985; // 每帧速度衰减（保留更多动能）
var BOUNCE   = 0.65;  // 碰壁弹性系数（更有弹性）
var BALL_RESTITUTION = 0.85; // 球间碰撞弹性系数
var CHARS    = ['书', '染', '美', '术'];
// ─────────────────────────────────────────────────

const WORKS_ALL = [
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193931.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193239.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193228.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223194551.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193219.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193209.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193234.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193155.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193245.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193251.jpg',
  'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193118.jpg'
];

Page({
  // ─── 物理状态（不放 data，避免 setData 开销）────
  _phys: null,    // [{x,y,vx,vy}, ...]
  _accel: { x: 0, y: 0 },
  _timer: null,
  _sw: 375,
  _sh: 667,
  _alive: false,
  // ──────────────────────────────────────────────

  LOCAL_BANNERS: [
    { id: 'l1', imageUrl: 'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193931.jpg', description: '学生优秀作品展示' },
    { id: 'l2', imageUrl: 'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193239.jpg', description: '创意绘画课堂' },
    { id: 'l3', imageUrl: 'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223194551.jpg', description: '艺术成长之路' },
    { id: 'l4', imageUrl: 'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193245.jpg', description: '专业美术教学' },
    { id: 'l5', imageUrl: 'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193251.jpg', description: '书染美术 · 用艺术点亮生活' }
  ],

  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    banners: [],
    studioConfig: {},
    activities: [],
    latitude: null,
    longitude: null,
    markers: [],
    honors: [
      { id: 1, img: 'https://tianma.chat/images/%E7%94%BB%E5%AE%A4%E5%A5%96%E7%AB%A0/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241212083047.jpg' },
      { id: 2, img: 'https://tianma.chat/images/%E7%94%BB%E5%AE%A4%E5%A5%96%E7%AB%A0/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241212083111.jpg' }
    ],
    worksLeft: [],
    worksRight: [],
    worksAll: [],
    balls: [
      { char: '书', x: 60,  y: 120 },
      { char: '染', x: 140, y: 120 },
      { char: '美', x: 220, y: 120 },
      { char: '术', x: 300, y: 120 }
    ]
  },

  onLoad: function() {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    this._initBalls();
    this.loadData();
  },

  onShow: function() {
    this._startPhysics();
    if (!app.globalData.token) {
      app.login().then(function() {
        this.loadData();
      }.bind(this));
    }
  },

  onHide: function() {
    this._stopPhysics();
  },

  onUnload: function() {
    this._stopPhysics();
  },

  onPullDownRefresh: function() {
    this.loadData().finally(function() {
      wx.stopPullDownRefresh();
    });
  },

  loadData: function() {
    return Promise.all([
      this.loadConfig(),
      this.loadActivities(),
      this.loadBanners(),
      this.loadFeaturedWorks()
    ]);
  },

  loadConfig: function() {
    return app.request({
      url: '/studio/config',
      noAuth: true
    }).then(function(data) {
      if (!data) return;
      var latitude = data.studio_latitude ? parseFloat(data.studio_latitude) : null;
      var longitude = data.studio_longitude ? parseFloat(data.studio_longitude) : null;
      var markers = [];
      if (latitude && longitude) {
        markers.push({
          id: 1,
          latitude: latitude,
          longitude: longitude,
          title: data.studio_name || '书染美术',
          iconPath: '/images/icon-marker.png',
          width: 40,
          height: 40
        });
      }
      this.setData({ studioConfig: data, latitude: latitude, longitude: longitude, markers: markers });
    }.bind(this)).catch(function() {});
  },

  loadBanners: function() {
    var self = this;
    return app.request({
      url: '/banner/list',
      noAuth: true
    }).then(function(data) {
      self.setData({ banners: (data && data.length) ? data : self.LOCAL_BANNERS });
    }).catch(function() {
      self.setData({ banners: self.LOCAL_BANNERS });
    });
  },

  loadFeaturedWorks: function() {
    var self = this;
    return app.request({
      url: '/works/featured',
      noAuth: true
    }).then(function(data) {
      var featured = (data || []).map(function(w) { return w.imageUrl; });
      // 不足10条用内置图片补齐
      var all = featured.slice();
      var i = 0;
      while (all.length < 10 && i < WORKS_ALL.length) {
        if (all.indexOf(WORKS_ALL[i]) === -1) {
          all.push(WORKS_ALL[i]);
        }
        i++;
      }
      self.setData({
        worksAll: all,
        worksLeft: all.filter(function(_, idx) { return idx % 2 === 0; }),
        worksRight: all.filter(function(_, idx) { return idx % 2 === 1; })
      });
    }).catch(function() {
      // 接口失败用内置
      self.setData({
        worksAll: WORKS_ALL,
        worksLeft: WORKS_ALL.filter(function(_, idx) { return idx % 2 === 0; }),
        worksRight: WORKS_ALL.filter(function(_, idx) { return idx % 2 === 1; })
      });
    });
  },

  loadActivities: function() {
    var self = this;
    return app.request({
      url: '/activity/list',
      noAuth: true
    }).then(function(data) {
      var activities = (data || []).map(function(item) {
        return Object.assign({}, item, {
          startDate: self.formatDate(item.startTime),
          endDate: self.formatDate(item.endTime)
        });
      });
      self.setData({ activities: activities });
    }).catch(function() {});
  },

  formatDate: function(dateTimeStr) {
    if (!dateTimeStr) return '';
    return dateTimeStr.split('T')[0].split(' ')[0];
  },

  openMap: function() {
    var latitude = this.data.latitude;
    var longitude = this.data.longitude;
    var studioConfig = this.data.studioConfig;
    if (latitude && longitude) {
      wx.openLocation({
        latitude: latitude,
        longitude: longitude,
        name: studioConfig.studio_name || '书染美术',
        address: studioConfig.studio_address || '',
        scale: 16
      });
    } else {
      wx.showToast({ title: '暂无地址信息', icon: 'none' });
    }
  },

  previewQrcode: function() {
    var qrcode = this.data.studioConfig.studio_qrcode;
    if (qrcode) {
      wx.previewImage({ urls: [qrcode], current: qrcode });
    }
  },

  goToStudio: function() {
    wx.navigateTo({ url: '/pages/studio/studio' });
  },

  goToCourses: function() {
    wx.navigateTo({ url: '/pages/courses/courses' });
  },

  goToActivity: function() {
    wx.navigateTo({ url: '/pages/activities/activities' });
  },

  goToLottery: function() {
    wx.switchTab({ url: '/pages/lottery/lottery' });
  },

  goToWorks: function() {
    wx.navigateTo({ url: '/pages/works/works' });
  },

  goToWorksWall: function() {
    wx.switchTab({ url: '/pages/works/works' });
  },

  goToBannerDetail: function(e) {
    var id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/banner-detail/banner-detail?id=' + id });
  },

  goToActivityDetail: function(e) {
    wx.navigateTo({ url: '/pages/activity/activity?id=' + e.currentTarget.dataset.id });
  },

  previewWork: function(e) {
    var all = e.currentTarget.dataset.all;
    var src = e.currentTarget.dataset.src;
    wx.previewImage({ urls: all, current: src });
  },

  // ══════════════════════════════════════════════
  // 重力感应物理引擎
  // 原理：
  //   1. wx.onAccelerometerChange 返回设备三轴加速度
  //      x: 正 = 右侧倾斜下沉  y: 正 = 顶部抬起（底部下沉）
  //   2. 每帧将加速度分量乘以系数，叠加到球的速度 vx/vy
  //   3. 乘以 FRICTION 系数模拟摩擦/空气阻力，使球最终停下
  //   4. 边界检测：碰壁时速度取反并乘以 BOUNCE 系数弹射
  //   5. 球间碰撞：检测两球心距离 < 2R 时做弹性碰撞
  //   6. 物理状态存在 _phys 数组而非 data 中，
  //      每帧只 setData 4 个 {x,y} 对象，性能开销小
  // ══════════════════════════════════════════════

  _initBalls: function() {
    var info = wx.getSystemInfoSync();
    this._sw = info.windowWidth;
    this._sh = info.windowHeight;
    var sw = this._sw;
    var sh = this._sh;
    // 初始位置从左到右排列（书染美术），给予小初速让球"活起来"
    this._phys = [
      { x: sw * 0.15, y: sh * 0.15, vx:  1, vy:  1 },
      { x: sw * 0.38, y: sh * 0.15, vx: -1, vy:  2 },
      { x: sw * 0.62, y: sh * 0.15, vx:  1, vy: -1 },
      { x: sw * 0.85, y: sh * 0.15, vx: -1, vy:  1 }
    ];
  },

  _startPhysics: function() {
    if (this._alive) return;
    this._alive = true;
    var self = this;

    // 开启加速度计（game 档位 ~25ms 刷新，足够流畅）
    wx.startAccelerometer({ interval: 'game' });
    wx.onAccelerometerChange(function(res) {
      self._accel = res;
    });

    // 物理循环 ~30fps
    this._timer = setInterval(function() {
      self._stepPhysics();
    }, 33);
  },

  _stopPhysics: function() {
    this._alive = false;
    if (this._timer) {
      clearInterval(this._timer);
      this._timer = null;
    }
    wx.stopAccelerometer();
  },

  _stepPhysics: function() {
    var phys = this._phys;
    if (!phys) return;
    var ax = this._accel.x || 0;
    var ay = this._accel.y || 0;
    var sw = this._sw;
    var sh = this._sh;
    var r  = BALL_R;
    var i, j, b, bj;

    // ── 1. 施加重力 & 摩擦 ──────────────────────
    for (i = 0; i < phys.length; i++) {
      b = phys[i];
      // accel.x 正 = 右倾 → 球向右加速（vx 增大）
      // accel.y 正 = 底部抬高(手机前倾) → 球向下加速，取负号
      b.vx += ax * G_FORCE;
      b.vy += -ay * G_FORCE;
      b.vx *= FRICTION;
      b.vy *= FRICTION;
      b.x  += b.vx;
      b.y  += b.vy;
    }

    // ── 2. 边界碰撞 ─────────────────────────────
    for (i = 0; i < phys.length; i++) {
      b = phys[i];
      if (b.x < r)      { b.x = r;      b.vx =  Math.abs(b.vx) * BOUNCE; }
      if (b.x > sw - r) { b.x = sw - r; b.vx = -Math.abs(b.vx) * BOUNCE; }
      if (b.y < r)      { b.y = r;      b.vy =  Math.abs(b.vy) * BOUNCE; }
      if (b.y > sh - r) { b.y = sh - r; b.vy = -Math.abs(b.vy) * BOUNCE; }
    }

    // ── 3. 球间碰撞（弹性碰撞）────────────────────────
    var minD = r * 2;
    var hitFlags = [false, false, false, false];
    for (i = 0; i < phys.length; i++) {
      for (j = i + 1; j < phys.length; j++) {
        b  = phys[i];
        bj = phys[j];
        var dx = bj.x - b.x;
        var dy = bj.y - b.y;
        var dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < minD && dist > 0.01) {
          // 将两球沿法线方向推开，消除穿透
          var overlap = (minD - dist) * 0.5;
          var nx = dx / dist;
          var ny = dy / dist;
          b.x  -= nx * overlap;
          b.y  -= ny * overlap;
          bj.x += nx * overlap;
          bj.y += ny * overlap;
          // 沿碰撞法线方向的相对速度
          var dvx = b.vx - bj.vx;
          var dvy = b.vy - bj.vy;
          var dot = dvx * nx + dvy * ny;
          if (dot > 0) {
            // 使用较高的弹性系数，碰撞更有力
            var imp = dot * BALL_RESTITUTION;
            b.vx  -= imp * nx;
            b.vy  -= imp * ny;
            bj.vx += imp * nx;
            bj.vy += imp * ny;
            // 标记碰撞，用于视觉反馈
            hitFlags[i] = true;
            hitFlags[j] = true;
          }
        }
      }
    }

    // ── 4. 一次性 setData 更新视图 ──────────────
    this.setData({
      'balls[0].x': phys[0].x, 'balls[0].y': phys[0].y, 'balls[0].hit': hitFlags[0],
      'balls[1].x': phys[1].x, 'balls[1].y': phys[1].y, 'balls[1].hit': hitFlags[1],
      'balls[2].x': phys[2].x, 'balls[2].y': phys[2].y, 'balls[2].hit': hitFlags[2],
      'balls[3].x': phys[3].x, 'balls[3].y': phys[3].y, 'balls[3].hit': hitFlags[3]
    });
  }
});
