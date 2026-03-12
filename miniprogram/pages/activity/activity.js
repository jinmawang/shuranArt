const app = getApp();

Page({
  data: {
    activity: {},
    shareCode: null,
    shareStatus: null
  },

  onLoad(options) {
    if (options.id) {
      this.loadActivity(options.id);
    }
  },

  onShow() {
    // 刷新分享状态
    if (this.data.activity.id) {
      this.loadShareStatus(this.data.activity.id);
    }
  },

  loadActivity(id) {
    app.request({
      url: '/activity/' + id,
      noAuth: true
    }).then(data => {
      this.setData({ activity: data || {} });
      // 加载分享状态
      this.loadShareStatus(id);
    });
  },

  loadShareStatus(activityId) {
    if (!app.globalData.token) return;
    app.request({
      url: '/share/status',
      data: { activityId: activityId }
    }).then(data => {
      this.setData({ shareStatus: data });
    });
  },

  goToLottery() {
    wx.switchTab({
      url: '/pages/lottery/lottery'
    });
  },

  showNotStartedTip() {
    wx.showToast({
      title: '活动即将开始，敬请期待',
      icon: 'none'
    });
  },

  // 创建分享并获取分享码
  createShareCode() {
    return new Promise((resolve, reject) => {
      if (!app.globalData.token) {
        app.login().then(() => {
          this.doCreateShare(resolve, reject);
        }).catch(reject);
      } else {
        this.doCreateShare(resolve, reject);
      }
    });
  },

  doCreateShare(resolve, reject) {
    app.request({
      url: '/share/create',
      method: 'POST',
      data: { activityId: this.data.activity.id }
    }).then(data => {
      if (data.success) {
        this.setData({ shareCode: data.shareCode });
        resolve(data);
      } else {
        wx.showToast({ title: data.msg, icon: 'none' });
        reject(new Error(data.msg));
      }
    }).catch(reject);
  },

  onShareAppMessage() {
    const activity = this.data.activity;
    const userId = app.globalData.userInfo?.id;

    // 先创建分享记录获取分享码
    this.createShareCode();

    // 使用活动自定义的分享标题和图片
    return {
      title: activity.shareTitle || activity.title || '快来参加活动吧！',
      path: `/pages/share/share?sharerId=${userId}&activityId=${activity.id}&shareCode=${this.data.shareCode || ''}`,
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  }
});
