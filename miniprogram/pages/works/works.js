const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    works: [],
    page: 1,
    hasMore: true,
    loading: false
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
    this.loadWorks();
  },

  loadWorks() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ loading: true });
    app.request({
      url: '/works/wall?page=' + this.data.page + '&size=20',
      noAuth: true
    }).then(data => {
      const works = data || [];
      this.setData({
        works: this.data.works.concat(works),
        page: this.data.page + 1,
        hasMore: works.length >= 20,
        loading: false
      });
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  onReachBottom() {
    this.loadWorks();
  },

  goToTimeline(e) {
    const childId = e.currentTarget.dataset.childid;
    wx.navigateTo({
      url: '/pages/works/timeline?childId=' + childId
    });
  },

  previewImage(e) {
    const src = e.currentTarget.dataset.src;
    wx.previewImage({ urls: [src], current: src });
  },

  goToUpload() {
    wx.navigateTo({ url: '/pages/works/upload' });
  }
});
