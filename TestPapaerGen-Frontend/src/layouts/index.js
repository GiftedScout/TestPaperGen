import React from 'react';
import { connect, history } from 'umi';
import { Menu } from 'antd';
import {
  HomeOutlined,
  OrderedListOutlined,
  EditOutlined,
  FileAddOutlined,
  HistoryOutlined,
  UserOutlined,
  LoginOutlined,
  ApiOutlined,
  FormOutlined,
  BarChartOutlined,
  ScheduleOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons';
import ExamSocket from '../components/ExamSocket';

class TopMenu extends React.Component {
  constructor(props) {
    super(props);
    this.state = {}
  }

  componentWillMount() {
    this.props.dispatch({type: 'loginModel/getLoginStatus'})
  }

  render() {
    const onClick = e => {
      if (history.location.pathname !== e.key) history.push(e.key);
    };
    const menu = () => {
      if (history.location.pathname === "/") {
        return null;
      }
      else if (history.location.pathname === "admin") {
        const items = [
          {
            key: "/admin",
            icon: <HomeOutlined />,
            label: "管理员首页"
          },
          {
            key: "/aiConfig",
            icon: <ApiOutlined />,
            label: "AI 配置"
          },
          {
            key: "/login",
            icon: <LoginOutlined />,
            label: "退出"
          }
        ]
        return <Menu items={items} mode="horizontal" theme='dark' onClick={onClick} selectedKeys={history.location.pathname} />
      }
      else {
        const userExamItems = this.props.user_role === 'user' ? [
          {
            key: "/exam",
            icon: <FormOutlined />,
            label: "我的考试"
          },
          {
            key: "/examReport",
            icon: <BarChartOutlined />,
            label: "学习报告"
          }
        ] : [];
        const teacherExamItems = (this.props.user_role === 'teacher' || this.props.user_role === 'admin') ? [
          {
            key: "/examManage",
            icon: <ScheduleOutlined />,
            label: "考试管理"
          },
          {
            key: "/answerAudit",
            icon: <SafetyCertificateOutlined />,
            label: "答案治理"
          }
        ] : [];
        const items = [
          {
            key: "/home",
            icon: <HomeOutlined />,
            label: "首页"
          },
          {
            key: "/questionBank",
            icon: <OrderedListOutlined />,
            label: "试题库显示"
          },
          {
            key: "/questionEdit",
            icon: <EditOutlined />,
            label: "添加或修改"
          },
          {
            key: "/questionGenerator",
            icon: <FileAddOutlined />,
            label: "组卷功能"
          },
          {
            key: "/questionGenHistory",
            icon: <HistoryOutlined />,
            label: "出题历史"
          },
          this.props.user_role === 'admin' ? {
            key: "/aiConfig",
            icon: <ApiOutlined />,
            label: "AI 配置"
          } : null,
          ...userExamItems,
          ...teacherExamItems,
          {
            key: "/login",
            icon: <UserOutlined />,
            label: `退出登陆：${this.props.username}`
          }
        ].filter(Boolean)
        return <Menu items={items}
                     mode="horizontal"
                     theme='dark'
                     onClick={onClick}
                     selectedKeys={history.location.pathname}
        />
      }
    }

    return <div>
        <div id="headers">
          {
            menu()
          }
        </div>
        <div id="contents">
          { this.props.children }
        </div>
        { this.props.isLogin && this.props.user_role ? <ExamSocket /> : null }
    </div>;
  }
}

function mapStateToProps({ loginModel }) {
  const { username, user_role, isLogin } = loginModel;
  return { username, user_role, isLogin };
}

export default connect(mapStateToProps)(TopMenu);
