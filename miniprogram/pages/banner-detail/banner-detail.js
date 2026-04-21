const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    banner: {},
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
    if (options.id) {
      this.loadBanner(options.id);
    }
  },

  loadBanner(id) {
    app.request({
      url: '/banner/detail?id=' + id,
      noAuth: true
    }).then(data => {
      if (data) {
        this.setData({ banner: data });
      }
    });
  },

  previewImage() {
    if (this.data.banner.imageUrl) {
      wx.previewImage({ urls: [this.data.banner.imageUrl], current: this.data.banner.imageUrl });
    }
  },

  onShareAppMessage() {
    const b = this.data.banner;
    return {
      title: b.shareText || b.description || '书染美术',
      path: '/pages/banner-detail/banner-detail?id=' + b.id,
      imageUrl: b.imageUrl
    };
  },

  onShareTimeline() {
    const b = this.data.banner;
    return {
      title: b.shareText || b.description || '书染美术',
      imageUrl: b.imageUrl
    };
  }
});
