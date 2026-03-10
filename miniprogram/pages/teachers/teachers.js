const app = getApp();

Page({
  data: {
    teachers: []
  },

  onLoad() {
    this.loadTeachers();
  },

  loadTeachers() {
    app.request({
      url: '/teacher/list',
      noAuth: true
    }).then(data => {
      this.setData({ teachers: data || [] });
    });
  },

  previewWork(e) {
    const { works, current } = e.currentTarget.dataset;
    wx.previewImage({
      urls: works,
      current: current
    });
  }
});
