const app = getApp();

Page({
  data: {
    config: {}
  },

  onLoad() {
    this.loadConfig();
  },

  loadConfig() {
    app.request({
      url: '/admin/config'
    }).then(data => {
      this.setData({ config: data || {} });
    });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail.value;
    this.setData({
      [`config.${key}`]: value
    });
  },

  saveConfig() {
    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/config',
      method: 'POST',
      data: this.data.config
    }).then(() => {
      wx.hideLoading();
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      });
    }).catch(() => {
      wx.hideLoading();
    });
  }
});
