const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    allChildren: [],
    approvedChildren: [],
    selectedChildId: null,
    imageUrl: '',
    description: '',
    showAddChild: false,
    newChildName: '',
    newChildReason: '',
    canAddChild: true
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
      const all = data || [];
      const approved = all.filter(c => c.status === 'approved');
      const pending = all.filter(c => c.status === 'pending');
      // 只有 pending + approved 计入名额，rejected 不占位
      const activeCount = approved.length + pending.length;
      this.setData({
        allChildren: all,
        approvedChildren: approved,
        selectedChildId: approved.length > 0 ? approved[0].id : null,
        canAddChild: activeCount < 3
      });
      // 提示有待审核的孩子
      if (pending.length > 0 && approved.length === 0) {
        wx.showToast({ title: '孩子正在审核中，请耐心等待', icon: 'none', duration: 2000 });
      }
    });
  },

  selectChild(e) {
    this.setData({ selectedChildId: e.currentTarget.dataset.id });
  },

  showAddChildModal() {
    this.setData({ showAddChild: true, newChildName: '', newChildReason: '' });
  },

  closeAddChild() {
    this.setData({ showAddChild: false });
  },

  onChildNameInput(e) {
    this.setData({ newChildName: e.detail.value });
  },

  onChildReasonInput(e) {
    this.setData({ newChildReason: e.detail.value });
  },

  addChild() {
    const name = this.data.newChildName.trim();
    const reason = this.data.newChildReason.trim();
    if (!name) {
      wx.showToast({ title: '请输入孩子姓名', icon: 'none' });
      return;
    }
    if (!reason) {
      wx.showToast({ title: '请填写申请理由', icon: 'none' });
      return;
    }
    app.request({
      url: '/works/child',
      method: 'POST',
      data: { name: name, reason: reason }
    }).then(data => {
      if (data === null) return;
      this.setData({ showAddChild: false });
      this.loadChildren();
      wx.showToast({ title: '已提交申请', icon: 'success', duration: 2000 });
      wx.showModal({ title: '提交成功', content: '孩子信息已提交，等待管理员审核通过后即可上传作品', showCancel: false });
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
      wx.showToast({ title: '请先选择已审核通过的孩子', icon: 'none' });
      return;
    }
    if (!this.data.imageUrl) {
      wx.showToast({ title: '请上传作品图片', icon: 'none' });
      return;
    }
    if (this.data.description && this.data.description.length > 100) {
      wx.showToast({ title: '描述不能超过100字', icon: 'none' });
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
    }).then(data => {
      if (data === null) return;
      wx.showModal({
        title: '提交成功',
        content: '作品已提交，等待管理员审核通过后展示在作品墙',
        showCancel: false,
        success: () => { wx.navigateBack(); }
      });
    });
  }
});
