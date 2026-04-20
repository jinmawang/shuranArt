const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    works: [],
    page: 1,
    hasMore: true,
    loading: false,
    shareText: '快来看我在书染美术的作品～',
    shareWork: null
  },

  onLoad() {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    this.loadShareText();
  },

  onShow() {
    // 每次显示时重新加载（tab页切换回来也刷新）
    this.setData({ works: [], page: 1, hasMore: true });
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

  loadShareText() {
    app.request({
      url: '/studio/config',
      noAuth: true
    }).then(data => {
      if (data && data.work_share_text) {
        this.setData({ shareText: data.work_share_text });
      }
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
  },

  setShareWork(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    this.setData({ shareWork: this.data.works[index] });
  },

  onShareAppMessage() {
    const work = this.data.shareWork;
    if (work) {
      return {
        title: this.data.shareText,
        path: '/pages/works/timeline?childId=' + work.childId,
        imageUrl: work.imageUrl
      };
    }
    return {
      title: this.data.shareText,
      path: '/pages/works/works'
    };
  },

  onShareTimeline() {
    const work = this.data.shareWork;
    return {
      title: this.data.shareText,
      imageUrl: work ? work.imageUrl : ''
    };
  }
});
