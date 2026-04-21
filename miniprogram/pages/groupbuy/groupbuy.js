const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activity: {},
    teams: [],
    startDate: '',
    endDate: '',
    myTeamId: null,
    fromTeamId: null,
    isFromShare: false
  },

  goBack() {
    wx.navigateBack();
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  onLoad(options) {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight,
      isFromShare: getCurrentPages().length === 1
    });
    if (options.id) {
      this.activityId = options.id;
      this.loadDetail(options.id);
    }
    if (options.teamId) {
      this.setData({ fromTeamId: options.teamId });
    }
  },

  onShow() {
    if (this.activityId) {
      this.loadDetail(this.activityId);
    }
  },

  loadDetail(id) {
    app.request({
      url: '/groupbuy/activity?id=' + id,
      noAuth: true
    }).then(data => {
      if (!data) return;
      const activity = data.activity || {};
      this.setData({
        activity: activity,
        teams: data.teams || [],
        startDate: this.formatDate(activity.startTime),
        endDate: this.formatDate(activity.endTime)
      });
      this.checkMyTeam();
    });
  },

  checkMyTeam() {
    if (!app.globalData.token) return;
    app.request({
      url: '/groupbuy/my-teams'
    }).then(data => {
      if (!data) return;
      const myTeam = data.find(t => t.activity && t.activity.id == this.activityId);
      if (myTeam) {
        this.setData({ myTeamId: myTeam.team.id });
      }
    }).catch(() => {});
  },

  formatDate(dateTimeStr) {
    if (!dateTimeStr) return '';
    var str = dateTimeStr.replace('T', ' ');
    var datePart = str.split(' ')[0] || '';
    var d = datePart.split('-');
    if (d.length < 3) return datePart;
    return d[0] + '年' + parseInt(d[1]) + '月' + parseInt(d[2]) + '日';
  },

  // 开团
  onCreateTeam() {
    if (!app.globalData.token) {
      app.login().then(() => this.doGetPhoneAndCreate());
      return;
    }
    this.doGetPhoneAndCreate();
  },

  doGetPhoneAndCreate() {
    // 手机号授权通过按钮的 open-type="getPhoneNumber" 触发
  },

  // 微信手机号授权回调（开团）
  onGetPhoneCreate(e) {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '需要授权手机号才能参与拼团', icon: 'none' });
      return;
    }
    // 请求订阅消息授权（用户同意后成团时可收到通知）
    this.requestSubscribe(() => {
      const code = e.detail.code;
      this.getPhoneNumber(code).then(phone => {
        this.doCreateTeam(phone);
      });
    });
  },

  // 微信手机号授权回调（加入团）
  onGetPhoneJoin(e) {
    const teamId = e.currentTarget.dataset.teamid;
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '需要授权手机号才能参与拼团', icon: 'none' });
      return;
    }
    this.requestSubscribe(() => {
      const code = e.detail.code;
      this.getPhoneNumber(code).then(phone => {
        this.doJoinTeam(teamId, phone);
      });
    });
  },

  // 请求订阅消息授权
  requestSubscribe(callback) {
    const tmplId = app.globalData.groupBuyTemplateId || '';
    if (!tmplId) {
      callback();
      return;
    }
    wx.requestSubscribeMessage({
      tmplIds: [tmplId],
      complete: () => {
        // 无论用户是否同意都继续流程
        callback();
      }
    });
  },

  getPhoneNumber(code) {
    return app.request({
      url: '/user/phone',
      method: 'POST',
      data: { code: code }
    }).then(data => {
      if (data && data.phone) return data.phone;
      wx.showToast({ title: '获取手机号失败，请重试', icon: 'none' });
      return '';
    });
  },

  doCreateTeam(phone) {
    if (!phone) return;
    wx.showLoading({ title: '开团中...' });
    app.request({
      url: '/groupbuy/create-team',
      method: 'POST',
      data: {
        activityId: this.activityId,
        phone: phone
      }
    }).then(data => {
      wx.hideLoading();
      if (data && data.success) {
        wx.showToast({ title: '开团成功！', icon: 'success' });
        this.setData({ myTeamId: data.teamId });
        this.loadDetail(this.activityId);
      }
    });
  },

  doJoinTeam(teamId, phone) {
    if (!phone) return;
    wx.showLoading({ title: '加入中...' });
    app.request({
      url: '/groupbuy/join-team',
      method: 'POST',
      data: {
        teamId: teamId,
        phone: phone
      }
    }).then(data => {
      wx.hideLoading();
      if (data && data.success) {
        const msg = data.completed ? '拼团成功！' : '加入成功！';
        wx.showToast({ title: msg, icon: 'success' });
        this.setData({ myTeamId: teamId });
        this.loadDetail(this.activityId);
      }
    });
  },

  onShareAppMessage() {
    const activity = this.data.activity;
    const teamId = this.data.myTeamId || '';
    return {
      title: activity.shareTitle || activity.title || '快来一起拼团吧！',
      path: `/pages/groupbuy/groupbuy?id=${activity.id}&teamId=${teamId}`,
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  },

  onShareTimeline() {
    const activity = this.data.activity;
    return {
      title: activity.shareTitle || activity.title || '快来一起拼团吧！',
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  }
});
