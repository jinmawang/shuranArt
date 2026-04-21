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
    var self = this;
    Promise.all([
      app.request({ url: '/activity/list', noAuth: true }).catch(function() { return []; }),
      app.request({ url: '/groupbuy/activities', noAuth: true }).catch(function() { return []; })
    ]).then(function(results) {
      var normal = (results[0] || []).map(function(item) {
        return Object.assign({}, item, {
          type: 'activity',
          startDate: self.formatDate(item.startTime),
          endDate: self.formatDate(item.endTime)
        });
      });
      var groupbuy = (results[1] || []).map(function(item) {
        return Object.assign({}, item, {
          type: 'groupbuy',
          startDate: self.formatDate(item.startTime),
          endDate: self.formatDate(item.endTime)
        });
      });
      var all = normal.concat(groupbuy);
      all.sort(function(a, b) {
        return (b.createdAt || '').localeCompare(a.createdAt || '');
      });
      self.setData({ activities: all });
    });
  },

  formatDate: function(dateTimeStr) {
    if (!dateTimeStr) return '';
    var str = dateTimeStr.replace('T', ' ');
    var datePart = str.split(' ')[0] || '';
    var d = datePart.split('-');
    if (d.length < 3) return datePart;
    return d[0] + '年' + parseInt(d[1]) + '月' + parseInt(d[2]) + '日';
  },

  goToDetail(e) {
    var item = e.currentTarget.dataset;
    if (item.type === 'groupbuy') {
      wx.navigateTo({ url: '/pages/groupbuy/groupbuy?id=' + item.id });
    } else {
      wx.navigateTo({ url: '/pages/activity/activity?id=' + item.id });
    }
  }
});
