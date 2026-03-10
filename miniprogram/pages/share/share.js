const app = getApp();

Page({
  data: {
    activity: {},
    userInfo: {},
    activityId: null
  },

  onLoad(options) {
    if (options.activityId) {
      this.setData({ activityId: options.activityId });
      this.loadActivity(options.activityId);
    } else {
      this.loadDefaultActivity();
    }

    // 处理来自分享链接的访问
    if (options.sharerId && options.activityId) {
      this.recordShare(options.sharerId, options.activityId);
    }
  },

  onShow() {
    this.loadUserInfo();
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
    });
  },

  loadDefaultActivity() {
    app.request({
      url: '/activity/list',
      noAuth: true
    }).then(data => {
      if (data && data.length > 0) {
        this.setData({
          activity: data[0],
          activityId: data[0].id
        });
      }
    });
  },

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

  onShareAppMessage() {
    const userId = this.data.userInfo.id || app.globalData.userInfo?.id;
    const activityId = this.data.activityId;
    const activity = this.data.activity;

    return {
      title: activity.title || '快来参加活动吧！',
      path: `/pages/share/share?sharerId=${userId}&activityId=${activityId}`,
      imageUrl: activity.coverImg || '/images/share-default.png'
    };
  }
});
