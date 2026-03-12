const app = getApp();

Page({
  data: {
    config: {},
    studioImages: [],
    markers: []
  },

  onLoad() {
    this.loadConfig();
  },

  loadConfig() {
    app.request({
      url: '/admin/config'
    }).then(data => {
      // 解析轮播图
      let studioImages = [];
      if (data.studio_images) {
        try {
          studioImages = JSON.parse(data.studio_images);
        } catch (e) {
          studioImages = [];
        }
      }

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
        studioImages: studioImages,
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
        // 实际项目中应该上传到云存储获取永久URL
        this.setData({ 'config.studio_qrcode': tempFilePath });
        wx.showToast({ title: '已选择', icon: 'success' });
      }
    });
  },

  // 添加轮播图
  addStudioImage() {
    const remaining = 9 - this.data.studioImages.length;
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      success: (res) => {
        const newImages = res.tempFiles.map(f => f.tempFilePath);
        const studioImages = [...this.data.studioImages, ...newImages];
        this.setData({ studioImages });
        wx.showToast({ title: '已添加', icon: 'success' });
      }
    });
  },

  // 删除轮播图
  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这张图片吗？',
      success: (res) => {
        if (res.confirm) {
          const studioImages = [...this.data.studioImages];
          studioImages.splice(index, 1);
          this.setData({ studioImages });
        }
      }
    });
  },

  saveConfig() {
    // 将轮播图数组转为JSON字符串
    const configData = {
      ...this.data.config,
      studio_images: JSON.stringify(this.data.studioImages)
    };

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
