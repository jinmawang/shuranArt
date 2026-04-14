const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    config: {},
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
  },

  loadConfig() {
    app.request({
      url: '/admin/config'
    }).then(data => {
      // 设置地图标记
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

      this.setData({
        config: data || {},
        markers: markers
      });
    });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail.value;
    this.setData({
      [`config.${key}`]: value
    });
  },

  // 选择地址
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
            success: (res) => {
              if (res.confirm) {
                wx.openSetting();
              }
            }
          });
        }
      }
    });
  },

  // 选择二维码
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
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
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
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      });
    }).catch(() => {
      wx.hideLoading();
    });
  }
});
