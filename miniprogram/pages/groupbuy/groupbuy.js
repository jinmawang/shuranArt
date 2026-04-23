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
    isFromShare: false,
    // 手机号输入弹窗
    showPhoneModal: false,
    phoneInput: '',
    pendingAction: '' // 'create' or 'join'
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

  // ─── 开团流程 ─────────────────────────────────

  onCreateTeam() {
    const doCreate = () => {
      this.pendingTeamId = null;
      this.setData({ pendingAction: 'create' });
      this.tryGetPhone();
    };
    if (!app.globalData.token) {
      app.login().then(doCreate).catch(() => {
        wx.showToast({ title: '登录失败，请重试', icon: 'none' });
      });
    } else {
      doCreate();
    }
  },

  // 加入团
  onJoinTeam(e) {
    const teamId = e.currentTarget.dataset.teamid;
    const doJoin = () => {
      this.pendingTeamId = teamId;
      this.setData({ pendingAction: 'join' });
      this.tryGetPhone();
    };
    if (!app.globalData.token) {
      app.login().then(doJoin).catch(() => {
        wx.showToast({ title: '登录失败，请重试', icon: 'none' });
      });
    } else {
      doJoin();
    }
  },

  // 尝试获取手机号：先看用户表是否已有，没有则弹手动输入
  tryGetPhone() {
    // 先请求用户信息，看是否已有手机号
    app.request({ url: '/user/info' }).then(user => {
      if (user && user.phone) {
        this.proceedWithPhone(user.phone);
      } else {
        // 没有手机号，弹出输入框
        this.setData({ showPhoneModal: true, phoneInput: '' });
      }
    }).catch(() => {
      this.setData({ showPhoneModal: true, phoneInput: '' });
    });
  },

  // 微信一键获取手机号回调（弹窗内按钮）
  onGetPhoneQuick(e) {
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      // 微信授权失败/拒绝，用户可手动输入
      return;
    }
    const code = e.detail.code;
    wx.showLoading({ title: '获取中...' });
    app.request({
      url: '/user/phone',
      method: 'POST',
      data: { code: code }
    }).then(data => {
      wx.hideLoading();
      if (data && data.phone) {
        this.setData({ showPhoneModal: false });
        this.proceedWithPhone(data.phone);
      } else {
        wx.showToast({ title: '获取失败，请手动输入', icon: 'none' });
      }
    }).catch(() => {
      wx.hideLoading();
      wx.showToast({ title: '获取失败，请手动输入', icon: 'none' });
    });
  },

  onPhoneInput(e) {
    this.setData({ phoneInput: e.detail.value });
  },

  // 手动输入手机号确认
  onPhoneConfirm() {
    const phone = (this.data.phoneInput || '').trim();
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    this.setData({ showPhoneModal: false });
    this.proceedWithPhone(phone);
  },

  onPhoneCancel() {
    this.setData({ showPhoneModal: false, pendingAction: '' });
  },

  noop() {},

  // 拿到手机号后执行开团/加入
  proceedWithPhone(phone) {
    this.requestSubscribe(() => {
      if (this.data.pendingAction === 'create') {
        this.doCreateTeam(phone);
      } else if (this.data.pendingAction === 'join') {
        this.doJoinTeam(this.pendingTeamId, phone);
      }
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
        callback();
      }
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
