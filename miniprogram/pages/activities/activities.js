const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activities: []
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
    this.loadActivities();
  },

  loadActivities() {
    app.request({
      url: '/activity/list',
      noAuth: true
    }).then(data => {
      var now = new Date();
      var activities = (data || []).map(function(item) {
        var startDate = item.startTime ? item.startTime.split('T')[0].split(' ')[0] : '';
        var endDate = item.endTime ? item.endTime.split('T')[0].split(' ')[0] : '';
        return Object.assign({}, item, {
          startDate: startDate,
          endDate: endDate
        });
      });
      this.setData({ activities: activities });
    });
  },

  goToDetail(e) {
    var id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/activity/activity?id=' + id });
  }
});
