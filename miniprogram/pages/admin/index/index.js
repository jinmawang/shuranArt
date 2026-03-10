const app = getApp();

Page({
  data: {
    isAdmin: false
  },

  onLoad() {
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

  goToPrizes() {
    wx.navigateTo({ url: '/pages/admin/prizes/prizes' });
  }
});
