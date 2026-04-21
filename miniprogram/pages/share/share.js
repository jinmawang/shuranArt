const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activity: {},
    userInfo: {},
    activityId: null,
    shareStatus: {},
    shareCode: null,
    isFromShare: false
  },

  goBack() {
    wx.navigateBack();
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  onLoad(options) {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight,
      isFromShare: getCurrentPages().length === 1
    });
    if (options.activityId) {
      this.setData({ activityId: options.activityId });
      this.loadActivity(options.activityId);
    } else {
      this.loadDefaultActivity();
    }

    // 处理来自分享链接的访问
    if (options.shareCode) {
      // 新的分享确认机制
      this.confirmShare(options.shareCode);
    } else if (options.sharerId && options.activityId) {
      // 兼容旧的分享机制
      this.recordShare(options.sharerId, options.activityId);
    }
  },

  onShow() {
    this.loadUserInfo();
    // 刷新分享状态
    if (this.data.activityId) {
      this.loadShareStatus(this.data.activityId);
    }
  },

  loadUserInfo() {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.fetchUserInfo();
      });
    } else {
      this.fetchUserInfo();
    }
  },

  fetchUserInfo() {
    app.request({
      url: '/user/info'
    }).then(data => {
      this.setData({ userInfo: data });
    });
  },

  loadActivity(id) {
    app.request({
      url: '/activity/' + id,
      noAuth: true
    }).then(data => {
      this.setData({ activity: data || {} });
      this.loadShareStatus(id);
    });
  },

  loadDefaultActivity() {
    app.request({
      url: '/activity/list',
      noAuth: true
    }).then(data => {
      if (data && data.length > 0) {
        // 只选择已开始的活动
        const activeActivity = data.find(a => a.started) || data[0];
        this.setData({
          activity: activeActivity,
          activityId: activeActivity.id
        });
        this.loadShareStatus(activeActivity.id);
      }
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

  // 新的分享确认机制
  confirmShare(shareCode) {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.doConfirmShare(shareCode);
      });
    } else {
      this.doConfirmShare(shareCode);
    }
  },

  doConfirmShare(shareCode) {
    app.request({
      url: '/share/confirm',
      method: 'POST',
      data: { shareCode: shareCode }
    }).then(result => {
      if (result.success) {
        wx.showToast({
          title: result.lotteryAdded ? '感谢助力！' : result.msg,
          icon: result.lotteryAdded ? 'success' : 'none'
        });
      } else {
        wx.showToast({
          title: result.msg,
          icon: 'none'
        });
      }
    });
  },

  // 兼容旧的分享记录方法
  recordShare(sharerId, activityId) {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.doRecordShare(sharerId, activityId);
      });
    } else {
      this.doRecordShare(sharerId, activityId);
    }
  },

  doRecordShare(sharerId, activityId) {
    app.request({
      url: '/share/record',
      method: 'POST',
      data: {
        sharerId: parseInt(sharerId),
        activityId: parseInt(activityId)
      }
    }).then(result => {
      if (result.lotteryAdded) {
        wx.showToast({
          title: '感谢助力！',
          icon: 'success'
        });
      } else {
        wx.showToast({
          title: result.msg,
          icon: 'none'
        });
      }
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
      data: { activityId: this.data.activityId }
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

  showLimitTip() {
    wx.showToast({
      title: '该活动分享次数已达上限',
      icon: 'none'
    });
  },

  onShareAppMessage() {
    const userId = this.data.userInfo.id || app.globalData.userInfo?.id;
    const activityId = this.data.activityId;
    const activity = this.data.activity;

    // 先创建分享记录获取分享码
    this.createShareCode();

    return {
      title: activity.shareTitle || activity.title || '快来参加活动吧！',
      path: `/pages/share/share?sharerId=${userId}&activityId=${activityId}&shareCode=${this.data.shareCode || ''}`,
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  }
});
