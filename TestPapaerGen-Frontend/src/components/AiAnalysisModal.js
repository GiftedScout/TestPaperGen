import React from 'react';
import { Button, Input, message, Modal, Spin, Tag } from 'antd';
import moment from 'moment';
import { checkCode } from '../../utils/myUtils';
import * as requestService from '../../services/requestServices';

const { TextArea } = Input;

/**
 * AI 解析弹窗。
 * 功能：查看解析 / 一键生成 / 手动编辑保存。
 * 生成失败时展示后端友好提示，不影响题库其他操作。
 */
class AiAnalysisModal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,   // 生成中
      saving: false,    // 保存中
      editing: false,   // 编辑态
      draft: '',        // 编辑中的解析草稿
    };
  }

  componentDidUpdate(prevProps) {
    // 每次打开弹窗时重置编辑态，载入当前题目解析
    if (!prevProps.visible && this.props.visible) {
      this.setState({ editing: false, draft: this.props.question ? (this.props.question.analysis || '') : '' });
    }
  }

  // 一键生成解析
  handleGenerate = () => {
    const question = this.props.question;
    if (!question) return;
    this.setState({ loading: true });
    requestService.generateAnalysis({ questionId: question.id })
      .then(res => {
        if (checkCode(res)) {
          message.success('解析生成成功');
          this.setState({ draft: res.data.analysis || '', editing: false });
          if (this.props.onGenerated) {
            this.props.onGenerated(res.data);
          }
        } else {
          message.error(res.msg || '解析生成失败');
        }
      })
      .catch(e => {
        console.log(e);
        message.error('解析生成失败，请检查网络或 AI 配置');
      })
      .finally(() => this.setState({ loading: false }));
  };

  // 保存手动编辑的解析
  handleSave = () => {
    const question = this.props.question;
    if (!question) return;
    if (!this.state.draft || !this.state.draft.trim()) {
      message.warning('解析内容不能为空');
      return;
    }
    this.setState({ saving: true });
    requestService.updateAnalysis({ id: question.id, analysis: this.state.draft })
      .then(res => {
        if (checkCode(res)) {
          message.success('保存成功');
          this.setState({ editing: false });
          if (this.props.onGenerated) {
            this.props.onGenerated({ ...question, analysis: this.state.draft });
          }
        } else {
          message.error(res.msg || '保存失败');
        }
      })
      .catch(e => {
        console.log(e);
        message.error('保存失败，请检查网络');
      })
      .finally(() => this.setState({ saving: false }));
  };

  render() {
    const { visible, question, onCancel } = this.props;
    const { loading, saving, editing, draft } = this.state;
    const hasAnalysis = question && question.analysis;

    return (
      <Modal
        title={question ? `试题解析 - #${question.id}` : '试题解析'}
        visible={visible}
        onCancel={onCancel}
        footer={[
          <Button key="close" onClick={onCancel}>关闭</Button>,
          <Button key="edit" onClick={() => this.setState({ editing: true })} disabled={!hasAnalysis || editing}>
            手动编辑
          </Button>,
          <Button key="generate" type="primary" loading={loading} onClick={this.handleGenerate}>
            {loading ? '生成中...' : 'AI 生成解析'}
          </Button>,
        ]}
        width={640}
      >
        <Spin spinning={loading}>
          <div style={{ marginBottom: 12 }}>
            <Tag color="blue">{question ? question.topic_type : ''}</Tag>
            {question && question.analysis_status === 1 && question.analysis_update_time ?
              <span style={{ color: '#666', fontSize: '0.85em' }}>
                更新于 {moment(question.analysis_update_time).format('YYYY-MM-DD HH:mm:ss')}
              </span> : null}
          </div>
          {editing ? (
            <TextArea
              value={draft}
              onChange={e => this.setState({ draft: e.target.value })}
              rows={10}
              placeholder="请输入试题解析"
            />
          ) : (
            <div style={{
              minHeight: 120,
              padding: '12px 16px',
              background: '#fafafa',
              border: '1px solid #f0f0f0',
              borderRadius: 4,
              whiteSpace: 'pre-wrap',
              lineHeight: 1.8,
            }}>
              {hasAnalysis ? question.analysis :
                <span style={{ color: '#999' }}>暂无解析，点击「AI 生成解析」一键生成。</span>}
            </div>
          )}
          {editing ? (
            <div style={{ marginTop: 12, textAlign: 'right' }}>
              <Button style={{ marginRight: 8 }} onClick={() => this.setState({ editing: false, draft: question ? (question.analysis || '') : '' })}>
                取消
              </Button>
              <Button type="primary" loading={saving} onClick={this.handleSave}>保存</Button>
            </div>
          ) : null}
        </Spin>
      </Modal>
    );
  }
}

export default AiAnalysisModal;
