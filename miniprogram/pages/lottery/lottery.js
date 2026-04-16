const app = getApp();

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0,
    userInfo: {},
    prizes: [],
    activityId: null,
    activity: null,
    activities: [],  // 所有进行中的活动
    lotteryStatus: null,  // 抽奖状态
    rotateAngle: 0,
    isSpinning: false,
    showResult: false,
    resultPrize: null,
    showRules: false,  // 显示规则弹窗
    canShare: false  // 是否可以分享
  },

  onLoad(options) {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight,
      navBarHeight: app.globalData.navBarHeight,
      navBarTotalHeight: app.globalData.navBarTotalHeight
    });
    if (options.activityId) {
      this.setData({ activityId: parseInt(options.activityId) });
    }
    this.loadData();
  },

  onShow() {
    // 刷新用户信息和抽奖状态
    this.loadUserInfo();
    if (this.data.activityId) {
      this.loadLotteryStatus();
    }
  },

  loadData() {
    this.loadUserInfo();
    this.loadPrizes();
    this.loadCurrentActivity();
  },

  loadUserInfo() {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.fetchUserInfo();
      }).catch(() => {});
    } else {
      this.fetchUserInfo();
    }
  },

  fetchUserInfo() {
    app.request({
      url: '/user/info'
    }).then(data => {
      if (!data) return;
      this.setData({ userInfo: data });
      app.globalData.userInfo = data;
    });
  },

  loadPrizes() {
    app.request({
      url: '/lottery/prizes',
      noAuth: true
    }).then(data => {
      this.setData({ prizes: data || [] });
    });
  },

  // 加载当前活动（如果没有指定activityId）
  loadCurrentActivity() {
    if (this.data.activityId) {
      // 已指定活动ID，加载活动详情
      this.loadActivityDetail(this.data.activityId);
      return;
    }

    // 获取活动列表
    app.request({
      url: '/activity/list',
      noAuth: true
    }).then(data => {
      const activities = data || [];
      // 找到所有进行中的活动
      const activeActivities = activities.filter(a => a.started && !a.ended);

      this.setData({
        activities: activeActivities,
        canShare: activeActivities.length > 0  // 只有有进行中的活动才能分享
      });

      if (activeActivities.length > 0) {
        // 默认选择第一个进行中的活动
        this.setData({
          activityId: activeActivities[0].id,
          activity: activeActivities[0]
        });
        this.loadLotteryStatus();
      } else if (activities.length > 0) {
        // 没有进行中的活动，取第一个活动（可能是即将开始的）
        this.setData({
          activityId: activities[0].id,
          activity: activities[0],
          canShare: false  // 无法分享
        });
        this.loadLotteryStatus();
      }
    });
  },

  loadActivityDetail(activityId) {
    app.request({
      url: '/activity/' + activityId,
      noAuth: true
    }).then(data => {
      this.setData({ activity: data });
      this.loadLotteryStatus();
    });
  },

  // 加载抽奖状态
  loadLotteryStatus() {
    if (!this.data.activityId) return;

    app.request({
      url: '/lottery/status',
      data: { activityId: this.data.activityId }
    }).then(data => {
      this.setData({ lotteryStatus: data });
    }).catch(() => {
      // 未登录时可能会失败，忽略
    });
  },

  startLottery() {
    if (this.data.isSpinning) return;

    if (!this.data.activityId) {
      wx.showToast({
        title: '请先选择活动',
        icon: 'none'
      });
      return;
    }

    if (!this.data.userInfo.lotteryChances || this.data.userInfo.lotteryChances <= 0) {
      wx.showToast({
        title: '没有抽奖机会',
        icon: 'none'
      });
      return;
    }

    this.setData({ isSpinning: true });

    // 调用抽奖接口
    app.request({
      url: '/lottery/draw',
      method: 'POST',
      data: { activityId: this.data.activityId }
    }).then(result => {
      // 计算旋转角度
      const prizeIndex = this.data.prizes.findIndex(p => p.id === result.prizeId);
      const singleAngle = 360 / this.data.prizes.length;
      const targetAngle = 360 - (prizeIndex * singleAngle + singleAngle / 2);
      const totalAngle = this.data.rotateAngle + 360 * 5 + targetAngle;

      this.setData({
        rotateAngle: totalAngle,
        resultPrize: result
      });

      // 旋转结束后显示结果
      setTimeout(() => {
        this.setData({
          isSpinning: false,
          showResult: true
        });
        // 更新用户信息和抽奖状态
        this.fetchUserInfo();
        this.loadLotteryStatus();
      }, 6200);
    }).catch(err => {
      this.setData({ isSpinning: false });
      wx.showToast({
        title: err.message || '抽奖失败',
        icon: 'none'
      });
    });
  },

  closeResult() {
    this.setData({ showResult: false });
  },

  goToShare() {
    // 检查是否有进行中的活动
    if (!this.data.canShare) {
      wx.showToast({
        title: '暂无进行中的活动',
        icon: 'none'
      });
      return;
    }

    const activityId = this.data.activityId;
    if (activityId) {
      wx.navigateTo({
        url: '/pages/share/share?activityId=' + activityId
      });
    } else {
      wx.showToast({
        title: '暂无活动',
        icon: 'none'
      });
    }
  },

  // 显示规则弹窗
  showRulesModal() {
    this.setData({ showRules: true });
  },

  // 关闭规则弹窗
  closeRulesModal() {
    this.setData({ showRules: false });
  }
});
