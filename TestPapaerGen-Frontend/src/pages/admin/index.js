import React from 'react';
import { connect } from 'umi';
import { Button, Divider, PageHeader } from 'antd';
import { ApiOutlined } from '@ant-design/icons';
import AdminTable from './adminTable';
import AllUserTable from './allUserTable';
import AiConfigModal from '../../components/AiConfigModal';


class Admin extends React.Component {
  constructor(props) {
    super(props);
    this.state = { aiConfigVisible: false }
  }

  // handle

  // life cycle
  initData = async () => {

  };

  componentWillMount() {
    this.initData().then(()=>null)
  }

  render() {
    return (
      <div>
        <PageHeader title={'管理员账户管理'} subTitle={'支持对用户账号进行管理'}
                    extra={[
                      <Button key="aiConfig" icon={<ApiOutlined />}
                              onClick={() => this.setState({ aiConfigVisible: true })}>AI 配置</Button>
                    ]} />
        <Divider orientation='left' style={{fontWeight: 'bold'}}>等待审批的注册教工用户</Divider>
        <AdminTable dataSource={this.props.applyUser}
                    dispatch={this.props.dispatch}
        />
        <Divider orientation='left' style={{fontWeight: 'bold'}}>当前系统用户管理</Divider>
        <AllUserTable
          dataSource={this.props.allUser}
          dispatch={this.props.dispatch}
        />
        <AiConfigModal
          visible={this.state.aiConfigVisible}
          onCancel={() => this.setState({ aiConfigVisible: false })}
        />
      </div>
    )
  }


}

function mapStateToProps({ loginModel }) {
  const { applyUser, allUser } = loginModel;
  return { applyUser, allUser };
}

export default connect(mapStateToProps)(Admin);
