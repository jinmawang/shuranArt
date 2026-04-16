const app = getApp();

Page({
  data: {
    token: '',
    status: 'loading', // loading, ready, success, error
    errorMsg: ''
  },

  onLoad(options) {
    if (!options.token) {
      this.setData({ status: 'error', errorMsg: '邀请链接无效' });
      return;
    }
    this.setData({ token: options.token, status: 'ready' });
  },

  acceptInvite() {
    this.setData({ status: 'loading' });
    app.request({
      url: '/invite/accept',
      method: 'POST',
      data: { token: this.data.token }
    }).then(() => {
      this.setData({ status: 'success' });
      wx.showToast({ title: '已成为管理员', icon: 'success' });
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/admin/index/index' });
      }, 1500);
    }).catch(err => {
      this.setData({
        status: 'error',
        errorMsg: (err && err.msg) || '接受邀请失败'
      });
    });
  }
});
