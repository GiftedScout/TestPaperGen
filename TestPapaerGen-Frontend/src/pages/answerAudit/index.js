import React from 'react';
import {
  Button,
  Card,
  Checkbox,
  Col,
  Empty,
  Input,
  Modal,
  PageHeader,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  message
} from 'antd';
import { ThunderboltOutlined } from '@ant-design/icons';
import {
  getAnswerPending,
  suggestAnswers,
  confirmAnswer,
  rejectAnswer,
  getAnswerAudits,
  getAnswerSampling,
  getQuestionBank
} from '../../services/requestServices';

const LOW_CONFIDENCE = 0.9;

const TYPE_COLOR = {
  选择题: 'volcano',
  判断题: 'purple',
  填空题: 'blue'
};

const AUDIT_STATUS = {
  suggested: { color: 'blue', text: '待确认' },
  confirmed: { color: 'green', text: '已确认' },
  rejected: { color: 'red', text: '已驳回' },
  failed: { color: 'red', text: '处理失败' }
};

const SOURCE_TEXT = {
  ai: 'AI',
  mock: '模拟'
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

function formatSource(source) {
  if (!source) {
    return '—';
  }
  return SOURCE_TEXT[source] || source;
}

function formatConfidence(confidence) {
  if (confidence == null) {
    return '—';
  }
  return `${(Number(confidence) * 100).toFixed(1)}%`;
}

export default class AnswerAudit extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      list: [],
      bankMap: {},
      lowOnly: false,
      selectedRowKeys: [],
      suggesting: false,
      sampling: null,
      samplingLoading: false,
      editVisible: false,
      editRecord: null,
      editValue: '',
      editSubmitting: false,
      rejectVisible: false,
      rejectRecord: null,
      rejectReason: '',
      rejectSubmitting: false,
      auditVisible: false,
      auditLoading: false,
      auditQuestionId: null,
      auditList: []
    };
  }

  componentDidMount() {
    this.loadPending();
    this.loadBankMap();
    this.loadSampling();
  }

  loadPending = async (lowOnlyValue) => {
    const lowOnly = typeof lowOnlyValue === 'boolean' ? lowOnlyValue : this.state.lowOnly;
    this.setState({ loading: true, selectedRowKeys: [] });
    try {
      const res = await getAnswerPending({ lowOnly });
      if (res && res.code === 200) {
        this.setState({ list: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取待确认答案失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取待确认答案失败'));
    } finally {
      this.setState({ loading: false });
    }
  };

  loadBankMap = async () => {
    try {
      const res = await getQuestionBank();
      if (res && res.code === 200 && Array.isArray(res.data)) {
        const bankMap = {};
        res.data.forEach(bank => {
          if (bank && bank.id != null) {
            bankMap[bank.id] = bank;
          }
        });
        this.setState({ bankMap });
      }
    } catch (e) {
      // 章节/知识点为辅助展示信息，失败不阻塞主流程
    }
  };

  loadSampling = async () => {
    this.setState({ samplingLoading: true });
    try {
      const res = await getAnswerSampling();
      if (res && res.code === 200) {
        this.setState({ sampling: res.data || null });
      } else {
        message.error((res && res.msg) || '获取抽检准确率失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取抽检准确率失败'));
    } finally {
      this.setState({ samplingLoading: false });
    }
  };

  handleLowOnlyChange = event => {
    const checked = event.target.checked;
    this.setState({ lowOnly: checked });
    this.loadPending(checked);
  };

  handleBatchSuggest = async () => {
    const questionIds = this.state.selectedRowKeys;
    if (!questionIds.length) {
      message.warning('请先勾选要生成建议的题目');
      return;
    }
    this.setState({ suggesting: true });
    try {
      const res = await suggestAnswers({ questionIds });
      if (res && res.code === 200) {
        const results = Array.isArray(res.data) ? res.data : [];
        const failed = results.filter(item => item && item.status === 'failed').length;
        const success = results.length - failed;
        message.success(`已生成建议 ${success} 条${failed ? `，失败 ${failed} 条` : ''}`);
        this.loadPending();
      } else {
        message.error((res && res.msg) || '生成建议失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '生成建议失败'));
    } finally {
      this.setState({ suggesting: false });
    }
  };

  doConfirm = async (auditId, finalAnswer) => {
    try {
      const res = await confirmAnswer({ auditId, finalAnswer });
      if (res && res.code === 200) {
        message.success(res.msg || '确认成功');
        this.loadPending();
        this.loadSampling();
        return true;
      }
      message.error((res && res.msg) || '确认失败');
      return false;
    } catch (e) {
      message.error(extractMessage(e, '确认失败'));
      return false;
    }
  };

  adoptSuggestion = record => {
    if (!record.latestAuditId) {
      message.warning('该题尚无 AI 建议，请先生成建议');
      return;
    }
    if (!record.suggestedAnswer) {
      message.warning('该题没有可采纳的建议值');
      return;
    }
    Modal.confirm({
      title: '采纳 AI 建议并确认？',
      content: `确认后标准答案将变为「${record.suggestedAnswer}」，立即生效。`,
      okText: '确认采纳',
      cancelText: '取消',
      onOk: () => this.doConfirm(record.latestAuditId, record.suggestedAnswer)
    });
  };

  openEdit = record => {
    if (!record.latestAuditId) {
      message.warning('该题尚无 AI 建议，请先生成建议');
      return;
    }
    this.setState({
      editVisible: true,
      editRecord: record,
      editValue: record.suggestedAnswer || record.currentAnswer || ''
    });
  };

  handleEditConfirm = async () => {
    const { editRecord, editValue } = this.state;
    if (!editValue || !editValue.trim()) {
      message.error('请填写确认后的标准答案');
      return;
    }
    this.setState({ editSubmitting: true });
    const success = await this.doConfirm(editRecord.latestAuditId, editValue.trim());
    this.setState({ editSubmitting: false });
    if (success) {
      this.setState({ editVisible: false, editRecord: null, editValue: '' });
    }
  };

  openReject = record => {
    if (!record.latestAuditId) {
      message.warning('该题尚无 AI 建议，无需驳回');
      return;
    }
    this.setState({ rejectVisible: true, rejectRecord: record, rejectReason: '' });
  };

  handleReject = async () => {
    const { rejectRecord, rejectReason } = this.state;
    if (!rejectReason || !rejectReason.trim()) {
      message.error('请填写驳回原因');
      return;
    }
    this.setState({ rejectSubmitting: true });
    try {
      const res = await rejectAnswer({ auditId: rejectRecord.latestAuditId, reason: rejectReason.trim() });
      if (res && res.code === 200) {
        message.success(res.msg || '已驳回');
        this.setState({ rejectVisible: false, rejectRecord: null, rejectReason: '' });
        this.loadPending();
      } else {
        message.error((res && res.msg) || '驳回失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '驳回失败'));
    } finally {
      this.setState({ rejectSubmitting: false });
    }
  };

  openAudits = async record => {
    this.setState({
      auditVisible: true,
      auditLoading: true,
      auditList: [],
      auditQuestionId: record.questionId
    });
    try {
      const res = await getAnswerAudits({ questionId: record.questionId });
      if (res && res.code === 200) {
        this.setState({ auditList: Array.isArray(res.data) ? res.data : [] });
      } else {
        message.error((res && res.msg) || '获取审计记录失败');
      }
    } catch (e) {
      message.error(extractMessage(e, '获取审计记录失败'));
    } finally {
      this.setState({ auditLoading: false });
    }
  };

  renderSampling = () => {
    const { sampling } = this.state;
    const types = sampling && Array.isArray(sampling.types) ? sampling.types : [];
    const threshold = sampling && sampling.threshold != null ? sampling.threshold : 90;
    return (
      <Card
        size="small"
        title="抽检准确率"
        style={{ marginBottom: 16 }}
        extra={<span style={{ color: '#888' }}>达标阈值 {threshold}%</span>}
      >
        {types.length === 0 ? (
          <Empty description="暂无已确认的抽检数据" />
        ) : (
          <Row gutter={16}>
            {types.map(item => {
              const accuracy = item.accuracy;
              const low = accuracy != null && Number(accuracy) < Number(threshold);
              return (
                <Col span={8} key={item.topicType}>
                  <Card size="small">
                    <Statistic
                      title={item.topicType}
                      value={accuracy == null ? '—' : accuracy}
                      suffix={accuracy == null ? '' : '%'}
                      valueStyle={low ? { color: '#cf1322' } : undefined}
                    />
                    <div style={{ color: '#666', marginTop: 4 }}>
                      已确认 {item.confirmedCount == null ? 0 : item.confirmedCount}
                      {'　'}未修改 {item.unchangedCount == null ? 0 : item.unchangedCount}
                    </div>
                    <div style={{ marginTop: 8 }}>
                      {accuracy == null ? (
                        <Tag>暂无数据</Tag>
                      ) : item.pass ? (
                        <Tag color="green">达标</Tag>
                      ) : (
                        <Tag color="red">低于 {threshold}%</Tag>
                      )}
                    </div>
                  </Card>
                </Col>
              );
            })}
          </Row>
        )}
        {sampling && sampling.method ? (
          <div style={{ color: '#999', marginTop: 12, fontSize: 12 }}>{sampling.method}</div>
        ) : null}
      </Card>
    );
  };

  render() {
    const {
      loading,
      list,
      bankMap,
      lowOnly,
      selectedRowKeys,
      suggesting,
      samplingLoading,
      editVisible,
      editRecord,
      editValue,
      editSubmitting,
      rejectVisible,
      rejectReason,
      rejectSubmitting,
      auditVisible,
      auditLoading,
      auditQuestionId,
      auditList
    } = this.state;

    const columns = [
      {
        title: '题干摘要',
        dataIndex: 'topic',
        key: 'topic',
        width: 280,
        ellipsis: true
      },
      {
        title: '题型',
        dataIndex: 'topicType',
        key: 'topicType',
        width: 100,
        render: text => <Tag color={TYPE_COLOR[text] || 'default'}>{text || '—'}</Tag>
      },
      {
        title: '章节',
        key: 'chapter',
        width: 160,
        ellipsis: true,
        render: (_, record) => {
          const bank = bankMap[record.questionId];
          if (!bank) {
            return '—';
          }
          return [bank.chapter_1, bank.chapter_2].filter(Boolean).join(' / ') || '—';
        }
      },
      {
        title: '知识点',
        key: 'label',
        width: 160,
        ellipsis: true,
        render: (_, record) => {
          const bank = bankMap[record.questionId];
          if (!bank) {
            return '—';
          }
          return [bank.label_1, bank.label_2].filter(Boolean).join(' / ') || '—';
        }
      },
      {
        title: '当前答案',
        dataIndex: 'currentAnswer',
        key: 'currentAnswer',
        width: 120,
        render: text => text || '—'
      },
      {
        title: 'AI 建议',
        dataIndex: 'suggestedAnswer',
        key: 'suggestedAnswer',
        width: 120,
        render: text => text || '—'
      },
      {
        title: '置信度',
        dataIndex: 'confidence',
        key: 'confidence',
        width: 140,
        render: confidence => {
          if (confidence == null) {
            return <Tag>—</Tag>;
          }
          if (Number(confidence) < LOW_CONFIDENCE) {
            return <Tag color="red">低置信度 {formatConfidence(confidence)}</Tag>;
          }
          return <Tag color="green">{formatConfidence(confidence)}</Tag>;
        }
      },
      {
        title: '来源',
        dataIndex: 'source',
        key: 'source',
        width: 90,
        render: source => formatSource(source)
      },
      {
        title: '状态',
        dataIndex: 'auditStatus',
        key: 'auditStatus',
        width: 110,
        render: status => {
          const config = AUDIT_STATUS[status];
          if (!config) {
            return <Tag>待反推</Tag>;
          }
          return <Tag color={config.color}>{config.text}</Tag>;
        }
      },
      {
        title: '操作',
        key: 'action',
        width: 300,
        render: (_, record) => (
          <Space size={0} wrap>
            <Button
              type="link"
              disabled={!record.latestAuditId || !record.suggestedAnswer}
              onClick={() => this.adoptSuggestion(record)}
            >
              采纳建议
            </Button>
            <Button type="link" disabled={!record.latestAuditId} onClick={() => this.openEdit(record)}>
              编辑后确认
            </Button>
            <Button type="link" danger disabled={!record.latestAuditId} onClick={() => this.openReject(record)}>
              驳回
            </Button>
            <Button type="link" onClick={() => this.openAudits(record)}>
              查看审计
            </Button>
          </Space>
        )
      }
    ];

    return (
      <div style={{ padding: '0 30px 40px' }}>
        <PageHeader
          title="答案治理"
          subTitle="AI 只产出建议，人工确认是标准答案生效的唯一入口"
        />
        {this.renderSampling()}
        <Card size="small" title="待确认答案">
          <div style={{ marginBottom: 12 }}>
            <Space size="middle">
              <Checkbox checked={lowOnly} onChange={this.handleLowOnlyChange}>
                仅看低置信度
              </Checkbox>
              <Button onClick={() => this.loadPending()} loading={loading}>
                刷新
              </Button>
              <Button
                type="primary"
                icon={<ThunderboltOutlined />}
                loading={suggesting}
                disabled={!selectedRowKeys.length}
                onClick={this.handleBatchSuggest}
              >
                批量生成建议
              </Button>
              <span style={{ color: '#888' }}>
                已选 {selectedRowKeys.length} 题（生成建议不会直接修改答案）
              </span>
            </Space>
          </div>
          <Table
            rowKey="questionId"
            size="small"
            columns={columns}
            dataSource={list}
            loading={loading || samplingLoading}
            scroll={{ x: 1700 }}
            pagination={{ pageSize: 10, showSizeChanger: false }}
            rowSelection={{
              selectedRowKeys,
              onChange: keys => this.setState({ selectedRowKeys: keys })
            }}
            locale={{ emptyText: <Empty description="暂无待确认答案" /> }}
          />
        </Card>

        <Modal
          title="编辑后确认"
          visible={editVisible}
          onCancel={() => this.setState({ editVisible: false, editRecord: null, editValue: '' })}
          onOk={this.handleEditConfirm}
          confirmLoading={editSubmitting}
          okText="确认并生效"
          cancelText="取消"
          destroyOnClose
        >
          <div style={{ marginBottom: 8, color: '#666' }}>
            题目：{editRecord ? editRecord.topic : ''}
          </div>
          <Input
            value={editValue}
            onChange={e => this.setState({ editValue: e.target.value })}
            placeholder="填写确认后的标准答案"
          />
          <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
            选择题请填选项字母组合（如 AC），判断题填「对」或「错」，填空题多空用 | 分隔。
          </div>
        </Modal>

        <Modal
          title="驳回建议"
          visible={rejectVisible}
          onCancel={() => this.setState({ rejectVisible: false, rejectRecord: null, rejectReason: '' })}
          onOk={this.handleReject}
          confirmLoading={rejectSubmitting}
          okText="确认驳回"
          cancelText="取消"
          okButtonProps={{ danger: true }}
          destroyOnClose
        >
          <div style={{ marginBottom: 8, color: '#666' }}>
            题目：{this.state.rejectRecord ? this.state.rejectRecord.topic : ''}
          </div>
          <Input.TextArea
            rows={3}
            value={rejectReason}
            onChange={e => this.setState({ rejectReason: e.target.value })}
            placeholder="请填写驳回原因"
          />
        </Modal>

        <Modal
          title={`审计记录${auditQuestionId ? ` - 题目 ${auditQuestionId}` : ''}`}
          visible={auditVisible}
          onCancel={() => this.setState({ auditVisible: false, auditList: [], auditQuestionId: null })}
          footer={null}
          width={720}
          destroyOnClose
        >
          {auditLoading ? (
            <div style={{ padding: 30, textAlign: 'center' }}>加载中…</div>
          ) : auditList.length === 0 ? (
            <Empty description="暂无审计记录" />
          ) : (
            <Timeline>
              {auditList.map(item => {
                const config = AUDIT_STATUS[item.status] || { color: 'gray', text: item.status || '未知' };
                return (
                  <Timeline.Item key={item.id} color={config.color}>
                    <div>
                      <Tag color={config.color}>{config.text}</Tag>
                      <span style={{ color: '#999', marginLeft: 8 }}>#{item.id}</span>
                    </div>
                    <div>原答案：{item.oldAnswer || '—'}</div>
                    <div>建议值：{item.suggestedAnswer || '—'}（置信度 {formatConfidence(item.confidence)}，来源 {formatSource(item.source)}）</div>
                    {item.confirmedAnswer ? <div>确认值：{item.confirmedAnswer}</div> : null}
                    {item.failReason ? <div style={{ color: '#cf1322' }}>失败原因：{item.failReason}</div> : null}
                    <div style={{ color: '#888' }}>
                      创建时间：{item.createTime || '—'}
                      {item.confirmUser ? `　确认人：${item.confirmUser}` : ''}
                      {item.confirmTime ? `　确认时间：${item.confirmTime}` : ''}
                    </div>
                  </Timeline.Item>
                );
              })}
            </Timeline>
          )}
        </Modal>
      </div>
    );
  }
}
