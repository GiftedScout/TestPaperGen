import React from 'react';
import { history } from 'umi';
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Descriptions,
  Empty,
  Input,
  Modal,
  PageHeader,
  Radio,
  Spin,
  Tag,
  message
} from 'antd';
import moment from 'moment';
import { getStudentPaper, submitStudentPaper } from '../../services/requestServices';

const CIRCLE_MARKS = ['①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨', '⑩'];

const QUESTION_TYPE_COLOR = {
  选择题: 'volcano',
  判断题: 'purple',
  填空题: 'blue',
  程序阅读题: 'cyan',
  程序设计题: 'green'
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

/**
 * 选择题题干中的选项以 ①-⑩ 内嵌，解析为独立的选项列表。
 * 解析失败（少于 2 个有效选项）时返回空列表，由调用方退化为文本输入。
 */
function parseChoiceTopic(topic) {
  const text = topic == null ? '' : String(topic);
  let firstIndex = -1;
  CIRCLE_MARKS.forEach(mark => {
    const index = text.indexOf(mark);
    if (index >= 0 && (firstIndex < 0 || index < firstIndex)) {
      firstIndex = index;
    }
  });
  if (firstIndex < 0) {
    return { stem: text, options: [] };
  }
  const stem = text.slice(0, firstIndex).trim();
  const options = [];
  let current = null;
  for (let i = 0; i < text.length; i++) {
    const markIndex = CIRCLE_MARKS.indexOf(text[i]);
    if (markIndex >= 0) {
      current = {
        letter: String.fromCharCode(65 + markIndex),
        content: ''
      };
      options.push(current);
    } else if (current) {
      current.content += text[i];
    }
  }
  const cleaned = options
    .map(option => ({
      letter: option.letter,
      content: option.content.trim().replace(/[；;]\s*$/, '')
    }))
    .filter(option => option.content);
  if (cleaned.length < 2) {
    return { stem: text, options: [] };
  }
  return { stem, options: cleaned };
}

export default class ExamAnswer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: true,
      submitting: false,
      paper: null,
      loadError: '',
      submittedTip: false,
      answers: {},
      now: Date.now()
    };
    this.tickTimer = null;
  }

  componentDidMount() {
    this.loadPaper();
    this.tickTimer = setInterval(() => {
      this.setState({ now: Date.now() });
    }, 30000);
  }

  componentWillUnmount() {
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
  }

  getPublishId = () => {
    const params = this.props.match && this.props.match.params;
    return params ? params.publishId : null;
  };

  loadPaper = async () => {
    const publishId = this.getPublishId();
    if (!publishId) {
      this.setState({ loading: false, loadError: '缺少试卷标识' });
      return;
    }
    this.setState({ loading: true, loadError: '', submittedTip: false });
    try {
      const res = await getStudentPaper({ publishId });
      if (res && res.code === 200) {
        this.setState({ paper: res.data || null, answers: {} });
      } else {
        const msg = (res && res.msg) || '获取试卷失败';
        this.setState({
          loadError: msg,
          submittedTip: msg.indexOf('已提交') >= 0
        });
      }
    } catch (e) {
      const msg = extractMessage(e, '获取试卷失败，请稍后重试');
      this.setState({
        loadError: msg,
        submittedTip: msg.indexOf('已提交') >= 0
      });
    } finally {
      this.setState({ loading: false });
    }
  };

  updateAnswer = (questionId, value) => {
    this.setState(prevState => {
      const answers = Object.assign({}, prevState.answers);
      answers[questionId] = value;
      return { answers };
    });
  };

  isExpired = () => {
    const paper = this.state.paper;
    if (!paper || !paper.deadline) {
      return false;
    }
    return moment(paper.deadline).isBefore(moment(this.state.now));
  };

  buildAnswer = question => {
    const value = this.state.answers[question.questionHistoryId];
    const topicType = question.topicType || '';
    if (topicType.indexOf('选择') >= 0) {
      const parsed = parseChoiceTopic(question.topic);
      if (parsed.options.length && Array.isArray(value)) {
        return value.slice().sort().join('');
      }
      return typeof value === 'string' ? value : '';
    }
    if (topicType.indexOf('判断') >= 0) {
      return typeof value === 'string' ? value : '';
    }
    if (topicType.indexOf('填空') >= 0) {
      const blankCount = question.blankCount || 1;
      const parts = [];
      const values = Array.isArray(value) ? value : [];
      for (let i = 0; i < blankCount; i++) {
        parts.push(values[i] == null ? '' : String(values[i]));
      }
      return parts.join('|');
    }
    return value == null ? '' : String(value);
  };

  handleSubmit = () => {
    const paper = this.state.paper;
    if (!paper) {
      return;
    }
    if (this.isExpired()) {
      message.warning('已过截止时间，无法提交');
      return;
    }
    Modal.confirm({
      title: '确认提交试卷？',
      content: '提交后不可修改，客观题将立即判分，主观题由教师评分。',
      okText: '确认提交',
      cancelText: '再检查一下',
      onOk: this.doSubmit
    });
  };

  doSubmit = async () => {
    const paper = this.state.paper;
    if (!paper) {
      return;
    }
    this.setState({ submitting: true });
    try {
      const questions = Array.isArray(paper.questions) ? paper.questions : [];
      const answers = questions.map(question => ({
        questionHistoryId: question.questionHistoryId,
        answer: this.buildAnswer(question)
      }));
      const res = await submitStudentPaper({ publishId: paper.publishId, answers });
      if (res && res.code === 200) {
        message.success('提交成功');
        history.push(`/exam/result/${paper.publishId}`);
      } else {
        message.error((res && res.msg) || '提交失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '提交失败，请稍后重试'));
    } finally {
      this.setState({ submitting: false });
    }
  };

  renderAnswerControl = question => {
    const topicType = question.topicType || '';
    const questionId = question.questionHistoryId;
    const value = this.state.answers[questionId];

    if (topicType.indexOf('选择') >= 0) {
      const parsed = parseChoiceTopic(question.topic);
      if (parsed.options.length) {
        return (
          <div>
            <div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>
              {parsed.stem || '请选择正确选项：'}
            </div>
            <Checkbox.Group
              value={Array.isArray(value) ? value : []}
              onChange={values => this.updateAnswer(questionId, values)}
              style={{ display: 'block' }}
            >
              {parsed.options.map(option => (
                <Checkbox
                  key={option.letter}
                  value={option.letter}
                  style={{ display: 'block', margin: '4px 0', whiteSpace: 'pre-wrap' }}
                >
                  {option.letter}. {option.content}
                </Checkbox>
              ))}
            </Checkbox.Group>
          </div>
        );
      }
      return (
        <div>
          <div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{question.topic}</div>
          <Input
            placeholder="请填写选项字母，如 A 或 AC"
            value={typeof value === 'string' ? value : ''}
            onChange={e => this.updateAnswer(questionId, e.target.value)}
            style={{ maxWidth: 320 }}
          />
        </div>
      );
    }

    if (topicType.indexOf('判断') >= 0) {
      return (
        <div>
          <div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{question.topic}</div>
          <Radio.Group
            value={typeof value === 'string' ? value : undefined}
            onChange={e => this.updateAnswer(questionId, e.target.value)}
          >
            <Radio value="对">对</Radio>
            <Radio value="错">错</Radio>
          </Radio.Group>
        </div>
      );
    }

    if (topicType.indexOf('填空') >= 0) {
      const blankCount = question.blankCount || 1;
      const values = Array.isArray(value) ? value : [];
      return (
        <div>
          <div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{question.topic}</div>
          {Array.from({ length: blankCount }).map((_, index) => (
            <Input
              key={index}
              placeholder={`第 ${index + 1} 空`}
              value={values[index] == null ? '' : values[index]}
              onChange={e => {
                const next = values.slice();
                next[index] = e.target.value;
                this.updateAnswer(questionId, next);
              }}
              style={{ width: 220, marginRight: 8, marginBottom: 8 }}
            />
          ))}
        </div>
      );
    }

    return (
      <div>
        <div style={{ whiteSpace: 'pre-wrap', marginBottom: 8 }}>{question.topic}</div>
        <Input.TextArea
          rows={4}
          placeholder="请在此作答"
          value={typeof value === 'string' ? value : ''}
          onChange={e => this.updateAnswer(questionId, e.target.value)}
        />
      </div>
    );
  };

  renderQuestion = (question, index) => {
    const topicType = question.topicType || '';
    return (
      <Card
        key={question.questionHistoryId}
        size="small"
        style={{ marginBottom: 16 }}
        title={
          <span>
            第 {index + 1} 题
            <Tag color={QUESTION_TYPE_COLOR[topicType] || 'default'} style={{ marginLeft: 8 }}>
              {topicType || '其他'}
            </Tag>
            <Tag>{question.score == null ? 0 : question.score} 分</Tag>
          </span>
        }
      >
        <div style={{ color: '#888', marginBottom: 8 }}>
          章节：{question.chapter1 || '未标注'}
          {question.chapter2 ? ` / ${question.chapter2}` : ''}
          {'　'}知识点：{question.label1 || '未标注'}
          {question.label2 ? ` / ${question.label2}` : ''}
        </div>
        {this.renderAnswerControl(question)}
      </Card>
    );
  };

  renderBody = () => {
    const paper = this.state.paper;
    const questions = paper && Array.isArray(paper.questions) ? paper.questions : [];
    const expired = this.isExpired();
    return (
      <div>
        <PageHeader
          onBack={() => history.push('/exam')}
          title={paper.paperName || '在线作答'}
          subTitle="请在截止时间前完成并提交"
          extra={[
            <Button key="list" onClick={() => history.push('/exam')}>
              返回列表
            </Button>
          ]}
        />
        <Descriptions size="small" column={4} style={{ marginBottom: 16 }} bordered>
          <Descriptions.Item label="发布人">{paper.publisher || '—'}</Descriptions.Item>
          <Descriptions.Item label="截止时间">{paper.deadline || '未设置'}</Descriptions.Item>
          <Descriptions.Item label="总分">{paper.totalScore == null ? '—' : paper.totalScore}</Descriptions.Item>
          <Descriptions.Item label="题量">{paper.questionCount == null ? questions.length : paper.questionCount}</Descriptions.Item>
        </Descriptions>
        {expired ? (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="该试卷已截止，无法提交"
            description="截止时间已过，作答内容不会被保存。"
          />
        ) : null}
        {questions.length === 0 ? (
          <Empty description="该试卷暂无题目" />
        ) : (
          questions.map((question, index) => this.renderQuestion(question, index))
        )}
        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Button
            type="primary"
            size="large"
            loading={this.state.submitting}
            disabled={expired || questions.length === 0}
            onClick={this.handleSubmit}
          >
            提交试卷
          </Button>
        </div>
      </div>
    );
  };

  render() {
    const { loading, loadError, submittedTip, paper } = this.state;
    const publishId = this.getPublishId();
    if (loading) {
      return (
        <div style={{ padding: 60, textAlign: 'center' }}>
          <Spin size="large" tip="加载中" />
        </div>
      );
    }
    if (loadError) {
      return (
        <div style={{ padding: '0 30px 30px' }}>
          <PageHeader onBack={() => history.push('/exam')} title="在线作答" />
          <Alert
            type={submittedTip ? 'info' : 'error'}
            showIcon
            message={loadError}
            description={submittedTip ? '该试卷已提交，可查看判分结果。' : '请返回列表后重试。'}
            action={
              <div>
                <Button onClick={() => history.push('/exam')} style={{ marginRight: 8 }}>
                  返回列表
                </Button>
                {submittedTip ? (
                  <Button type="primary" onClick={() => history.push(`/exam/result/${publishId}`)}>
                    查看结果
                  </Button>
                ) : null}
              </div>
            }
          />
        </div>
      );
    }
    if (!paper) {
      return (
        <div style={{ padding: '0 30px 30px' }}>
          <Empty description="未找到试卷" />
        </div>
      );
    }
    return <div style={{ padding: '0 30px 40px' }}>{this.renderBody()}</div>;
  }
}
