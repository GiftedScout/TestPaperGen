import React from 'react';
import { Alert, Button, Input, message, Select, Switch, Tag } from 'antd';
import { CloudDownloadOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { checkCode } from '../utils/myUtils';
import * as requestService from '../services/requestServices';

const { Password } = Input;

/**
 * AI 配置面板（仅管理员可用，后端二次校验 user_role）。
 *
 * 能力：启用开关、接口地址、模型名、API Key；拉取模型列表；连通性测试；保存生效。
 * 安全：apiKey 只在组件内存中存在，提交成功后立即清空，不写 localStorage/cookie；
 *       后端只存运行时内存，status 仅回显脱敏串。
 */
class AiConfigPanel extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      saving: false,
      testing: false,
      modelsLoading: false,
      status: null,
      enabled: false,
      baseUrl: '',
      model: '',
      apiKey: '',
      models: [],
      testResult: null,
    };
  }

  componentDidMount() {
    this.loadStatus();
  }

  loadStatus = () => {
    requestService.getAiStatus()
      .then(res => {
        if (checkCode(res)) {
          const s = res.data || {};
          this.setState({
            status: s,
            enabled: !!s.enabled,
            baseUrl: s.baseUrl || '',
            model: s.model || '',
          });
        }
      })
      .catch(e => console.log(e));
  };

  buildProbePayload = () => {
    const { baseUrl, model, apiKey } = this.state;
    const payload = { baseUrl: baseUrl.trim(), model: model.trim() };
    if (apiKey.trim()) {
      payload.apiKey = apiKey.trim();
    }
    return payload;
  };

  handleFetchModels = () => {
    const { baseUrl } = this.state;
    if (!baseUrl.trim()) {
      message.warning('请先填写接口地址');
      return;
    }
    this.setState({ modelsLoading: true });
    requestService.listAiModels(this.buildProbePayload())
      .then(res => {
        if (checkCode(res)) {
          const list = (res.data || []).map(x => ({ label: x.id, value: x.id }));
          this.setState({ models: list });
          message.success(list.length ? `获取到 ${list.length} 个模型` : '接口未返回模型');
        } else {
          message.error(res.msg || '模型列表获取失败');
        }
      })
      .catch(e => {
        console.log(e);
        message.error('模型列表获取失败，请检查地址与密钥');
      })
      .finally(() => this.setState({ modelsLoading: false }));
  };

  handleTest = () => {
    const { baseUrl } = this.state;
    if (!baseUrl.trim()) {
      message.warning('请先填写接口地址');
      return;
    }
    this.setState({ testing: true, testResult: null });
    requestService.testAiConnection(this.buildProbePayload())
      .then(res => {
        if (checkCode(res)) {
          this.setState({ testResult: res.data });
          if (res.data && res.data.ok) {
            message.success(`连接成功，耗时 ${res.data.latencyMs} ms`);
          } else {
            message.error((res.data && res.data.error) || '连接失败');
          }
        } else {
          message.error(res.msg || '连接测试失败');
        }
      })
      .catch(e => {
        console.log(e);
        message.error('连接测试失败，请检查网络');
      })
      .finally(() => this.setState({ testing: false }));
  };

  handleSave = () => {
    const { enabled, status } = this.state;
    const payload = this.buildProbePayload();
    if (enabled && !payload.apiKey && !(status && status.apiKeyConfigured)) {
      message.warning('启用 AI 需要填写 API Key');
      return;
    }
    payload.enabled = enabled;

    this.setState({ saving: true });
    requestService.saveAiConfig(payload)
      .then(res => {
        if (checkCode(res)) {
          message.success(res.msg || 'AI 配置已生效');
          this.setState({ apiKey: '', testResult: null });
          this.loadStatus();
          if (this.props.onSaved) {
            this.props.onSaved(res.data);
          }
        } else {
          message.error(res.msg || '保存失败');
        }
      })
      .catch(e => {
        console.log(e);
        message.error('保存失败，请检查网络或权限');
      })
      .finally(() => this.setState({ saving: false }));
  };

  render() {
    const { saving, testing, modelsLoading, status, enabled, baseUrl, model, apiKey, models, testResult } = this.state;

    return (
      <div style={{ maxWidth: 760 }}>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="密钥只写入后端内存，不入库、不写文件、不回显明文；后端重启后恢复为配置文件中的值。"
        />

        <div style={{ marginBottom: 14 }}>
          当前模式：
          <Tag color={status && status.enabled ? 'green' : 'default'}>
            {status ? (status.enabled ? '真实模型' : 'Mock 占位') : '-'}
          </Tag>
          <span style={{ marginLeft: 16 }}>
            已配置密钥：{status && status.apiKeyConfigured
              ? <code>{status.apiKeyMasked}</code>
              : <span style={{ color: '#999' }}>未配置</span>}
          </span>
        </div>

        <div style={{ marginBottom: 16 }}>
          <span style={{ display: 'inline-block', width: 96 }}>启用 AI：</span>
          <Switch checked={enabled} onChange={v => this.setState({ enabled: v })} />
          <span style={{ marginLeft: 8, color: '#888', fontSize: '0.85em' }}>关闭时使用 Mock 占位解析</span>
        </div>

        <div style={{ marginBottom: 14 }}>
          <div style={{ marginBottom: 4 }}>接口地址（OpenAI 兼容）：</div>
          <Input
            value={baseUrl}
            onChange={e => this.setState({ baseUrl: e.target.value })}
            placeholder="https://api.deepseek.com/v1"
          />
        </div>

        <div style={{ marginBottom: 14 }}>
          <div style={{ marginBottom: 4 }}>模型名称：</div>
          <div style={{ display: 'flex', gap: 8 }}>
            <Select
              style={{ flex: 1 }}
              showSearch
              allowClear
              placeholder={models.length ? '从列表选择' : '先点右侧「拉取模型」'}
              value={model || undefined}
              options={models}
              onChange={v => this.setState({ model: v || '' })}
              notFoundContent="暂无列表，可直接在下方输入模型名"
            />
            <Button icon={<CloudDownloadOutlined />} loading={modelsLoading} onClick={this.handleFetchModels}>
              拉取模型
            </Button>
          </div>
          <Input
            style={{ marginTop: 8 }}
            value={model}
            onChange={e => this.setState({ model: e.target.value })}
            placeholder="也可手动输入，如 deepseek-chat / gpt-4o-mini"
          />
        </div>

        <div style={{ marginBottom: 4 }}>API Key：</div>
        <Password
          value={apiKey}
          autoComplete="new-password"
          onChange={e => this.setState({ apiKey: e.target.value })}
          placeholder={status && status.apiKeyConfigured ? '已配置，留空表示不修改' : 'sk-...'}
        />
        <div style={{ marginTop: 6, marginBottom: 16, color: '#888', fontSize: '0.85em' }}>
          留空表示不修改已有密钥；保存成功后输入框立即清空。测试连接使用当前输入值，不保存。
        </div>

        <div style={{ marginBottom: 12 }}>
          <Button icon={<ThunderboltOutlined />} loading={testing} onClick={this.handleTest} style={{ marginRight: 8 }}>
            测试连接
          </Button>
          <Button type="primary" loading={saving} onClick={this.handleSave}>保存并生效</Button>
        </div>

        {testResult ? (
          testResult.ok ? (
            <Alert type="success" showIcon
                   message={`连通正常：${testResult.model}，耗时 ${testResult.latencyMs} ms`} />
          ) : (
            <Alert type="error" showIcon message="连接失败" description={testResult.error || '未知错误'} />
          )
        ) : null}
      </div>
    );
  }
}

export default AiConfigPanel;
