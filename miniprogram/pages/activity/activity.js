const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    activity: {},
    shareCode: null,
    shareStatus: null,
    startDate: '',
    endDate: '',
    studioWechatId: '',
    studioQrcode: '',
    shareFrom: null
  },

  goBack() {
    wx.navigateBack();
  },

  onLoad(options) {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    this.loadStudioContact();
    if (options.id) {
      this.loadActivity(options.id);
    } else {
      // L2 设计 3.4: scene 参数无效，降级跳转首页（BS-017）
      wx.switchTab({ url: '/pages/index/index' });
      return;
    }

    // 处理来自分享链接的访问
    if (options.shareCode) {
      this.confirmShare(options.shareCode);
    }

    // L2 设计 3.4: 处理扫码着陆的 shareFrom 参数（REQ-PST-013）
    if (options.shareFrom) {
      this.setData({ shareFrom: options.shareFrom });
    }
  },

  // 确认分享（被分享者点击链接时调用）
  confirmShare(shareCode) {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.doConfirmShare(shareCode);
      });
    } else {
      this.doConfirmShare(shareCode);
    }
  },

  doConfirmShare(shareCode) {
    app.request({
      url: '/share/confirm',
      method: 'POST',
      data: { shareCode: shareCode }
    }).then(result => {
      if (result.success && result.lotteryAdded) {
        wx.showToast({
          title: '助力成功！',
          icon: 'success'
        });
      }
    });
  },

  onShow() {
    // 刷新分享状态
    if (this.data.activity.id) {
      this.loadShareStatus(this.data.activity.id);
    }
  },

  loadActivity(id) {
    app.request({
      url: '/activity/detail?id=' + id,
      noAuth: true
    }).then(data => {
      this.setData({
        activity: data || {},
        startDate: this.formatDate(data?.startTime),
        endDate: this.formatDate(data?.endTime)
      });
      // 加载分享状态
      this.loadShareStatus(id);
      // 首次进入活动自动增加抽奖机会
      this.checkFirstVisit(id);
    });
  },

  // 格式化日期（只显示年月日）
  formatDate(dateTimeStr) {
    if (!dateTimeStr) return '';
    const date = dateTimeStr.split('T')[0].split(' ')[0];
    return date;
  },

  // 检查是否首次进入该活动，自动增加抽奖机会
  checkFirstVisit(activityId) {
    if (!app.globalData.token) return;
    app.request({
      url: '/activity/visit',
      method: 'POST',
      data: { activityId: activityId }
    }).then(result => {
      if (result && result.lotteryAdded) {
        wx.showToast({
          title: '获得1次抽奖机会',
          icon: 'success'
        });
      }
    }).catch(() => {
      // 忽略错误
    });
  },

  loadShareStatus(activityId) {
    if (!app.globalData.token) return;
    app.request({
      url: '/share/status',
      data: { activityId: activityId }
    }).then(data => {
      this.setData({ shareStatus: data });
      // 预先创建分享码，确保 onShareAppMessage 时已就绪
      if (data && data.canShare && !this.data.shareCode) {
        this.createShareCode();
      }
    });
  },

  loadStudioContact() {
    app.request({
      url: '/studio/config',
      noAuth: true
    }).then(data => {
      this.setData({
        studioWechatId: data.studio_wechat_id || '',
        studioQrcode: data.studio_qrcode || ''
      });
    });
  },

  copyWechatId() {
    const wechatId = this.data.studioWechatId;
    if (!wechatId) return;
    wx.setClipboardData({
      data: wechatId,
      success: () => {
        wx.showToast({
          title: '微信号已复制，去微信搜索添加好友吧',
          icon: 'none',
          duration: 2500
        });
      }
    });
  },

  previewQrcode() {
    const qrcode = this.data.studioQrcode;
    if (!qrcode) return;
    wx.previewImage({
      urls: [qrcode],
      current: qrcode
    });
  },

  goToLottery() {
    wx.switchTab({
      url: '/pages/lottery/lottery'
    });
  },

  showNotStartedTip() {
    wx.showToast({
      title: '活动即将开始，敬请期待',
      icon: 'none'
    });
  },

  // 创建分享并获取分享码
  createShareCode() {
    return new Promise((resolve, reject) => {
      if (!app.globalData.token) {
        app.login().then(() => {
          this.doCreateShare(resolve, reject);
        }).catch(reject);
      } else {
        this.doCreateShare(resolve, reject);
      }
    });
  },

  doCreateShare(resolve, reject) {
    app.request({
      url: '/share/create',
      method: 'POST',
      data: { activityId: this.data.activity.id }
    }).then(data => {
      if (data.success) {
        this.setData({ shareCode: data.shareCode });
        resolve(data);
      } else {
        wx.showToast({ title: data.msg, icon: 'none' });
        reject(new Error(data.msg));
      }
    }).catch(reject);
  },

  onShareAppMessage() {
    const activity = this.data.activity;
    const userId = app.globalData.userInfo?.id;

    // 使用已缓存的 shareCode，若无则提前创建
    // 注意: onShareAppMessage 必须同步返回，所以无法等待异步结果
    // 通过在 onShow 和 loadShareStatus 时预先创建 shareCode 来解决
    if (!this.data.shareCode) {
      this.createShareCode();
    }

    // 分享活动详情页，其他用户点击后直接进入该活动的详情页
    // 使用 sharerId 作为兜底方案，即使 shareCode 未就绪也能追踪分享者
    return {
      title: activity.shareTitle || activity.title || '快来参加活动吧！',
      path: `/pages/activity/activity?id=${activity.id}&sharerId=${userId || ''}&shareCode=${this.data.shareCode || ''}`,
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  },

  onShareTimeline() {
    const activity = this.data.activity;
    return {
      title: activity.shareTitle || activity.title || '快来参加活动吧！',
      imageUrl: activity.shareImage || activity.coverImg || '/images/share-default.png'
    };
  }
});
