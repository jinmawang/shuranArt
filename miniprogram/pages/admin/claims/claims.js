const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    records: []
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
    this.loadRecords();
  },

  loadRecords() {
    app.request({
      url: '/admin/lottery-records'
    }).then(data => {
      this.setData({ records: data || [] });
    });
  },

  claimRecord(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    wx.showModal({
      title: '确认核销',
      content: '确认核销奖品"' + name + '"？核销后不可撤回。',
      success: res => {
        if (res.confirm) {
          app.request({
            url: '/admin/lottery-record/' + id + '/claim',
            method: 'POST'
          }).then(() => {
            wx.showToast({ title: '核销成功', icon: 'success' });
            this.loadRecords();
          }).catch(() => {
            wx.showToast({ title: '核销失败', icon: 'none' });
          });
        }
      }
    });
  }
});
