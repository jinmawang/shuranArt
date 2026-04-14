const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    banners: [],
    showModal: false,
    editing: {}
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
    this.loadBanners();
  },

  loadBanners() {
    app.request({
      url: '/admin/banners'
    }).then(data => {
      this.setData({ banners: data || [] });
    });
  },

  addBanner() {
    if (this.data.banners.length >= 5) {
      wx.showToast({ title: '最多添加5张轮播图', icon: 'none' });
      return;
    }
    this.setData({
      showModal: true,
      editing: { sortOrder: 0, status: 1 }
    });
  },

  editBanner(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editing: { ...item }
    });
  },

  closeModal() {
    this.setData({ showModal: false, editing: {} });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    const value = e.detail.value;
    this.setData({ [`editing.${key}`]: value });
  },

  onStatusChange(e) {
    this.setData({ 'editing.status': e.detail.value ? 1 : 0 });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ 'editing.imageUrl': url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
  },

  saveBanner() {
    const { editing } = this.data;
    if (!editing.imageUrl) {
      wx.showToast({ title: '请选择图片', icon: 'none' });
      return;
    }
    if (editing.description && editing.description.length > 20) {
      wx.showToast({ title: '描述不能超过20字', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      sortOrder: parseInt(editing.sortOrder) || 0
    };

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/banner',
      method: 'POST',
      data: data
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.closeModal();
      this.loadBanners();
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deleteBanner(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这张轮播图吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/banner/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadBanners();
          });
        }
      }
    });
  }
});
