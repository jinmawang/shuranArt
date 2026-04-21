const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activities: [],
    showModal: false,
    editing: {},
    showTeams: false,
    currentActivityTitle: '',
    teams: []
  },

  goBack() {
    if (this.data.showTeams) {
      this.setData({ showTeams: false, teams: [] });
    } else {
      wx.navigateBack();
    }
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
      url: '/admin/groupbuy/activities'
    }).then(data => {
      this.setData({ activities: data || [] });
    });
  },

  addActivity() {
    this.setData({
      showModal: true,
      editing: { groupSize: 3, status: 1 }
    });
  },

  editActivity(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editing: {
        ...item,
        startDate: item.startTime ? item.startTime.split(' ')[0].split('T')[0] : '',
        endDate: item.endTime ? item.endTime.split(' ')[0].split('T')[0] : ''
      }
    });
  },

  closeModal() {
    this.setData({ showModal: false, editing: {} });
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({ [`editing.${key}`]: e.detail.value });
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

  chooseCoverImage() {
    this.chooseAndUploadImage('coverImg');
  },

  chooseShareImage() {
    this.chooseAndUploadImage('shareImage');
  },

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
          wx.showToast({ title: '上传失败', icon: 'none' });
        });
      }
    });
  },

  saveActivity() {
    const { editing } = this.data;
    if (!editing.title) {
      wx.showToast({ title: '请输入课程名称', icon: 'none' });
      return;
    }

    const data = {
      ...editing,
      startTime: editing.startDate ? editing.startDate + 'T00:00:00' : null,
      endTime: editing.endDate ? editing.endDate + 'T23:59:59' : null,
      groupSize: parseInt(editing.groupSize) || 3
    };
    delete data.startDate;
    delete data.endDate;
    delete data.started;
    delete data.ended;

    wx.showLoading({ title: '保存中...' });
    app.request({
      url: '/admin/groupbuy/activity',
      method: 'POST',
      data: data
    }).then(data => {
      wx.hideLoading();
      if (data !== null) {
        wx.showToast({ title: '保存成功', icon: 'success' });
        this.closeModal();
        this.loadActivities();
      }
    });
  },

  deleteActivity(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个拼团活动吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/admin/groupbuy/activity/' + id,
            method: 'DELETE'
          }).then(() => {
            wx.showToast({ title: '删除成功', icon: 'success' });
            this.loadActivities();
          });
        }
      }
    });
  },

  // 查看成团名单
  viewTeams(e) {
    const item = e.currentTarget.dataset.item;
    wx.showLoading({ title: '加载中...' });
    app.request({
      url: '/admin/groupbuy/teams?activityId=' + item.id
    }).then(data => {
      wx.hideLoading();
      this.setData({
        showTeams: true,
        currentActivityTitle: item.title,
        teams: data || []
      });
    }).catch(() => {
      wx.hideLoading();
    });
  }
});
