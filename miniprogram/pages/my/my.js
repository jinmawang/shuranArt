const app = getApp();

Page({
  data: {
    userInfo: {},
    records: []
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    this.loadData();
  },

  loadData() {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.fetchData();
      });
    } else {
      this.fetchData();
    }
  },

  fetchData() {
    // 获取用户信息
    app.request({
      url: '/user/info'
    }).then(data => {
      this.setData({ userInfo: data });
      app.globalData.userInfo = data;
    });

    // 获取中奖记录
    app.request({
      url: '/lottery/records'
    }).then(data => {
      this.setData({ records: (data || []).slice(0, 5) });
    });
  },

  goToRecords() {
    // 可以创建一个单独的记录页面，这里简单处理
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
    });
  },

  goToShare() {
    wx.navigateTo({
      url: '/pages/share/share'
    });
  }
});
