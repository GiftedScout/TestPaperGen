import React from 'react';
import { Button, Modal, Table, Tag, Typography } from 'antd';

const { Paragraph } = Typography;

/**
 * 批量生成解析结果弹窗。
 * 展示逐题生成结果（成功/失败与原因），失败不影响其他题目。
 * 关闭时由父组件刷新题库列表，保证解析状态列同步。
 */
class AiBatchResultModal extends React.Component {
  render() {
    const { visible, onCancel, results } = this.props;
    const list = results || [];
    const successCount = list.filter(item => item.success).length;
    const failCount = list.length - successCount;

    const columns = [
      {
        title: '#',
        dataIndex: 'key',
        key: 'key',
        width: 55,
      },
      {
        title: '题目ID',
        dataIndex: 'questionId',
        key: 'questionId',
        width: 90,
      },
      {
        title: '状态',
        dataIndex: 'success',
        key: 'success',
        width: 90,
        render: (success) => success ?
          <Tag color='green'>成功</Tag> :
          <Tag color='red'>失败</Tag>,
      },
      {
        title: '详情',
        dataIndex: 'success',
        key: 'detail',
        render: (success, record) => success ?
          <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
            {record.analysis || '已生成'}
          </Paragraph> :
          <span style={{ color: '#ff4d4f' }}>{record.error || '未知错误'}</span>,
      },
    ];

    const dataSource = list.map((item, index) => ({ ...item, key: index }));

    return (
      <Modal
        title={`批量生成结果（共 ${list.length} 题，成功 ${successCount}，失败 ${failCount}）`}
        visible={visible}
        onCancel={onCancel}
        footer={[
          <Button key="close" type="primary" onClick={onCancel}>关闭</Button>,
        ]}
        width={720}
      >
        <Table
          columns={columns}
          dataSource={dataSource}
          pagination={{ pageSize: 10, hideOnSinglePage: true }}
          size='small'
        />
      </Modal>
    );
  }
}

export default AiBatchResultModal;
