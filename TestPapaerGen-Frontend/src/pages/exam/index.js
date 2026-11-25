import React from 'react';
import { history } from 'umi';
import { Button, Empty, PageHeader, Table, Tag, message } from 'antd';
import { getStudentPapers } from '../../services/requestServices';

const STATUS_MAP = {
  assigned: { color: 'blue', text: '待作答' },
  submitted: { color: 'green', text: '已提交' },
  expired: { color: 'red', text: '已截止' }
};

export default class ExamList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      list: []
    };
  }

  componentDidMount() {
    this.loadList();
    if (typeof window !== 'undefined') {
      window.addEventListener('exam:ws', this.onSocketEvent);
    }
  }

  componentWillUnmount() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('exam:ws', this.onSocketEvent);
    }
  }

  onSocketEvent = event => {
    const detail = event && event.detail;
    if (detail && detail.type === 'paper.published') {
      this.loadList();
    }
  };

  loadList = async () => {
    this.setState({ loading: true });
    try {
      const res = await getStudentPapers();
      if (res && res.code === 200) {
        this.setState({ list: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取考试列表失败');
      }
    } catch (e) {
      message.error('获取考试列表失败');
    } finally {
      this.setState({ loading: false });
    }
  };

  goAnswer = record => {
    history.push(`/exam/answer/${record.publishId}`);
  };

  goResult = record => {
    history.push(`/exam/result/${record.publishId}`);
  };

  render() {
    const columns = [
      {
        title: '试卷名称',
        dataIndex: 'paperName',
        key: 'paperName',
        ellipsis: true
      },
      {
        title: '发布人',
        dataIndex: 'publisher',
        key: 'publisher',
        width: 140
      },
      {
        title: '截止时间',
        dataIndex: 'deadline',
        key: 'deadline',
        width: 180,
        render: text => text || '未设置'
      },
      {
        title: '总分',
        dataIndex: 'totalScore',
        key: 'totalScore',
        width: 90
      },
      {
        title: '题量',
        dataIndex: 'questionCount',
        key: 'questionCount',
        width: 90
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        render: text => {
          const config = STATUS_MAP[text] || { color: 'default', text: text || '—' };
          return <Tag color={config.color}>{config.text}</Tag>;
        }
      },
      {
        title: '操作',
        key: 'action',
        width: 200,
        render: (_, record) => (
          <div>
            <Button
              type="link"
              disabled={record.submitted || record.expired}
              onClick={() => this.goAnswer(record)}
            >
              去作答
            </Button>
            <Button
              type="link"
              disabled={!record.submitted}
              onClick={() => this.goResult(record)}
            >
              查看结果
            </Button>
          </div>
        )
      }
    ];

    return (
      <div style={{ padding: '0 30px 30px' }}>
        <PageHeader
          title="我的考试"
          subTitle="查看已发布的试卷，在规定时间内在线作答"
          extra={[
            <Button key="refresh" onClick={this.loadList}>
              刷新
            </Button>
          ]}
        />
        <Table
          rowKey="publishId"
          columns={columns}
          dataSource={this.state.list}
          loading={this.state.loading}
          locale={{ emptyText: <Empty description="暂无发布的考试" /> }}
          pagination={{ pageSize: 10, showSizeChanger: false }}
        />
      </div>
    );
  }
}
