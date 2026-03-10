const app = getApp();

Page({
  data: {
    teachers: [],
    showModal: false,
    editing: {}
  },

  onLoad() {
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
      editing: { sortOrder: 0, status: 1 }
    });
  },

  editTeacher(e) {
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

  saveTeacher() {
    const { editing } = this.data;
    if (!editing.name) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });

    app.request({
      url: '/admin/teacher',
      method: 'POST',
      data: editing
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
