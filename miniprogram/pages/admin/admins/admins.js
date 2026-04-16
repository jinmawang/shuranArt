const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    admins: [],
    showShareModal: false,
    inviteToken: ''
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
    this.loadAdmins();
  },

  loadAdmins() {
    app.request({
      url: '/admin/admins'
    }).then(data => {
      this.setData({ admins: data || [] });
    });
  },

  generateInvite() {
    app.request({
      url: '/admin/invite',
      method: 'POST'
    }).then(data => {
      this.setData({
        inviteToken: data.token,
        showShareModal: true
      });
    }).catch(() => {
      wx.showToast({ title: '生成邀请失败', icon: 'none' });
    });
  },

  closeShareModal() {
    this.setData({ showShareModal: false });
  },

  onShareAppMessage() {
    return {
      title: '邀请你成为书染美术管理员',
      path: '/pages/admin-invite/admin-invite?token=' + this.data.inviteToken
    };
  },

  deleteAdmin(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    wx.showModal({
      title: '确认删除',
      content: '确定要移除管理员"' + name + '"吗？',
      success: res => {
        if (res.confirm) {
          app.request({
            url: '/admin/admin/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.loadAdmins();
          }).catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' });
          });
        }
      }
    });
  }
});
