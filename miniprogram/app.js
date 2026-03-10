App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'https://tianma.chat/api'
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = wx.getStorageSync('userInfo');
    }
  },

  // 登录方法
  login() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: res => {
          if (res.code) {
            this.request({
              url: '/user/login',
              method: 'POST',
              data: { code: res.code },
              noAuth: true
            }).then(data => {
              this.globalData.token = data.token;
              this.globalData.userInfo = data;
              wx.setStorageSync('token', data.token);
              wx.setStorageSync('userInfo', data);
              resolve(data);
            }).catch(reject);
          } else {
            reject(new Error('登录失败'));
          }
        },
        fail: reject
      });
    });
  },

  // 统一请求方法
  request(options) {
    return new Promise((resolve, reject) => {
      const header = {
        'Content-Type': 'application/json'
      };

      if (!options.noAuth && this.globalData.token) {
        header['Authorization'] = 'Bearer ' + this.globalData.token;
      }

      wx.request({
        url: this.globalData.baseUrl + options.url,
        method: options.method || 'GET',
        data: options.data,
        header: header,
        success: res => {
          if (res.statusCode === 200 && res.data.code === 0) {
            resolve(res.data.data);
          } else if (res.statusCode === 401) {
            // Token过期，重新登录
            this.login().then(() => {
              this.request(options).then(resolve).catch(reject);
            }).catch(reject);
          } else {
            wx.showToast({
              title: res.data.msg || '请求失败',
              icon: 'none'
            });
            reject(new Error(res.data.msg));
          }
        },
        fail: err => {
          wx.showToast({
            title: '网络错误',
            icon: 'none'
          });
          reject(err);
        }
      });
    });
  }
});
