const app = getApp();

Page({
  data: {
    banners: [],
    studioConfig: {},
    activities: []
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
      url: '/admin/config',
      noAuth: true
    }).then(data => {
      const banners = data.studio_images ? JSON.parse(data.studio_images) : [];
      this.setData({
        studioConfig: data,
        banners: banners.length ? banners : ['/images/default-banner.png']
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
