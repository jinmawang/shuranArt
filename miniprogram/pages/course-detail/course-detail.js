const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    course: null,
    loading: true
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
    if (options.id) {
      this.loadCourse(options.id);
    }
  },

  loadCourse(id) {
    app.request({
      url: '/course/' + id,
      noAuth: true
    }).then(data => {
      this.setData({
        course: data,
        loading: false
      });
      if (data && data.name) {
        wx.setNavigationBarTitle({ title: data.name });
      }
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  onImageError() {
    this.setData({
      'course.coverImg': '/images/default-course.png'
    });
  }
});
