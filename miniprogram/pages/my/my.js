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
    // 上传头像到云存储（这里简化处理，实际应上传到云存储获取永久URL）
    // 微信小程序的临时文件路径可以直接展示，但需要上传到服务器才能持久化
    wx.showToast({ title: '头像已更新', icon: 'success' });
    // 如需持久化，需要上传到云存储后调用 this.updateUserInfo({ avatarUrl: permanentUrl })
  },

  // 上传头像到服务器（预留接口）
  uploadAvatar(tempPath) {
    // TODO: 上传到腾讯云COS或微信云存储获取永久URL
    // wx.cloud.uploadFile 或 wx.uploadFile
    // 获取永久URL后调用 updateUserInfo
    this.updateUserInfo({ avatarUrl: tempPath });
  },

  // 使用随机头像
  useRandomAvatar() {
    const randomAvatar = this.data.defaultAvatar;
    if (randomAvatar) {
      this.updateUserInfo({ avatarUrl: randomAvatar });
    }
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

  // 显示编辑资料提示
  showEditProfile() {
    wx.showToast({ title: '点击头像可更换', icon: 'none' });
  },

  goToRecords() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none'
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
