const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    currentTab: 0,
    prizeRecords: [],
    pointsUsers: [],
    minPoints: 100
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
    this.loadData();
    this.loadConfig();
  },

  switchTab(e) {
    this.setData({ currentTab: parseInt(e.currentTarget.dataset.tab) });
  },

  loadConfig() {
    app.request({ url: '/admin/config' }).then(data => {
      if (data && data.min_exchange_points) {
        this.setData({ minPoints: parseInt(data.min_exchange_points) || 100 });
      }
    });
  },

  loadData() {
    // 奖品类待核销
    app.request({ url: '/admin/lottery-records' }).then(data => {
      this.setData({ prizeRecords: data || [] });
    });
    // 积分汇总
    app.request({ url: '/admin/points-summary' }).then(data => {
      this.setData({ pointsUsers: data || [] });
    });
  },

  claimRecord(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    wx.showModal({
      title: '确认核销',
      content: '确认核销奖品"' + name + '"？',
      success: res => {
        if (res.confirm) {
          app.request({
            url: '/admin/lottery-record/' + id + '/claim',
            method: 'POST'
          }).then(() => {
            wx.showToast({ title: '已兑换', icon: 'success' });
            this.loadData();
          });
        }
      }
    });
  },

  voidRecord(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认作废',
      content: '作废后不可恢复',
      success: res => {
        if (res.confirm) {
          app.request({
            url: '/admin/lottery-record/' + id + '/void',
            method: 'POST'
          }).then(() => {
            wx.showToast({ title: '已作废', icon: 'success' });
            this.loadData();
          });
        }
      }
    });
  },

  exchangePoints(e) {
    const userId = e.currentTarget.dataset.userid;
    const name = e.currentTarget.dataset.name;
    const total = e.currentTarget.dataset.total;
    wx.showModal({
      title: '积分兑换',
      content: name + ' 累计' + total + '积分，确认兑换？兑换后所有积分奖品标记为已兑换。',
      success: res => {
        if (res.confirm) {
          app.request({
            url: '/admin/points-exchange',
            method: 'POST',
            data: { userId: userId }
          }).then(data => {
            if (data === null) return;
            wx.showToast({ title: '兑换成功', icon: 'success' });
            this.loadData();
          });
        }
      }
    });
  }
});
