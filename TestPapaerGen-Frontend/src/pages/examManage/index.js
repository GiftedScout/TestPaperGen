import React from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Modal,
  PageHeader,
  Progress,
  Radio,
  Row,
  Select,
  Table,
  Tag,
  message
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import moment from 'moment';
import {
  getAllTestPaperGenHistory,
  publishExam,
  getExamPublishList,
  getExamSubmissions,
  getClassReport,
  getExamStudents
} from '../../services/requestServices';

const SUBMISSION_STATUS = {
  assigned: { color: 'blue', text: '待作答' },
  submitted: { color: 'green', text: '已提交' }
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

export default class ExamManage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      papers: [],
      papersLoading: false,
      students: [],
      studentsLoading: false,
      publishes: [],
      publishesLoading: false,
      testPaperUid: undefined,
      scope: 'all',
      studentUsernames: [],
      deadline: null,
      submitting: false,
      submissionVisible: false,
      submissionLoading: false,
      submissionData: null,
      activePublish: null,
      reportVisible: false,
      reportLoading: false,
      reportData: null
    };
  }

  componentDidMount() {
    this.loadPapers();
    this.loadStudents();
    this.loadPublishes();
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
    if (detail && detail.type === 'submission.received') {
      this.loadPublishes();
    }
  };

  loadPapers = async () => {
    this.setState({ papersLoading: true });
    try {
      const res = await getAllTestPaperGenHistory();
      if (res && res.code === 200) {
        this.setState({ papers: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取试卷列表失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取试卷列表失败'));
    } finally {
      this.setState({ papersLoading: false });
    }
  };

  loadStudents = async () => {
    this.setState({ studentsLoading: true });
    try {
      const res = await getExamStudents();
      if (res && res.code === 200) {
        this.setState({ students: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取学生列表失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取学生列表失败'));
    } finally {
      this.setState({ studentsLoading: false });
    }
  };

  loadPublishes = async () => {
    this.setState({ publishesLoading: true });
    try {
      const res = await getExamPublishList();
      if (res && res.code === 200) {
        this.setState({ publishes: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取已发布列表失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取已发布列表失败'));
    } finally {
      this.setState({ publishesLoading: false });
    }
  };

  handlePublish = async () => {
    const { testPaperUid, scope, studentUsernames, deadline } = this.state;
    if (!testPaperUid) {
      message.error('请选择要发布的试卷');
      return;
    }
    if (!deadline) {
      message.error('请设置截止时间');
      return;
    }
    if (!deadline.isAfter(moment())) {
      message.error('截止时间必须晚于当前时间');
      return;
    }
    if (scope === 'students' && studentUsernames.length === 0) {
      message.error('请选择至少一名学生');
      return;
    }
    this.setState({ submitting: true });
    try {
      const res = await publishExam({
        testPaperUid,
        scope,
        studentUsernames: scope === 'students' ? studentUsernames : [],
        deadline: deadline.format('YYYY-MM-DD HH:mm:ss')
      });
      if (res && res.code === 200) {
        message.success(res.msg || '发布成功');
        this.setState({
          testPaperUid: undefined,
          studentUsernames: [],
          deadline: null
        });
        this.loadPublishes();
      } else {
        message.error((res && res.msg) || '发布失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '发布失败，请稍后重试'));
    } finally {
      this.setState({ submitting: false });
    }
  };

  openSubmissions = async record => {
    this.setState({
      submissionVisible: true,
      submissionLoading: true,
      submissionData: null,
      activePublish: record
    });
    try {
      const res = await getExamSubmissions({ id: record.id });
      if (res && res.code === 200) {
        this.setState({ submissionData: res.data || null });
      } else {
        message.error((res && res.msg) || '获取提交列表失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取提交列表失败'));
    } finally {
      this.setState({ submissionLoading: false });
    }
  };

  closeSubmissions = () => {
    this.setState({ submissionVisible: false, submissionData: null, activePublish: null });
  };

  openReport = async record => {
    this.setState({
      reportVisible: true,
      reportLoading: true,
      reportData: null,
      activePublish: record
    });
    try {
      const res = await getClassReport({ id: record.id });
      if (res && res.code === 200) {
        this.setState({ reportData: res.data || null });
      } else {
        message.error((res && res.msg) || '获取班级报告失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取班级报告失败'));
    } finally {
      this.setState({ reportLoading: false });
    }
  };

  closeReport = () => {
    this.setState({ reportVisible: false, reportData: null });
  };

  renderDimensionTable = (data, emptyText) => {
    const columns = [
      { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
      { title: '题量', dataIndex: 'questionCount', key: 'questionCount', width: 70 },
      { title: '正确', dataIndex: 'correctCount', key: 'correctCount', width: 70 },
      {
        title: '错误',
        key: 'wrongCount',
        width: 70,
        render: (_, record) => {
          const total = record.questionCount || 0;
          const correct = record.correctCount || 0;
          const pending = record.pendingCount || 0;
          return Math.max(0, total - correct - pending);
        }
      },
      { title: '待评', dataIndex: 'pendingCount', key: 'pendingCount', width: 70 },
      {
        title: '得分率',
        dataIndex: 'rate',
        key: 'rate',
        width: 180,
        render: rate => <Progress percent={Number(rate) || 0} size="small" />
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
    const {
      papers,
      papersLoading,
      students,
      studentsLoading,
      publishes,
      publishesLoading,
      testPaperUid,
      scope,
      studentUsernames,
      deadline,
      submitting,
      submissionVisible,
      submissionLoading,
      submissionData,
      activePublish,
      reportVisible,
      reportLoading,
      reportData
    } = this.state;

    const paperOptions = papers
      .filter(paper => paper.test_paper_uid)
      .map(paper => ({
        label: paper.test_paper_name || paper.test_paper_uid,
        value: paper.test_paper_uid
      }));
    const studentOptions = students.map(username => ({ label: username, value: username }));

    const publishColumns = [
      { title: '试卷名称', dataIndex: 'paperName', key: 'paperName', ellipsis: true },
      { title: '发布人', dataIndex: 'publisher', key: 'publisher', width: 120 },
      { title: '截止时间', dataIndex: 'deadline', key: 'deadline', width: 180, render: text => text || '未设置' },
      { title: '题量', dataIndex: 'questionCount', key: 'questionCount', width: 80 },
      { title: '总分', dataIndex: 'totalScore', key: 'totalScore', width: 80 },
      {
        title: '目标 / 提交',
        key: 'progress',
        width: 120,
        render: (_, record) => `${record.targetCount == null ? 0 : record.targetCount} / ${record.submittedCount == null ? 0 : record.submittedCount}`
      },
      {
        title: '状态',
        dataIndex: 'expired',
        key: 'expired',
        width: 100,
        render: expired => (expired
          ? <Tag color="red">已截止</Tag>
          : <Tag color="green">进行中</Tag>)
      },
      {
        title: '操作',
        key: 'action',
        width: 200,
        render: (_, record) => (
          <div>
            <Button type="link" onClick={() => this.openSubmissions(record)}>查看提交</Button>
            <Button type="link" onClick={() => this.openReport(record)}>班级报告</Button>
          </div>
        )
      }
    ];

    const submissionColumns = [
      { title: '学生', dataIndex: 'studentUsername', key: 'studentUsername' },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: text => {
          const config = SUBMISSION_STATUS[text] || { color: 'default', text: text || '—' };
          return <Tag color={config.color}>{config.text}</Tag>;
        }
      },
      { title: '提交时间', dataIndex: 'submitTime', key: 'submitTime', render: text => text || '—' },
      { title: '得分', dataIndex: 'score', key: 'score', render: value => (value == null ? '—' : value) },
      { title: '总分', dataIndex: 'totalScore', key: 'totalScore' },
      { title: '待评数', dataIndex: 'pendingCount', key: 'pendingCount' },
      { title: '未判分题数', dataIndex: 'ungradedCount', key: 'ungradedCount' }
    ];

    return (
      <div style={{ padding: '0 30px 40px' }}>
        <PageHeader
          title="考试管理"
          subTitle="发布试卷并查看学生提交与班级掌握情况"
          extra={[
            <Button key="refresh" icon={<ReloadOutlined />} onClick={this.loadPublishes}>
              刷新列表
            </Button>
          ]}
        />
        <Card size="small" title="发布试卷" style={{ marginBottom: 16 }}>
          <Form layout="vertical">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item label="选择试卷">
                  <Select
                    showSearch
                    optionFilterProp="label"
                    placeholder="请选择已组好的试卷"
                    value={testPaperUid}
                    loading={papersLoading}
                    options={paperOptions}
                    onChange={value => this.setState({ testPaperUid: value })}
                    notFoundContent={papersLoading ? '加载中' : <Empty description="暂无可发布试卷" />}
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="截止时间">
                  <DatePicker
                    showTime
                    style={{ width: '100%' }}
                    format="YYYY-MM-DD HH:mm:ss"
                    value={deadline}
                    placeholder="请选择截止时间"
                    onChange={value => this.setState({ deadline: value })}
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="发布范围">
                  <Radio.Group
                    value={scope}
                    onChange={e => this.setState({ scope: e.target.value, studentUsernames: [] })}
                  >
                    <Radio value="all">全班</Radio>
                    <Radio value="students">指定学生</Radio>
                  </Radio.Group>
                </Form.Item>
              </Col>
            </Row>
            <Form.Item label="指定学生（发布范围为指定学生时必选）">
              <Select
                mode="multiple"
                allowClear
                disabled={scope !== 'students'}
                placeholder={scope === 'students' ? '请选择学生' : '发布范围设为全班时无需选择'}
                value={studentUsernames}
                loading={studentsLoading}
                options={studentOptions}
                onChange={value => this.setState({ studentUsernames: value })}
                notFoundContent={studentsLoading ? '加载中' : <Empty description="暂无学生" />}
              />
            </Form.Item>
            <Button type="primary" icon={<PlusOutlined />} loading={submitting} onClick={this.handlePublish}>
              发布试卷
            </Button>
          </Form>
        </Card>
        <Card size="small" title="已发布考试">
          <Table
            rowKey="id"
            size="small"
            columns={publishColumns}
            dataSource={publishes}
            loading={publishesLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无已发布的考试" /> }}
          />
        </Card>

        <Modal
          title={activePublish ? `提交情况 - ${activePublish.paperName || ''}` : '提交情况'}
          visible={submissionVisible}
          onCancel={this.closeSubmissions}
          footer={null}
          width={900}
          destroyOnClose
        >
          <Table
            rowKey="studentUsername"
            size="small"
            columns={submissionColumns}
            dataSource={submissionData && Array.isArray(submissionData.students) ? submissionData.students : []}
            loading={submissionLoading}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无学生提交" /> }}
          />
        </Modal>

        <Modal
          title={activePublish ? `班级报告 - ${activePublish.paperName || ''}` : '班级报告'}
          visible={reportVisible}
          onCancel={this.closeReport}
          footer={null}
          width={1000}
          destroyOnClose
        >
          {reportLoading ? (
            <div style={{ padding: 40, textAlign: 'center' }}>加载中…</div>
          ) : (
            <div>
              <Descriptions size="small" column={3} bordered style={{ marginBottom: 16 }}>
                <Descriptions.Item label="试卷">{reportData && reportData.publish ? reportData.publish.paperName : '—'}</Descriptions.Item>
                <Descriptions.Item label="发布人">{reportData && reportData.publish ? reportData.publish.publisher : '—'}</Descriptions.Item>
                <Descriptions.Item label="截止时间">{reportData && reportData.publish ? reportData.publish.deadline : '—'}</Descriptions.Item>
                <Descriptions.Item label="总分">{reportData && reportData.publish ? reportData.publish.totalScore : '—'}</Descriptions.Item>
                <Descriptions.Item label="题量">{reportData && reportData.publish ? reportData.publish.questionCount : '—'}</Descriptions.Item>
                <Descriptions.Item label="学生数">{reportData && Array.isArray(reportData.students) ? reportData.students.length : 0}</Descriptions.Item>
              </Descriptions>
              <Card size="small" title="章节维度" style={{ marginBottom: 16 }}>
                {this.renderDimensionTable(reportData && reportData.chapters, '暂无章节数据')}
              </Card>
              <Card size="small" title="知识点维度">
                {this.renderDimensionTable(reportData && reportData.labels, '暂无知识点数据')}
              </Card>
            </div>
          )}
        </Modal>
      </div>
    );
  }
}
