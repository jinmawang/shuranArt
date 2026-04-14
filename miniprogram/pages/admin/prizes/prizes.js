const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    prizes: [],
    showModal: false,
    editing: {},
    typeOptions: [
      { label: '积分', value: 'points' },
      { label: '体验课', value: 'experience' },
      { label: '礼品', value: 'gift' }
    ],
    typeIndex: 0,
    typeMap: {
      'points': '积分',
      'experience': '体验课',
      'gift': '礼品'
    }
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
    this.loadPrizes();
  },

  loadPrizes() {
    app.request({
      url: '/admin/prizes'
    }).then(data => {
      this.setData({ prizes: data || [] });
    });
  },

  addPrize() {
    this.setData({
      showModal: true,
      editing: {
        type: 'points',
        value: 0,
        probability: 0,
        stock: -1,
        needClaim: 0,
        status: 1
      },
      typeIndex: 0
    });
  },

  editPrize(e) {
    const item = e.currentTarget.dataset.item;
    const typeIndex = this.data.typeOptions.findIndex(t => t.value === item.type);
    this.setData({
      showModal: true,
      editing: { ...item },
      typeIndex: typeIndex >= 0 ? typeIndex : 0
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

  chooseIcon() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ 'editing.icon': url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
  },

  onTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      typeIndex: index,
      'editing.type': this.data.typeOptions[index].value
    });
  },

  onNeedClaimChange(e) {
    this.setData({ 'editing.needClaim': e.detail.value ? 1 : 0 });
  },

  onStatusChange(e) {
    this.setData({ 'editing.status': e.detail.value ? 1 : 0 });
  },

  savePrize() {
    const { editing } = this.data;
    if (!editing.name) {
      wx.showToast({ title: '请输入名称', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/prize',
      method: 'POST',
      data: {
        ...editing,
        probability: parseInt(editing.probability) || 0,
        stock: parseInt(editing.stock) || -1,
        value: parseInt(editing.value) || 0
      }
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.closeModal();
      this.loadPrizes();
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deletePrize(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个奖品吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/prize/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadPrizes();
          });
        }
      }
    });
  }
});
