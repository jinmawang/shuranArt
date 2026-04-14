// CRS: 课程列表页
// 需求来源: EARS-CRS-001, EARS-CRS-002, AC-CRS-001, AC-CRS-002
// 参照 teachers/teachers.js 模式
const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    courses: [],
    categories: ['全部', '素描', '水彩', '油画', '国画'],
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

  // EP-01: 加载课程列表 (EARS-CRS-001)
  loadCourses() {
    const { currentCategory } = this.data;
    let url = '/course/list';
    // EARS-CRS-002: 分类筛选
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

  // AC-CRS-002: 分类筛选切换
  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ currentCategory: category });
    this.loadCourses();
  },

  // AC-CRS-003: 点击课程跳转详情
  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/course-detail/course-detail?id=' + id
    });
  },

  // B1: 价格格式化 -- 0 显示"免费"，其他格式化为千分位
  formatPrice(price) {
    if (price === 0) return '免费';
    return price.toLocaleString();
  }
});
