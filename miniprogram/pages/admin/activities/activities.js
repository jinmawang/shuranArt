const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activities: [],
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
    this.loadActivities();
  },

  loadActivities() {
    app.request({
      url: '/admin/activities'
    }).then(data => {
      this.setData({ activities: data || [] });
    });
  },

  addActivity() {
    this.setData({
      showModal: true,
      editing: { totalShareLimit: 5, status: 1 }
    });
  },

  editActivity(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editing: {
        ...item,
        startDate: item.startTime ? item.startTime.split(' ')[0] : '',
        endDate: item.endTime ? item.endTime.split(' ')[0] : ''
      }
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

  onStartDateChange(e) {
    this.setData({ 'editing.startDate': e.detail.value });
  },

  onEndDateChange(e) {
    this.setData({ 'editing.endDate': e.detail.value });
  },

  onStatusChange(e) {
    this.setData({ 'editing.status': e.detail.value ? 1 : 0 });
  },

  // 选择封面图片
  chooseCoverImage() {
    this.chooseAndUploadImage('coverImg');
  },

  // 选择分享图片
  chooseShareImage() {
    this.chooseAndUploadImage('shareImage');
  },

  // 选择并上传图片到服务器
  chooseAndUploadImage(key) {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ [`editing.${key}`]: url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
  },

  saveActivity() {
    const { editing } = this.data;
    if (!editing.title) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      startTime: editing.startDate ? editing.startDate + ' 00:00:00' : null,
      endTime: editing.endDate ? editing.endDate + ' 23:59:59' : null,
      totalShareLimit: parseInt(editing.totalShareLimit) || 5
    };

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/activity',
      method: 'POST',
      data: data
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.closeModal();
      this.loadActivities();
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deleteActivity(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个活动吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/activity/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadActivities();
          });
        }
      }
    });
  }
});
