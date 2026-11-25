import React from 'react';
import { history } from 'umi';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  PageHeader,
  Row,
  Col,
  Spin,
  Statistic,
  Tag,
  message
} from 'antd';
import { getStudentResult } from '../../services/requestServices';

const JUDGE_MAP = {
  correct: { color: 'green', text: '正确' },
  wrong: { color: 'red', text: '错误' },
  pending: { color: 'orange', text: '待评' },
  ungraded: { color: 'default', text: '未判分' }
};

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

export default class ExamResult extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      result: null,
      loadError: ''
    };
  }

  componentDidMount() {
    this.loadResult();
  }

  getPublishId = () => {
    const params = this.props.match && this.props.match.params;
    return params ? params.publishId : null;
  };

  loadResult = async () => {
    const publishId = this.getPublishId();
    if (!publishId) {
      this.setState({ loading: false, loadError: '缺少试卷标识' });
      return;
    }
    this.setState({ loading: true, loadError: '' });
    try {
      const res = await getStudentResult({ publishId });
      if (res && res.code === 200) {
        this.setState({ result: res.data || null });
      } else {
        this.setState({ loadError: (res && res.msg) || '获取作答结果失败' });
      }
    } catch (e) {
      this.setState({ loadError: extractMessage(e, '获取作答结果失败，请稍后重试') });
    } finally {
      this.setState({ loading: false });
    }
  };

  renderQuestion = (question, index) => {
    const judge = JUDGE_MAP[question.judgeResult] || { color: 'default', text: question.judgeResult || '—' };
    const correctAnswer = question.correctAnswer == null || question.correctAnswer === ''
      ? '尚未经教师确认'
      : question.correctAnswer;
    return (
      <Card
        key={question.questionHistoryId}
        size="small"
        style={{ marginBottom: 16 }}
        title={
          <span>
            第 {index + 1} 题
            <Tag style={{ marginLeft: 8 }}>{question.topicType || '其他'}</Tag>
            <Tag color={judge.color}>{judge.text}</Tag>
          </span>
        }
        extra={
          <span>
            得分 {question.scoreAwarded == null ? 0 : question.scoreAwarded} / {question.score == null ? 0 : question.score}
          </span>
        }
      >
        <div style={{ whiteSpace: 'pre-wrap', marginBottom: 12 }}>{question.topic}</div>
        <Descriptions size="small" column={1} bordered>
          <Descriptions.Item label="你的答案">
            {question.yourAnswer ? question.yourAnswer : '未作答'}
          </Descriptions.Item>
          <Descriptions.Item label="正确答案">{correctAnswer}</Descriptions.Item>
          <Descriptions.Item label="解析">{question.analysis || '暂无解析'}</Descriptions.Item>
          {question.reason ? (
            <Descriptions.Item label="判定说明">{question.reason}</Descriptions.Item>
          ) : null}
          <Descriptions.Item label="章节 / 知识点">
            {(question.chapter1 || '未标注') + (question.chapter2 ? ` / ${question.chapter2}` : '')}
            {'　'}
            {(question.label1 || '未标注') + (question.label2 ? ` / ${question.label2}` : '')}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    );
  };

  render() {
    const { loading, result, loadError } = this.state;
    const publishId = this.getPublishId();
    if (loading) {
      return (
        <div style={{ padding: 60, textAlign: 'center' }}>
          <Spin size="large" tip="加载中" />
        </div>
      );
    }
    if (loadError || !result) {
      return (
        <div style={{ padding: '0 30px 30px' }}>
          <PageHeader onBack={() => history.push('/exam')} title="作答结果" />
          <Alert
            type="error"
            showIcon
            message={loadError || '未找到作答结果'}
            description="请确认已提交该试卷后再查看结果。"
            action={
              <Button onClick={() => history.push('/exam')}>
                返回列表
              </Button>
            }
          />
        </div>
      );
    }
    const questions = Array.isArray(result.questions) ? result.questions : [];
    return (
      <div style={{ padding: '0 30px 40px' }}>
        <PageHeader
          onBack={() => history.push('/exam')}
          title={result.paperName || '作答结果'}
          subTitle="客观题已自动判分，主观题待教师评分"
          extra={[
            <Button key="list" onClick={() => history.push('/exam')}>
              返回列表
            </Button>,
            <Button key="report" type="primary" onClick={() => history.push('/examReport')}>
              学习报告
            </Button>
          ]}
        />
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card size="small">
              <Statistic title="得分" value={result.score == null ? 0 : result.score} suffix={`/ ${result.totalScore == null ? 0 : result.totalScore}`} />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="客观题满分" value={result.objectivePossible == null ? 0 : result.objectivePossible} />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="待评数" value={result.pendingCount == null ? 0 : result.pendingCount} />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic title="未判分题数" value={result.ungradedCount == null ? 0 : result.ungradedCount} />
            </Card>
          </Col>
        </Row>
        {questions.length === 0 ? (
          <Empty description="暂无题目结果" />
        ) : (
          questions.map((question, index) => this.renderQuestion(question, index))
        )}
        <div style={{ textAlign: 'center', marginTop: 8 }}>
          <Button onClick={() => history.push(`/examReport`)}>查看学习报告</Button>
        </div>
      </div>
    );
  }
}
