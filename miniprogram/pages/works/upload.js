const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    children: [],
    selectedChildId: null,
    imageUrl: '',
    description: '',
    showAddChild: false,
    newChildName: ''
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
    this.loadChildren();
  },

  loadChildren() {
    app.request({ url: '/works/children' }).then(data => {
      const children = data || [];
      this.setData({
        children: children,
        selectedChildId: children.length > 0 ? children[0].id : null
      });
    });
  },

  selectChild(e) {
    this.setData({ selectedChildId: e.currentTarget.dataset.id });
  },

  showAddChildModal() {
    this.setData({ showAddChild: true, newChildName: '' });
  },

  closeAddChild() {
    this.setData({ showAddChild: false });
  },

  onChildNameInput(e) {
    this.setData({ newChildName: e.detail.value });
  },

  addChild() {
    const name = this.data.newChildName.trim();
    if (!name) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }
    app.request({
      url: '/works/child',
      method: 'POST',
      data: { name: name }
    }).then(child => {
      this.setData({ showAddChild: false, selectedChildId: child.id });
      this.loadChildren();
      wx.showToast({ title: '添加成功', icon: 'success' });
    });
  },

  chooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: res => {
        const tempPath = res.tempFiles[0].tempFilePath;
        app.uploadImage(tempPath).then(url => {
          this.setData({ imageUrl: url });
        }).catch(() => {
          wx.showToast({ title: '上传失败', icon: 'none' });
        });
      }
    });
  },

  onDescInput(e) {
    this.setData({ description: e.detail.value });
  },

  submit() {
    if (!this.data.selectedChildId) {
      wx.showToast({ title: '请选择或添加孩子', icon: 'none' });
      return;
    }
    if (!this.data.imageUrl) {
      wx.showToast({ title: '请上传作品图片', icon: 'none' });
      return;
    }
    app.request({
      url: '/works/upload',
      method: 'POST',
      data: {
        childId: this.data.selectedChildId,
        imageUrl: this.data.imageUrl,
        description: this.data.description
      }
    }).then(() => {
      wx.showToast({ title: '提交成功，等待审核', icon: 'success' });
      setTimeout(() => { wx.navigateBack(); }, 1500);
    }).catch(err => {
      wx.showToast({ title: (err && err.msg) || '提交失败', icon: 'none' });
    });
  }
});
