// CRS: 管理后台课程管理页
// 需求来源: EARS-CRS-005, AC-CRS-004
// 参照 admin/teachers/teachers.js 模式
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

  // EP-03: 加载全部课程（含已下架）(RM-003)
  loadCourses() {
    app.request({
      url: '/admin/courses'
    }).then(data => {
      this.setData({ courses: data || [] });
    });
  },

  // 添加课程 -- 打开空表单
  addCourse() {
    this.setData({
      showModal: true,
      editing: { sortOrder: 0, status: 1 }
    });
  },

  // 编辑课程 -- 打开预填表单
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

  // EP-04: 保存课程（新增/编辑）(RM-004, RM-005)
  // 前端校验: V-004 ~ V-008
  saveCourse() {
    const { editing } = this.data;

    // V-004: name 必填
    if (!editing.name) {
      wx.showToast({ title: '请输入课程名称', icon: 'none' });
      return;
    }
    // V-005: category 必填
    if (!editing.category) {
      wx.showToast({ title: '请输入课程类别', icon: 'none' });
      return;
    }
    // V-006: price 必填且 >= 0
    if (editing.price === undefined || editing.price === '' || editing.price === null) {
      wx.showToast({ title: '请输入课程价格', icon: 'none' });
      return;
    }
    if (Number(editing.price) < 0) {
      wx.showToast({ title: '课程价格不能为负数', icon: 'none' });
      return;
    }
    // V-007: duration 必填
    if (!editing.duration) {
      wx.showToast({ title: '请输入课程时长', icon: 'none' });
      return;
    }
    // V-008: suitableFor 必填
    if (!editing.suitableFor) {
      wx.showToast({ title: '请输入适合人群', icon: 'none' });
      return;
    }

    // 确保数值类型正确
    const data = {
      ...editing,
      price: Number(editing.price),
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

  // EP-05: 删除课程 (RM-006, B6: 确认弹窗后删除)
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

  // EP-06: 课程上下架切换 (AC-CRS-004)
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
