const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    courses: [],
    allCourses: [],
    categories: ['全部'],
    currentCategory: '全部'
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
    this.loadCourses();
  },

  loadCourses() {
    app.request({
      url: '/course/list',
      noAuth: true
    }).then(data => {
      const all = data || [];
      // 从课程列表中动态提取类别
      const catSet = new Set();
      all.forEach(c => { if (c.category) catSet.add(c.category); });
      const categories = ['全部', ...Array.from(catSet)];
      this.setData({
        allCourses: all,
        courses: all,
        categories: categories
      });
    });
  },

  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category;
    const filtered = category === '全部'
      ? this.data.allCourses
      : this.data.allCourses.filter(c => c.category === category);
    this.setData({ currentCategory: category, courses: filtered });
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/course-detail/course-detail?id=' + id
    });
  }
});
