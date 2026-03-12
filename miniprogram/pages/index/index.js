const app = getApp();

Page({
  data: {
    banners: [],
    studioConfig: {},
    activities: [],
    latitude: null,
    longitude: null,
    markers: []
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    // 确保已登录
    if (!app.globalData.token) {
      app.login().then(() => {
        this.loadData();
      });
    }
  },

  onPullDownRefresh() {
    this.loadData().finally(() => {
      wx.stopPullDownRefresh();
    });
  },

  loadData() {
    return Promise.all([
      this.loadConfig(),
      this.loadActivities()
    ]);
  },

  loadConfig() {
    return app.request({
      url: '/studio/config',
      noAuth: true
    }).then(data => {
      const banners = data.studio_images ? JSON.parse(data.studio_images) : [];
      const latitude = data.studio_latitude ? parseFloat(data.studio_latitude) : null;
      const longitude = data.studio_longitude ? parseFloat(data.studio_longitude) : null;

      // 设置地图标记
      const markers = [];
      if (latitude && longitude) {
        markers.push({
          id: 1,
          latitude: latitude,
          longitude: longitude,
          title: data.studio_name || '舒然画室',
          iconPath: '/images/icon-marker.png',
          width: 40,
          height: 40
        });
      }

      this.setData({
        studioConfig: data,
        banners: banners.length ? banners : ['/images/default-banner.png'],
        latitude: latitude,
        longitude: longitude,
        markers: markers
      });
    }).catch(() => {
      this.setData({
        banners: ['/images/default-banner.png']
      });
    });
  },

  loadActivities() {
    return app.request({
      url: '/activity/list',
      noAuth: true
    }).then(data => {
      this.setData({
        activities: data || []
      });
    });
  },

  // 打开地图导航
  openMap() {
    const { latitude, longitude, studioConfig } = this.data;
    if (latitude && longitude) {
      wx.openLocation({
        latitude: latitude,
        longitude: longitude,
        name: studioConfig.studio_name || '舒然画室',
        address: studioConfig.studio_address || '',
        scale: 16
      });
    } else {
      wx.showToast({
        title: '暂无地址信息',
        icon: 'none'
      });
    }
  },

  // 预览二维码
  previewQrcode() {
    const qrcode = this.data.studioConfig.studio_qrcode;
    if (qrcode) {
      wx.previewImage({
        urls: [qrcode],
        current: qrcode
      });
    }
  },

  goToTeachers() {
    wx.navigateTo({
      url: '/pages/teachers/teachers'
    });
  },

  goToActivity() {
    if (this.data.activities.length > 0) {
      wx.navigateTo({
        url: '/pages/activity/activity?id=' + this.data.activities[0].id
      });
    } else {
      wx.showToast({
        title: '暂无活动',
        icon: 'none'
      });
    }
  },

  goToLottery() {
    wx.switchTab({
      url: '/pages/lottery/lottery'
    });
  },

  goToShare() {
    if (this.data.activities.length > 0) {
      wx.navigateTo({
        url: '/pages/share/share?activityId=' + this.data.activities[0].id
      });
    } else {
      wx.showToast({
        title: '暂无活动',
        icon: 'none'
      });
    }
  },

  goToActivityDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/activity/activity?id=' + id
    });
  }
});
