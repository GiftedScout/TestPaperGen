import React from 'react';
import { PageHeader } from 'antd';
import AiConfigPanel from '../../components/AiConfigPanel';

/**
 * AI 配置页（管理员）。
 * 独立页面而非仅弹窗，保证入口在顶部导航中始终可见。
 */
class AiConfig extends React.Component {
  render() {
    return (
      <div>
        <PageHeader
          title="AI 解析配置"
          subTitle="配置 OpenAI 兼容接口、密钥与模型；支持模型列表拉取与连通性测试"
        />
        <AiConfigPanel />
      </div>
    );
  }
}

export default AiConfig;
