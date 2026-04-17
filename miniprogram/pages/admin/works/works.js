const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    currentTab: 0,
    pendingWorks: [],
    allWorks: [],
    shareText: '',
    intervalDays: '30',
    showConfig: false
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
    const tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ currentTab: tab });
  },

  loadData() {
    app.request({ url: '/admin/works/pending' }).then(data => {
      this.setData({ pendingWorks: data || [] });
    });
    app.request({ url: '/admin/works/all' }).then(data => {
      this.setData({ allWorks: data || [] });
    });
  },

  loadConfig() {
    app.request({ url: '/admin/config' }).then(data => {
      this.setData({
        shareText: data.work_share_text || '快来看我在书染美术的作品～',
        intervalDays: data.work_upload_interval_days || '30'
      });
    });
  },

  approveWork(e) {
    const id = e.currentTarget.dataset.id;
    app.request({ url: '/admin/works/' + id + '/approve', method: 'POST' }).then(() => {
      wx.showToast({ title: '已通过', icon: 'success' });
      this.loadData();
    });
  },

  rejectWork(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认拒绝',
      content: '确定拒绝该作品？',
      success: res => {
        if (res.confirm) {
          app.request({ url: '/admin/works/' + id + '/reject', method: 'POST' }).then(() => {
            wx.showToast({ title: '已拒绝', icon: 'success' });
            this.loadData();
          });
        }
      }
    });
  },

  deleteWork(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复',
      success: res => {
        if (res.confirm) {
          app.request({ url: '/admin/works/' + id, method: 'DELETE' }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadData();
          });
        }
      }
    });
  },

  toggleConfig() {
    this.setData({ showConfig: !this.data.showConfig });
  },

  onShareTextInput(e) {
    this.setData({ shareText: e.detail.value });
  },

  onIntervalInput(e) {
    this.setData({ intervalDays: e.detail.value });
  },

  saveConfig() {
    app.request({
      url: '/admin/config',
      method: 'POST',
      data: {
        work_share_text: this.data.shareText,
        work_upload_interval_days: this.data.intervalDays
      }
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' });
      this.setData({ showConfig: false });
    });
  },

  previewImage(e) {
    wx.previewImage({ urls: [e.currentTarget.dataset.src], current: e.currentTarget.dataset.src });
  }
});
