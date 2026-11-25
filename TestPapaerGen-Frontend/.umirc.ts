import { defineConfig } from 'umi';

export default defineConfig({
  // 加速构建
  mfsu: {},
  // cdn目录
  publicPath: "/static/",

  // 按需加载 暂时不需要
  // dynamicImport: {
  //   loading: "@/layouts/loading"
  // },

  // 国际化
  locale: {
    default: 'zh-CN',
    antd: true
  },
  history: {
    type: 'hash'
  },
  routes: [
    {
      path: '/',
      component: '@/layouts/index',
      routes: [

        // 登陆
        { path: '/',  exact: true, component: '@/pages/login', title: "欢迎使用《组卷系统》" },
        { path: '/login',  exact: true, redirect: '/' },

        // 管理员
        {exact: true, path: '/admin', component: '@/pages/admin', title: "组卷系统-管理员"},
        {exact: true, path: '/aiConfig', component: '@/pages/aiConfig', title: "组卷系统-AI配置"},

        // 用户
        {exact: true, path: '/home', component: '@/pages/home', title: "组卷系统-首页"},
        {exact: true, path: '/questionBank', component: '@/pages/questionBank', title: "组卷系统-题库"},
        {exact: true, path: '/questionEdit', component: '@/pages/questionEdit', title: "组卷系统-编辑"},
        {exact: true, path: '/questionGenerator', component: '@/pages/questionGenerator', title: "组卷系统-组卷"},
        {exact: true, path: '/questionGenHistory', component: '@/pages/questionGenHistory', title: "组卷系统-历史记录"},
        // {exact: true, path: '/questionManager', component: '@/pages/questionManager', title: "组卷系统"}

        // 任务包 A：学生端考试闭环
        {exact: true, path: '/exam', component: '@/pages/exam', title: "组卷系统-我的考试"},
        {exact: true, path: '/exam/answer/:publishId', component: '@/pages/exam/answer', title: "组卷系统-在线作答"},
        {exact: true, path: '/exam/result/:publishId', component: '@/pages/exam/result', title: "组卷系统-作答结果"},
        {exact: true, path: '/examReport', component: '@/pages/examReport', title: "组卷系统-学习报告"},
        {exact: true, path: '/examManage', component: '@/pages/examManage', title: "组卷系统-考试管理"},
        {exact: true, path: '/answerAudit', component: '@/pages/answerAudit', title: "组卷系统-答案治理"}
      ]
    }
  ],
  plugins: [],
});
