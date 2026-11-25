import React from 'react';
import { Button, Card, Col, Empty, PageHeader, Progress, Row, Spin, Statistic, Table, message } from 'antd';
import { getStudentReport } from '../../services/requestServices';

function extractMessage(error, fallback) {
  if (!error) {
    return fallback;
  }
  if (typeof error === 'string') {
    return error;
  }
  if (error.msg) {
    return error.msg;
  }
  if (error.data && error.data.msg) {
    return error.data.msg;
  }
  if (error.response && error.response.msg) {
    return error.response.msg;
  }
  return fallback;
}

export default class ExamReport extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      report: null
    };
  }

  componentDidMount() {
    this.loadReport();
  }

  loadReport = async () => {
    this.setState({ loading: true });
    try {
      const res = await getStudentReport({});
      if (res && res.code === 200) {
        this.setState({ report: res.data || null });
      } else {
        message.error((res && res.msg) || '获取学习报告失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取学习报告失败，请稍后重试'));
    } finally {
      this.setState({ loading: false });
    }
  };

  renderDimensionTable = (data, emptyText) => {
    const columns = [
      {
        title: '名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true
      },
      {
        title: '题量',
        dataIndex: 'questionCount',
        key: 'questionCount',
        width: 80
      },
      {
        title: '正确',
        dataIndex: 'correctCount',
        key: 'correctCount',
        width: 80
      },
      {
        title: '错误',
        key: 'wrongCount',
        width: 80,
        render: (_, record) => {
          const total = record.questionCount || 0;
          const correct = record.correctCount || 0;
          const pending = record.pendingCount || 0;
          return Math.max(0, total - correct - pending);
        }
      },
      {
        title: '待评',
        dataIndex: 'pendingCount',
        key: 'pendingCount',
        width: 80
      },
      {
        title: '得分率',
        dataIndex: 'rate',
        key: 'rate',
        width: 200,
        render: rate => {
          const percent = Number(rate) || 0;
          return <Progress percent={percent} size="small" />;
        }
      }
    ];
    return (
      <Table
        rowKey="name"
        size="small"
        columns={columns}
        dataSource={Array.isArray(data) ? data : []}
        pagination={false}
        locale={{ emptyText: <Empty description={emptyText} /> }}
      />
    );
  };

  render() {
    const { loading, report } = this.state;
    if (loading && !report) {
      return (
        <div style={{ padding: 60, textAlign: 'center' }}>
          <Spin size="large" tip="加载中" />
        </div>
      );
    }
    const data = report || {};
    const hasData = (data.totalPossible != null && Number(data.totalPossible) > 0)
      || (Array.isArray(data.chapters) && data.chapters.length > 0)
      || (Array.isArray(data.labels) && data.labels.length > 0);
    return (
      <div style={{ padding: '0 30px 40px' }}>
        <PageHeader
          title="学习报告"
          subTitle="按章节与知识点查看掌握情况"
          extra={[
            <Button key="refresh" onClick={this.loadReport} loading={loading}>
              刷新
            </Button>
          ]}
        />
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card size="small">
              <Statistic title="得分率" value={data.rate == null ? 0 : data.rate} suffix="%" />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="已得 / 可得分" value={data.totalEarned == null ? 0 : data.totalEarned} suffix={`/ ${data.totalPossible == null ? 0 : data.totalPossible}`} />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="待评数" value={data.pendingCount == null ? 0 : data.pendingCount} />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="未判分题数" value={data.ungradedCount == null ? 0 : data.ungradedCount} />
            </Card>
          </Col>
        </Row>
        {!hasData ? (
          <Card size="small">
            <Empty description="暂无已提交的考试，提交作答后即可查看学习报告" />
          </Card>
        ) : (
          <div>
            <Card size="small" title="章节维度" style={{ marginBottom: 16 }}>
              {this.renderDimensionTable(data.chapters, '暂无章节数据')}
            </Card>
            <Card size="small" title="知识点维度">
              {this.renderDimensionTable(data.labels, '暂无知识点数据')}
            </Card>
          </div>
        )}
      </div>
    );
  }
}
