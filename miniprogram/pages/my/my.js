const app = getApp();

// 生成随机头像URL（基于用户ID，保证同一用户头像固定）
function getRandomAvatar(userId) {
  // 使用 DiceBear API 生成可爱的随机头像
  const styles = ['fun-emoji', 'adventurer', 'avataaars', 'bottts', 'lorelei'];
  const styleIndex = userId ? (userId % styles.length) : 0;
  const style = styles[styleIndex];
  return `https://api.dicebear.com/7.x/${style}/png?seed=${userId || 'default'}&size=150`;
}

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    userInfo: {},
    records: [],
    tempAvatarUrl: '',
    tempNickName: '',
    defaultAvatar: '',
    isAdmin: false
  },

  onLoad() {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
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
      // 如果用户没有设置头像，生成随机头像
      const defaultAvatar = getRandomAvatar(data.id);
      this.setData({
        userInfo: data,
        defaultAvatar: defaultAvatar
      });
      app.globalData.userInfo = data;
    });

    // 获取中奖记录
    app.request({
      url: '/lottery/records'
    }).then(data => {
      this.setData({ records: (data || []).slice(0, 5) });
    });

    // 检查管理员权限
    app.request({
      url: '/user/is-admin'
    }).then(data => {
      this.setData({ isAdmin: data && data.isAdmin });
    }).catch(() => {});
  },

  // 选择头像
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    this.setData({ tempAvatarUrl: avatarUrl });
    // 上传到服务器获取永久URL
    app.uploadImage(avatarUrl).then(url => {
      this.updateUserInfo({ avatarUrl: url });
    }).catch(() => {
      wx.showToast({ title: '头像上传失败', icon: 'none' });
    });
  },

  // 输入昵称
  onInputNickname(e) {
    this.setData({ tempNickName: e.detail.value });
  },

  // 昵称输入完成
  onNicknameBlur(e) {
    const nickName = e.detail.value;
    if (nickName && nickName.trim()) {
      this.updateUserInfo({ nickName: nickName.trim() });
    }
  },

  // 更新用户信息
  updateUserInfo(info) {
    app.request({
      url: '/user/update',
      method: 'PUT',
      data: info
    }).then(() => {
      wx.showToast({ title: '更新成功', icon: 'success' });
      this.fetchData();
    }).catch(() => {
      wx.showToast({ title: '更新失败', icon: 'none' });
    });
  },

  goToShare() {
    wx.navigateTo({
      url: '/pages/share/share'
    });
  },

  goToAdmin() {
    wx.navigateTo({
      url: '/pages/admin/index/index'
    });
  }
});
