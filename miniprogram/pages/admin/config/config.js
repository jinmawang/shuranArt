const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    config: {},
    markers: [],
    galleryList: [],
    studentWorksList: []
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
  },

  loadConfig() {
    app.request({
      url: '/admin/config'
    }).then(data => {
      const markers = [];
      if (data.studio_latitude && data.studio_longitude) {
        markers.push({
          id: 1,
          latitude: parseFloat(data.studio_latitude),
          longitude: parseFloat(data.studio_longitude),
          title: data.studio_name || '画室位置',
          iconPath: '/images/icon-marker.png',
          width: 30,
          height: 30
        });
      }

      // 解析画室日常和学生作品
      var galleryList = [];
      var studentWorksList = [];
      try {
        if (data.studio_gallery) galleryList = JSON.parse(data.studio_gallery);
      } catch (e) {}
      try {
        if (data.student_works) studentWorksList = JSON.parse(data.student_works);
      } catch (e) {}

      this.setData({
        config: data || {},
        markers: markers,
        galleryList: galleryList,
        studentWorksList: studentWorksList
      });
    });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail.value;
    this.setData({ [`config.${key}`]: value });
  },

  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const markers = [{
          id: 1,
          latitude: res.latitude,
          longitude: res.longitude,
          title: res.name || '画室位置',
          iconPath: '/images/icon-marker.png',
          width: 30,
          height: 30
        }];
        this.setData({
          'config.studio_address': res.address + (res.name ? ' ' + res.name : ''),
          'config.studio_latitude': res.latitude.toString(),
          'config.studio_longitude': res.longitude.toString(),
          markers: markers
        });
      },
      fail: (err) => {
        if (err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要授权位置权限才能选择地址',
            confirmText: '去设置',
            success: (res) => { if (res.confirm) wx.openSetting(); }
          });
        }
      }
    });
  },

  chooseQrcode() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ 'config.studio_qrcode': url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => { wx.hideLoading(); });
      }
    });
  },

  // 画室日常照片管理
  addGallery() {
    const list = this.data.galleryList;
    const remaining = 20 - list.length;
    if (remaining <= 0) return;
    wx.chooseMedia({
      count: Math.min(remaining, 9),
      mediaType: ['image'],
      success: (res) => {
        wx.showLoading({ title: '上传中...' });
        const uploads = res.tempFiles.map(f => app.uploadImage(f.tempFilePath));
        Promise.all(uploads).then(urls => {
          wx.hideLoading();
          const newList = list.concat(urls);
          this.setData({ galleryList: newList, 'config.studio_gallery': JSON.stringify(newList) });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => { wx.hideLoading(); wx.showToast({ title: '上传失败', icon: 'none' }); });
      }
    });
  },

  removeGallery(e) {
    const list = this.data.galleryList;
    list.splice(e.currentTarget.dataset.index, 1);
    this.setData({ galleryList: list, 'config.studio_gallery': JSON.stringify(list) });
  },

  // 学生作品照片管理
  addStudentWork() {
    const list = this.data.studentWorksList;
    const remaining = 20 - list.length;
    if (remaining <= 0) return;
    wx.chooseMedia({
      count: Math.min(remaining, 9),
      mediaType: ['image'],
      success: (res) => {
        wx.showLoading({ title: '上传中...' });
        const uploads = res.tempFiles.map(f => app.uploadImage(f.tempFilePath));
        Promise.all(uploads).then(urls => {
          wx.hideLoading();
          const newList = list.concat(urls);
          this.setData({ studentWorksList: newList, 'config.student_works': JSON.stringify(newList) });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => { wx.hideLoading(); wx.showToast({ title: '上传失败', icon: 'none' }); });
      }
    });
  },

  removeStudentWork(e) {
    const list = this.data.studentWorksList;
    list.splice(e.currentTarget.dataset.index, 1);
    this.setData({ studentWorksList: list, 'config.student_works': JSON.stringify(list) });
  },

  saveConfig() {
    const configData = { ...this.data.config };
    wx.showLoading({ title: '保存中...' });
    app.request({
      url: '/admin/config',
      method: 'POST',
      data: configData
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
    }).catch(() => { wx.hideLoading(); });
  }
});
