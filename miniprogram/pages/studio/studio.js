const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    currentTab: 0,
    studioConfig: {},
    teachers: [],
    gallery: [],
    studentWorks: [],
    latitude: null,
    longitude: null,
    markers: []
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
    this.loadConfig();
    this.loadTeachers();
  },

  switchTab(e) {
    this.setData({ currentTab: e.currentTarget.dataset.tab });
  },

  onTabSwipe(e) {
    this.setData({ currentTab: e.detail.current });
  },

  loadConfig() {
    app.request({
      url: '/studio/config',
      noAuth: true
    }).then(data => {
      var latitude = data.studio_latitude ? parseFloat(data.studio_latitude) : null;
      var longitude = data.studio_longitude ? parseFloat(data.studio_longitude) : null;
      var markers = [];
      if (latitude && longitude) {
        markers.push({
          id: 1,
          latitude: latitude,
          longitude: longitude,
          title: data.studio_name || '书染美术',
          iconPath: '/images/icon-marker.png',
          width: 40,
          height: 40
        });
      }

      // 解析画室日常和学生作品
      var gallery = [];
      var studentWorks = [];
      try {
        if (data.studio_gallery) gallery = JSON.parse(data.studio_gallery);
      } catch (e) {}
      try {
        if (data.student_works) studentWorks = JSON.parse(data.student_works);
      } catch (e) {}

      this.setData({
        studioConfig: data,
        latitude: latitude,
        longitude: longitude,
        markers: markers,
        gallery: gallery,
        studentWorks: studentWorks
      });
    });
  },

  loadTeachers() {
    app.request({
      url: '/teacher/list',
      noAuth: true
    }).then(data => {
      this.setData({ teachers: data || [] });
    });
  },

  openMap() {
    if (this.data.latitude && this.data.longitude) {
      wx.openLocation({
        latitude: this.data.latitude,
        longitude: this.data.longitude,
        name: this.data.studioConfig.studio_name || '书染美术',
        address: this.data.studioConfig.studio_address || '',
        scale: 16
      });
    }
  },

  copyWechatId(e) {
    var wechatId = e.currentTarget.dataset.wechat;
    if (!wechatId) return;
    wx.setClipboardData({
      data: wechatId,
      success: function() {
        wx.showToast({ title: '微信号已复制', icon: 'success' });
      }
    });
  },

  previewGallery(e) {
    wx.previewImage({
      urls: this.data.gallery,
      current: e.currentTarget.dataset.src
    });
  },

  previewStudentWork(e) {
    wx.previewImage({
      urls: this.data.studentWorks,
      current: e.currentTarget.dataset.src
    });
  },

  previewWork(e) {
    var works = e.currentTarget.dataset.works;
    var current = e.currentTarget.dataset.current;
    wx.previewImage({ urls: works, current: current });
  }
});
