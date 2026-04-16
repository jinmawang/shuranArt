const app = getApp();

// 内置课程数据
const BUILTIN_COURSES = [
  {
    id: 'builtin_1',
    name: '创意儿童画',
    category: '创意',
    description: '适合3-10岁儿童，通过自由绘画、线条探索、色彩实验等方式激发想象力和色彩感知，培养创意思维和艺术兴趣。',
    duration: '1小时/次',
    suitableFor: '3-10岁',
    coverImg: 'https://images.pexels.com/photos/8612991/pexels-photo-8612991.jpeg?auto=compress&cs=tinysrgb&w=600',
    builtin: true
  },
  {
    id: 'builtin_2',
    name: '素描基础',
    category: '素描',
    description: '从几何体入门，系统学习透视原理、明暗关系、线条运用等素描基本功，为绘画打下扎实基础。',
    duration: '1.5小时/次',
    suitableFor: '7-12岁',
    coverImg: 'https://images.pexels.com/photos/159644/art-supplies-brushes-rulers-702137.jpeg?auto=compress&cs=tinysrgb&w=600',
    builtin: true
  },
  {
    id: 'builtin_3',
    name: '水彩画',
    category: '水彩',
    description: '学习水彩颜料特性、湿画法与干画法技巧、色彩搭配原理，创作色彩缤纷的水彩作品。',
    duration: '1.5小时/次',
    suitableFor: '5-12岁',
    coverImg: 'https://images.pexels.com/photos/1153895/pexels-photo-1153895.jpeg?auto=compress&cs=tinysrgb&w=600',
    builtin: true
  },
  {
    id: 'builtin_4',
    name: '国画',
    category: '国画',
    description: '学习毛笔特性与基本笔法，了解中国传统绘画技巧，从花鸟鱼虫入手感受水墨之美。',
    duration: '1.5小时/次',
    suitableFor: '6-15岁',
    coverImg: 'https://images.pexels.com/photos/6941365/pexels-photo-6941365.jpeg?auto=compress&cs=tinysrgb&w=600',
    builtin: true
  }
];

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    builtinCourses: [],
    customCourses: [],
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
    this.loadData();
  },

  loadData() {
    // 设置内置课程
    this.setData({ builtinCourses: BUILTIN_COURSES });

    // 加载自定义课程
    app.request({
      url: '/course/list',
      noAuth: true
    }).then(data => {
      this.setData({ customCourses: data || [] });
      this.buildCategories();
    });
  },

  buildCategories() {
    var cats = new Set(['全部']);
    BUILTIN_COURSES.forEach(c => cats.add(c.category));
    this.data.customCourses.forEach(c => { if (c.category) cats.add(c.category); });
    this.setData({ categories: Array.from(cats) });
  },

  onCategoryTap(e) {
    const category = e.currentTarget.dataset.category;
    this.setData({ currentCategory: category });
  },

  getFilteredBuiltin() {
    var cat = this.data.currentCategory;
    if (cat === '全部') return this.data.builtinCourses;
    return this.data.builtinCourses.filter(c => c.category === cat);
  },

  getFilteredCustom() {
    var cat = this.data.currentCategory;
    if (cat === '全部') return this.data.customCourses;
    return this.data.customCourses.filter(c => c.category === cat);
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (typeof id === 'string' && id.startsWith('builtin_')) return;
    wx.navigateTo({
      url: '/pages/course-detail/course-detail?id=' + id
    });
  },

  showBuiltinDetail(e) {
    var index = e.currentTarget.dataset.index;
    var course = this.getFilteredBuiltin()[index];
    if (!course) return;
    wx.showModal({
      title: course.name,
      content: course.description + '\n\n适合年龄：' + course.suitableFor + '\n课时：' + course.duration,
      showCancel: false,
      confirmText: '知道了'
    });
  }
});
