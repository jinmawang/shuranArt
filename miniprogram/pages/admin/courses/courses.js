const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    courses: [],
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
    this.loadCourses();
  },

  loadCourses() {
    app.request({
      url: '/admin/courses'
    }).then(data => {
      this.setData({ courses: data || [] });
    });
  },

  addCourse() {
    this.setData({
      showModal: true,
      editing: { sortOrder: 0, status: 1 }
    });
  },

  editCourse(e) {
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

  chooseCoverImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath;
        wx.showLoading({ title: '上传中...' });
        app.uploadImage(tempFilePath).then(url => {
          wx.hideLoading();
          this.setData({ 'editing.coverImg': url });
          wx.showToast({ title: '上传成功', icon: 'success' });
        }).catch(() => {
          wx.hideLoading();
        });
      }
    });
  },

  saveCourse() {
    const { editing } = this.data;

    if (!editing.name) {
      wx.showToast({ title: '请输入课程名称', icon: 'none' });
      return;
    }
    if (!editing.category) {
      wx.showToast({ title: '请输入课程分类', icon: 'none' });
      return;
    }
    if (!editing.suitableFor) {
      wx.showToast({ title: '请输入适合年龄', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      sortOrder: Number(editing.sortOrder) || 0
    };

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/course',
      method: 'POST',
      data: data
    }).then(() => {
      wx.hideLoading();
      wx.showToast({ title: '保存成功', icon: 'success' });
      this.closeModal();
      this.loadCourses();
    }).catch(() => {
      wx.hideLoading();
    });
  },

  deleteCourse(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个课程吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/course/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadCourses();
          });
        }
      }
    });
  },

  toggleStatus(e) {
    const item = e.currentTarget.dataset.item;
    const newStatus = item.status === 1 ? 0 : 1;
    const actionText = newStatus === 1 ? '上架' : '下架';

    wx.showModal({
      title: '确认' + actionText,
      content: '确定要' + actionText + '课程"' + item.name + '"吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/course/' + item.id + '/status',
            method: 'PUT',
            data: { status: newStatus }
          }).then(() => {
            wx.showToast({ title: actionText + '成功', icon: 'success' });
            this.loadCourses();
          });
        }
      }
    });
  }
});
