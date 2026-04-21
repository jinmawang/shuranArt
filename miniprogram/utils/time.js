/**
 * 格式化日期时间为 "YYYY年MM月DD日 HH:mm"
 * 支持 ISO-8601 (2024-12-23T14:30:00) 和空格分隔 (2024-12-23 14:30:00) 两种格式
 */
function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '';
  var str = dateTimeStr.replace('T', ' ');
  var parts = str.split(' ');
  var datePart = parts[0] || '';
  var timePart = parts[1] || '00:00:00';
  var d = datePart.split('-');
  var t = timePart.split(':');
  if (d.length < 3) return datePart;
  var year = d[0];
  var month = parseInt(d[1]);
  var day = parseInt(d[2]);
  var hour = t[0] || '00';
  var minute = t[1] || '00';
  return year + '年' + month + '月' + day + '日 ' + hour + ':' + minute;
}

/**
 * 格式化日期为 "YYYY年MM月DD日"
 */
function formatDate(dateTimeStr) {
  if (!dateTimeStr) return '';
  var str = dateTimeStr.replace('T', ' ');
  var datePart = str.split(' ')[0] || '';
  var d = datePart.split('-');
  if (d.length < 3) return datePart;
  return d[0] + '年' + parseInt(d[1]) + '月' + parseInt(d[2]) + '日';
}

module.exports = {
  formatDateTime: formatDateTime,
  formatDate: formatDate
};
