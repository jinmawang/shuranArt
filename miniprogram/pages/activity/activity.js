const app = getApp();

Page({
  data: {
    activity: {}
  },

  onLoad(options) {
    if (options.id) {
      this.loadActivity(options.id);
    }
  },

  loadActivity(id) {
    app.request({
      url: '/activity/' + id,
      noAuth: true
    }).then(data => {
      this.setData({ activity: data || {} });
    });
  },

  goToLottery() {
    wx.switchTab({
      url: '/pages/lottery/lottery'
    });
  },

  onShareAppMessage() {
    const activity = this.data.activity;
    const userId = app.globalData.userInfo?.id;

    return {
      title: activity.title || '快来参加活动吧！',
      path: `/pages/share/share?sharerId=${userId}&activityId=${activity.id}`,
      imageUrl: activity.coverImg || '/images/share-default.png'
    };
  }
});
