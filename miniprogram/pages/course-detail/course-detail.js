// CRS: 课程详情页
// 需求来源: EARS-CRS-003, AC-CRS-003
// 参照 activity/activity.js 模式（通过 ID 查询详情）
const app = getApp();

Page({
  data: {
    course: null,
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.loadCourse(options.id);
    }
  },

  // EP-02: 加载课程详情 (RM-002)
  loadCourse(id) {
    app.request({
      url: '/course/' + id,
      noAuth: true
    }).then(data => {
      this.setData({
        course: data,
        loading: false
      });
      // 设置页面标题为课程名称
      if (data && data.name) {
        wx.setNavigationBarTitle({ title: data.name });
      }
    }).catch(() => {
      this.setData({ loading: false });
    });
  },

  // B3: 封面图加载失败时显示占位图
  onImageError() {
    this.setData({
      'course.coverImg': '/images/default-course.png'
    });
  }
});
