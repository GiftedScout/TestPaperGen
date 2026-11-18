import React from 'react';
import { Button, Modal } from 'antd';
import AiConfigPanel from './AiConfigPanel';

/**
 * AI 配置弹窗：面板逻辑复用 AiConfigPanel，弹窗只负责容器。
 */
class AiConfigModal extends React.Component {
  render() {
    const { visible, onCancel } = this.props;
    return (
      <Modal
        title="AI 解析配置（管理员）"
        visible={visible}
        onCancel={onCancel}
        width={780}
        footer={[<Button key="close" onClick={onCancel}>关闭</Button>]}
      >
        {visible ? <AiConfigPanel /> : null}
      </Modal>
    );
  }
}

export default AiConfigModal;
