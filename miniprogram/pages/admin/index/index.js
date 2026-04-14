const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    isAdmin: false
  },

  goBack() {
    wx.navigateBack();
  },

  onLoad() {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    this.checkAdmin();
  },

  checkAdmin() {
    app.request({
      url: '/admin/config'
    }).then(() => {
      this.setData({ isAdmin: true });
    }).catch(() => {
      wx.showToast({
        title: '无管理员权限',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    });
  },

  goToConfig() {
    wx.navigateTo({ url: '/pages/admin/config/config' });
  },

  goToTeachers() {
    wx.navigateTo({ url: '/pages/admin/teachers/teachers' });
  },

  goToActivities() {
    wx.navigateTo({ url: '/pages/admin/activities/activities' });
  },

  goToCourses() {
    wx.navigateTo({ url: '/pages/admin/courses/courses' });
  },

  goToPrizes() {
    wx.navigateTo({ url: '/pages/admin/prizes/prizes' });
  },

  goToBanners() {
    wx.navigateTo({ url: '/pages/admin/banners/banners' });
  }
});
