const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    child: {},
    works: [],
    shareText: ''
  },

  goBack() {
    wx.navigateBack();
  },

  onLoad(options) {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    if (options.childId) {
      this.loadTimeline(options.childId);
    }
  },

  loadTimeline(childId) {
    app.request({
      url: '/works/timeline/' + childId,
      noAuth: true
    }).then(data => {
      this.setData({
        child: data.child || {},
        works: data.works || [],
        shareText: data.shareText || '快来看我在书染美术的作品～'
      });
    });
  },

  previewImage(e) {
    const src = e.currentTarget.dataset.src;
    const urls = this.data.works.map(w => w.imageUrl);
    wx.previewImage({ urls: urls, current: src });
  },

  onShareAppMessage() {
    const child = this.data.child;
    const latestWork = this.data.works.length > 0 ? this.data.works[0] : null;
    return {
      title: this.data.shareText,
      path: '/pages/works/timeline?childId=' + child.id,
      imageUrl: latestWork ? latestWork.imageUrl : ''
    };
  }
});
