App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'https://tianma.chat/api',
    studioConfig: null,
    statusBarHeight: 0,
    navBarHeight: 0,
    navBarTotalHeight: 0
  },

  onLaunch(options) {
    // 计算导航栏高度
    const sysInfo = wx.getSystemInfoSync();
    const menuBtn = wx.getMenuButtonBoundingClientRect();
    this.globalData.statusBarHeight = sysInfo.statusBarHeight;
    // 导航栏高度 = 胶囊按钮高度 + 上下间距
    this.globalData.navBarHeight = (menuBtn.top - sysInfo.statusBarHeight) * 2 + menuBtn.height;
    this.globalData.navBarTotalHeight = sysInfo.statusBarHeight + this.globalData.navBarHeight;

    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = wx.getStorageSync('userInfo');
    }

    // 加载画室配置（PST: 海报绘制时需要画室名称）
    this.loadStudioConfig();

    // PST: 处理扫码着陆 scene 参数（REQ-PST-012, REQ-PST-013）
    this.handleSceneLaunch(options);
  },

  onShow(options) {
    // PST: 处理扫码着陆 scene 参数（热启动场景）
    this.handleSceneLaunch(options);
  },

  /**
   * 处理小程序码扫码着陆
   * L2 设计: PST-api-detail.md 3.4 handleSceneLaunch
   * 需求来源: REQ-PST-012（跳转活动页）, REQ-PST-013（shareFrom 传递）
   *
   * scene 参数格式: "s={userId}&a={actId}"（短格式，BS-013）
   */
  handleSceneLaunch(options) {
    if (options && options.query && options.query.scene) {
      const scene = decodeURIComponent(options.query.scene);
      const params = this.parseQueryString(scene);
      // 解析 "s=5&a=1" 格式
      const shareFrom = params['s'];  // userId of sharer
      const actId = params['a'];      // activityId

      if (actId) {
        const url = '/pages/activity/activity?id=' + actId +
          (shareFrom ? '&shareFrom=' + shareFrom : '');
        wx.navigateTo({
          url: url,
          fail: () => {
            // 如果 navigateTo 失败（如已在该页面），尝试 redirectTo
            wx.redirectTo({ url: url });
          }
        });
      }
    }
  },

  /**
   * 解析查询字符串为键值对
   * L2 设计: PST-api-detail.md 3.4 parseQueryString
   */
  parseQueryString(str) {
    const params = {};
    if (!str) return params;
    const pairs = str.split('&');
    for (let i = 0; i < pairs.length; i++) {
      const pair = pairs[i].split('=');
      if (pair.length === 2) {
        params[pair[0]] = pair[1];
      }
    }
    return params;
  },

  /**
   * 加载画室配置
   * PST: 海报绘制需要画室名称（BS-002）
   */
  loadStudioConfig() {
    this.request({
      url: '/studio/config',
      noAuth: true
    }).then(data => {
      this.globalData.studioConfig = data;
    }).catch(() => {
      // 加载失败时使用空对象，海报绘制时会降级使用默认值
    });
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

  // 上传图片到服务器，返回永久URL
  uploadImage(tempFilePath) {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: this.globalData.baseUrl + '/upload',
        filePath: tempFilePath,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + this.globalData.token
        },
        success: res => {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 0) {
              resolve(data.data.url);
            } else {
              wx.showToast({ title: data.msg || '上传失败', icon: 'none' });
              reject(new Error(data.msg));
            }
          } catch (e) {
            reject(e);
          }
        },
        fail: err => {
          wx.showToast({ title: '上传失败，请检查网络', icon: 'none' });
          reject(err);
        }
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
