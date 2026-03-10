const app = getApp();

Page({
  data: {
    userInfo: {},
    prizes: [],
    rotateAngle: 0,
    isSpinning: false,
    showResult: false,
    resultPrize: null
  },

  onLoad() {
    this.loadData();
  },

  onShow() {
    // 刷新用户信息
    this.loadUserInfo();
  },

  loadData() {
    this.loadUserInfo();
    this.loadPrizes();
  },

  loadUserInfo() {
    if (!app.globalData.token) {
      app.login().then(() => {
        this.fetchUserInfo();
      });
    } else {
      this.fetchUserInfo();
    }
  },

  fetchUserInfo() {
    app.request({
      url: '/user/info'
    }).then(data => {
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

  startLottery() {
    if (this.data.isSpinning) return;

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
      method: 'POST'
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
        // 更新用户信息
        this.fetchUserInfo();
      }, 4000);
    }).catch(() => {
      this.setData({ isSpinning: false });
    });
  },

  closeResult() {
    this.setData({ showResult: false });
  },

  goToShare() {
    wx.navigateTo({
      url: '/pages/share/share'
    });
  }
});
