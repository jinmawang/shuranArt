const app = getApp();

Page({
  LOCAL_TEACHERS: [
    {
      id: 'local1',
      name: '主讲老师',
      title: '专业美术教师',
      intro: '多年美术教学经验，擅长儿童绘画启蒙和系统训练，善于发现每位学生的潜力。',
      avatar: 'https://tianma.chat/images/%E8%80%81%E5%B8%88%E4%BB%8B%E7%BB%8D/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241209102359.jpg',
      works: [
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193931.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193239.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193228.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223194551.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193219.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193209.jpg'
      ]
    },
    {
      id: 'local2',
      name: '助教老师',
      title: '美术辅导老师',
      intro: '专注于帮助学生打好绘画基础，耐心引导，因材施教，陪伴学生在艺术道路上成长。',
      avatar: 'https://tianma.chat/images/%E8%80%81%E5%B8%88%E4%BB%8B%E7%BB%8D/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241209102418.jpg',
      works: [
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193234.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193155.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193245.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193251.jpg',
        'https://tianma.chat/images/%E4%BD%9C%E5%93%81/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20241223193118.jpg'
      ]
    }
  ],

  data: {
    teachers: []
  },

  onLoad() {
    this.loadTeachers();
  },

  loadTeachers() {
    app.request({
      url: '/teacher/list',
      noAuth: true
    }).then(data => {
      this.setData({ teachers: (data && data.length) ? data : this.LOCAL_TEACHERS });
    }).catch(() => {
      this.setData({ teachers: this.LOCAL_TEACHERS });
    });
  },

  previewWork(e) {
    const { works, current } = e.currentTarget.dataset;
    wx.previewImage({
      urls: works,
      current: current
    });
  }
});

