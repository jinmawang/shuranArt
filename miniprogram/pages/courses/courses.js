const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    courses: [],
    categories: ['全部', '素描', '水彩', '油画', '国画', '手工'],
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
    const { currentCategory } = this.data;
    let url = '/course/list';
    if (currentCategory && currentCategory !== '全部') {
      url += '?category=' + encodeURIComponent(currentCategory);
    }

    app.request({
      url: url,
      noAuth: true
    }).then(data => {
      this.setData({ courses: data || [] });
    });
  },

  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ currentCategory: category });
    this.loadCourses();
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/course-detail/course-detail?id=' + id
    });
  }
});
