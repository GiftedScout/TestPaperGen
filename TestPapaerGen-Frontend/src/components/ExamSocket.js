import React from 'react';
import { notification } from 'antd';
import { API } from '../config/requestConfig';

const MAX_RETRY = 5;
const PING_INTERVAL = 25000;

const TYPE_TITLE = {
  'paper.published': '新试卷发布',
  'submission.received': '收到学生提交',
  'result.ready': '成绩已就绪'
};

function buildSocketUrl() {
  return `${API.replace(/^http/, 'ws')}/ws/exam`;
}

function describeMessage(type, data) {
  const payload = data || {};
  if (type === 'paper.published') {
    return `《${payload.paperName || '未命名试卷'}》由 ${payload.teacher || '教师'} 发布，截止时间：${payload.deadline || '未设置'}`;
  }
  if (type === 'submission.received') {
    return `${payload.student || '学生'} 提交了《${payload.paperName || '未命名试卷'}》，当前得分 ${payload.score == null ? '-' : payload.score}`;
  }
  if (type === 'result.ready') {
    return `《${payload.paperName || '未命名试卷'}》已出分，得分 ${payload.score == null ? '-' : payload.score}`;
  }
  return '';
}

/**
 * 局域网考试实时通知通道：登录后挂载在布局中。
 */
export default class ExamSocket extends React.Component {
  constructor(props) {
    super(props);
    this.socket = null;
    this.pingTimer = null;
    this.reconnectTimer = null;
    this.retryCount = 0;
    this.closedByUser = false;
  }

  componentDidMount() {
    if (typeof window === 'undefined' || typeof WebSocket === 'undefined') {
      return;
    }
    this.connect();
  }

  componentWillUnmount() {
    this.closedByUser = true;
    this.clearTimers();
    if (this.socket) {
      try {
        this.socket.close();
      } catch (e) {
        // 关闭失败无需提示
      }
      this.socket = null;
    }
  }

  clearTimers = () => {
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  };

  startPing = () => {
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
    }
    this.pingTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        try {
          this.socket.send('ping');
        } catch (e) {
          // 静默忽略
        }
      }
    }, PING_INTERVAL);
  };

  scheduleReconnect = () => {
    if (this.closedByUser || this.retryCount >= MAX_RETRY) {
      return;
    }
    const delay = Math.min(30000, 1000 * Math.pow(2, this.retryCount));
    this.retryCount += 1;
    this.reconnectTimer = setTimeout(() => {
      this.connect();
    }, delay);
  };

  connect = () => {
    if (this.closedByUser || typeof WebSocket === 'undefined') {
      return;
    }
    let socket = null;
    try {
      socket = new WebSocket(buildSocketUrl());
    } catch (e) {
      this.scheduleReconnect();
      return;
    }
    this.socket = socket;

    socket.onopen = () => {
      this.retryCount = 0;
      this.startPing();
    };

    socket.onmessage = event => {
      let message = null;
      try {
        message = JSON.parse(event.data);
      } catch (e) {
        return;
      }
      if (!message || !message.type) {
        return;
      }
      const title = TYPE_TITLE[message.type] || '考试通知';
      const description = describeMessage(message.type, message.data);
      if (description) {
        notification.info({
          message: title,
          description,
          key: `exam-ws-${message.type}`
        });
      }
      if (typeof window !== 'undefined' && window.dispatchEvent && typeof CustomEvent !== 'undefined') {
        window.dispatchEvent(new CustomEvent('exam:ws', { detail: message }));
      }
    };

    socket.onerror = () => {
      // 连接失败不弹窗，交由 onclose 处理重连
    };

    socket.onclose = () => {
      if (this.pingTimer) {
        clearInterval(this.pingTimer);
        this.pingTimer = null;
      }
      this.socket = null;
      if (!this.closedByUser) {
        this.scheduleReconnect();
      }
    };
  };

  render() {
    return null;
  }
}
