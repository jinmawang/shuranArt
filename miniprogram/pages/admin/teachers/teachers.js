const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    teachers: [],
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
    this.loadTeachers();
  },

  loadTeachers() {
    app.request({
      url: '/admin/teachers'
    }).then(data => {
      this.setData({ teachers: data || [] });
    });
  },

  addTeacher() {
    this.setData({
      showModal: true,
      editing: { sortOrder: 0, status: 1, works: [] }
    });
  },

  editTeacher(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editing: { ...item, works: item.works || [] }
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

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ 'editing.avatar': url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
  },

  chooseWork() {
    const works = this.data.editing.works || [];
    const remaining = 9 - works.length;
    if (remaining <= 0) return;

    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      success: (res) => {
        wx.showLoading({ title: '上传中...' });
        const uploads = res.tempFiles.map(f => app.uploadImage(f.tempFilePath));
        Promise.all(uploads).then(urls => {
          wx.hideLoading();
          const newWorks = works.concat(urls);
          this.setData({ 'editing.works': newWorks });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
          wx.showToast({ title: '上传失败', icon: 'none' });
        });
      }
    });
  },

  removeWork(e) {
    const index = e.currentTarget.dataset.index;
    const works = this.data.editing.works || [];
    works.splice(index, 1);
    this.setData({ 'editing.works': works });
  },

  saveTeacher() {
    const { editing } = this.data;
    if (!editing.name) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      sortOrder: Number(editing.sortOrder) || 0
    };

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/teacher',
      method: 'POST',
      data: data
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.closeModal();
      this.loadTeachers();
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deleteTeacher(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这位老师吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/teacher/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadTeachers();
          });
        }
      }
    });
  }
});
