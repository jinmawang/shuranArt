const app = getApp();

Page({
  data: {
    activity: {},
    shareCode: null,
    shareStatus: null,
    startDate: '',
    endDate: '',
    studioWechatId: '',
    studioQrcode: '',
    // PST 海报相关状态（L2: PST-api-detail.md 3.1 STEP-01/STEP-09）
    isPosterGenerating: false,  // REQ-PST-014: 防重复点击
    showPosterModal: false,     // REQ-PST-004: 海报预览弹窗
    posterImagePath: '',        // 海报临时图片路径
    shareFrom: null             // REQ-PST-013: 扫码分享追踪参数
  },

  onLoad(options) {
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
      url: '/activity/' + id,
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

  // ============================================
  // PST 海报功能（L2: PST-api-detail.md 3.1 ~ 3.3）
  // ============================================

  /**
   * 生成海报入口
   * L2 设计: PST-api-detail.md 3.1 generatePoster
   * 需求来源: REQ-PST-001 ~ REQ-PST-004, REQ-PST-014
   */
  async generatePoster() {
    // STEP-01: 防重复点击（REQ-PST-014, BS-012）
    if (this.data.isPosterGenerating) return;
    this.setData({ isPosterGenerating: true });
    wx.showLoading({ title: '海报生成中...' });

    try {
      // STEP-02: 确保已登录（BS-011）
      if (!app.globalData.token) {
        try {
          await app.login();
        } catch (e) {
          wx.hideLoading();
          wx.showToast({ title: '登录失败，请重试', icon: 'none' });
          this.setData({ isPosterGenerating: false });
          return;
        }
      }

      // STEP-03: 获取小程序码（REQ-PST-003）
      let wxacodeBase64;
      try {
        const wxacodeResult = await this.requestPromise({
          url: '/share/wxacode',
          data: { activityId: this.data.activity.id }
        });
        wxacodeBase64 = wxacodeResult.wxacodeBase64;
      } catch (error) {
        // BS-007: 小程序码获取失败
        wx.hideLoading();
        wx.showToast({ title: '小程序码获取失败，请检查网络后重试', icon: 'none', duration: 3000 });
        this.setData({ isPosterGenerating: false });
        return;
      }

      // STEP-04: 下载背景图（REQ-PST-002, BS-001, BS-008）
      const activity = this.data.activity;
      const bgImageUrl = activity.shareImage || activity.coverImg || '/images/default-activity.png';
      let bgImagePath;
      try {
        bgImagePath = await this.downloadImage(bgImageUrl);
      } catch (e) {
        // BS-008: 超时使用默认图
        bgImagePath = '/images/default-activity.png';
      }

      // STEP-05: 获取画室名称（BS-002）
      const studioName = app.globalData.studioConfig?.studio_name || '舒然画室';

      // STEP-06: 准备小程序码临时图片
      const wxacodeArrayBuffer = wx.base64ToArrayBuffer(wxacodeBase64);
      const wxacodeTempPath = `${wx.env.USER_DATA_PATH}/wxacode_temp.png`;
      const fs = wx.getFileSystemManager();
      fs.writeFileSync(wxacodeTempPath, wxacodeArrayBuffer, 'binary');

      // STEP-07: Canvas 2D 绘制海报（750x1334px, 决策 2 + 决策 7）
      try {
        const posterPath = await this.drawPoster({
          bgImagePath,
          activity,
          studioName,
          wxacodeTempPath
        });

        // STEP-09: 显示海报预览弹窗（REQ-PST-004）
        wx.hideLoading();
        this.setData({
          isPosterGenerating: false,
          showPosterModal: true,
          posterImagePath: posterPath
        });
      } catch (canvasError) {
        // Canvas 绘制异常
        wx.hideLoading();
        wx.showToast({ title: '海报生成失败，请重试', icon: 'none', duration: 3000 });
        this.setData({ isPosterGenerating: false });
      }
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: '海报生成失败，请重试', icon: 'none', duration: 3000 });
      this.setData({ isPosterGenerating: false });
    }
  },

  /**
   * Canvas 2D 绘制海报
   * L2 设计: PST-api-detail.md 3.1 STEP-07（7a ~ 7g）
   * 需求来源: REQ-PST-001, REQ-PST-020（750x1334px）
   */
  drawPoster({ bgImagePath, activity, studioName, wxacodeTempPath }) {
    return new Promise((resolve, reject) => {
      const query = wx.createSelectorQuery();
      query.select('#posterCanvas')
        .fields({ node: true, size: true })
        .exec((res) => {
          if (!res[0] || !res[0].node) {
            reject(new Error('Canvas node not found'));
            return;
          }

          const canvas = res[0].node;
          const ctx = canvas.getContext('2d');

          // 设置画布大小为 750x1334（REQ-PST-020, 决策 7）
          canvas.width = 750;
          canvas.height = 1334;

          // 加载所有图片后绘制
          const bgImg = canvas.createImage();
          const wxacodeImg = canvas.createImage();

          let loadedCount = 0;
          const totalImages = 2;

          const onAllLoaded = () => {
            try {
              // 7a: 绘制背景图（上方 60%，即 0~800px）
              const bgRatio = bgImg.width / bgImg.height;
              const targetRatio = 750 / 800;
              let sx = 0, sy = 0, sw = bgImg.width, sh = bgImg.height;
              // aspectFill 裁剪
              if (bgRatio > targetRatio) {
                sw = bgImg.height * targetRatio;
                sx = (bgImg.width - sw) / 2;
              } else {
                sh = bgImg.width / targetRatio;
                sy = (bgImg.height - sh) / 2;
              }
              ctx.drawImage(bgImg, sx, sy, sw, sh, 0, 0, 750, 800);

              // 7b: 绘制白色信息区（下方 40%，即 800~1334px）
              ctx.fillStyle = '#FFFFFF';
              ctx.fillRect(0, 800, 750, 534);

              // 7c: 绘制活动标题（BS-004: 超过 40 字截断）
              let title = activity.title || '';
              if (title.length > 40) {
                title = title.substring(0, 40) + '...';
              }
              ctx.font = 'bold 36px sans-serif';
              ctx.fillStyle = '#333333';
              ctx.textAlign = 'center';
              ctx.fillText(title, 375, 870);

              // 7d: 绘制活动时间
              const timeText = this.formatDate(activity.startTime) + ' - ' + this.formatDate(activity.endTime);
              ctx.font = '24px sans-serif';
              ctx.fillStyle = '#666666';
              ctx.textAlign = 'center';
              ctx.fillText(timeText, 375, 920);

              // 7e: 绘制画室名称
              ctx.font = '28px sans-serif';
              ctx.fillStyle = '#6366F1';
              ctx.textAlign = 'center';
              ctx.fillText(studioName, 375, 970);

              // 7f: 绘制小程序码（右下角，150x150px）
              // 白色圆角背景底
              ctx.fillStyle = '#FFFFFF';
              this.roundRect(ctx, 550, 1050, 170, 170, 10);
              ctx.fill();
              ctx.drawImage(wxacodeImg, 560, 1060, 150, 150);

              // 7g: 绘制"长按识别小程序码"提示文字
              ctx.font = '20px sans-serif';
              ctx.fillStyle = '#999999';
              ctx.textAlign = 'center';
              ctx.fillText('长按识别小程序码', 635, 1240);

              // STEP-08: 导出临时图片
              wx.canvasToTempFilePath({
                canvas: canvas,
                fileType: 'jpg',
                quality: 0.9,
                success: (res) => {
                  resolve(res.tempFilePath);
                },
                fail: (err) => {
                  reject(err);
                }
              });
            } catch (drawErr) {
              reject(drawErr);
            }
          };

          const onImageLoad = () => {
            loadedCount++;
            if (loadedCount === totalImages) {
              onAllLoaded();
            }
          };

          bgImg.onload = onImageLoad;
          bgImg.onerror = () => {
            // BS-008: 背景图加载失败，使用白色填充继续
            bgImg._failed = true;
            onImageLoad();
          };
          wxacodeImg.onload = onImageLoad;
          wxacodeImg.onerror = () => reject(new Error('wxacode image load failed'));

          bgImg.src = bgImagePath;
          wxacodeImg.src = wxacodeTempPath;
        });
    });
  },

  /**
   * 绘制圆角矩形辅助方法
   * L2 设计: PST-api-detail.md 3.1 STEP-07f roundRect
   */
  roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.arcTo(x + w, y, x + w, y + r, r);
    ctx.lineTo(x + w, y + h - r);
    ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
    ctx.lineTo(x + r, y + h);
    ctx.arcTo(x, y + h, x, y + h - r, r);
    ctx.lineTo(x, y + r);
    ctx.arcTo(x, y, x + r, y, r);
    ctx.closePath();
  },

  /**
   * 保存海报到相册
   * L2 设计: PST-api-detail.md 3.2 savePosterToAlbum
   * 需求来源: REQ-PST-005 ~ REQ-PST-007
   */
  savePosterToAlbum() {
    wx.saveImageToPhotosAlbum({
      filePath: this.data.posterImagePath,
      success: () => {
        // REQ-PST-005: 保存成功提示
        wx.showToast({ title: '保存成功', icon: 'success' });
      },
      fail: (error) => {
        if (error.errMsg && error.errMsg.indexOf('auth deny') !== -1) {
          // BS-010, REQ-PST-007: 权限被拒绝，引导设置
          wx.showModal({
            title: '提示',
            content: '请在设置中开启相册权限',
            confirmText: '去设置',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting();
              }
            }
          });
        } else {
          // 其他保存失败原因
          wx.showToast({ title: '保存失败，请检查手机存储空间', icon: 'none', duration: 3000 });
        }
      }
    });
  },

  /**
   * 分享海报给好友
   * L2 设计: PST-api-detail.md 3.3 sharePosterToFriend
   * 需求来源: REQ-PST-008
   */
  sharePosterToFriend() {
    wx.shareFileMessage({
      filePath: this.data.posterImagePath,
      fileName: '舒然画室活动海报.jpg',
      success: () => {},
      fail: (error) => {
        if (error.errMsg && error.errMsg.indexOf('cancel') !== -1) {
          // 用户取消，不提示
        } else {
          wx.showToast({ title: '分享失败，请重试', icon: 'none', duration: 3000 });
        }
      }
    });
  },

  /**
   * 关闭海报预览弹窗
   * L2 设计: BS-014（点击外部区域关闭）
   */
  closePosterModal() {
    this.setData({ showPosterModal: false });
  },

  /**
   * 下载网络图片到临时文件
   * L2 设计: PST-api-detail.md 3.1 STEP-04
   * BS-008: 5 秒超时
   */
  downloadImage(url) {
    return new Promise((resolve, reject) => {
      // 本地路径直接返回
      if (url.startsWith('/')) {
        resolve(url);
        return;
      }
      wx.downloadFile({
        url: url,
        timeout: 5000,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.tempFilePath);
          } else {
            reject(new Error('download failed'));
          }
        },
        fail: reject
      });
    });
  },

  /**
   * 封装 app.request 为 Promise
   * 用于 async/await 调用
   */
  requestPromise(options) {
    return app.request(options);
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
  }
});
