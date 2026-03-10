const app = getApp();

Page({
  data: {
    activities: [],
    showModal: false,
    editing: {}
  },

  onLoad() {
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
      editing: { dailyShareLimit: 5, status: 1 }
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

  saveActivity() {
    const { editing } = this.data;
    if (!editing.title) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      startTime: editing.startDate ? editing.startDate + ' 00:00:00' : null,
      endTime: editing.endDate ? editing.endDate + ' 23:59:59' : null
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
