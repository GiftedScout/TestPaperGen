import generatorModel from './questionGeneratorModel';
import historyModel from './questionGenHistoryModel';
import * as requests from '../services/requestServices';
import { downloadWord } from '../utils/downloadWord';
import { message } from 'antd';

jest.mock('antd', () => ({ message: { success: jest.fn(), error: jest.fn() } }));
jest.mock('../services/requestServices', () => ({
  questionGen2: jest.fn(), reExportTestPaper: jest.fn(),
}));
jest.mock('../utils/myUtils', () => ({ isArray: Array.isArray }));
jest.mock('../utils/downloadWord', () => ({ downloadWord: jest.fn() }));

const call = (fn, ...args) => ({ fn, args });
const state = { questionGenerator: {
  testPaperGenList: [{ id: 1 }], TKTList: [{ id: 2 }],
  XZTList: [], PDTList: [], JDTList: [],
} };

beforeEach(() => jest.clearAllMocks());

test.each([undefined, false, true])('new export forwards flag %s and waits for download', flag => {
  const gen = generatorModel.effects.questionGen2({ payload: {
    testPaperName: '试卷', randomSwitch: true, withAnalysis: flag,
  } }, { select: fn => fn(state), call });
  let step = gen.next();
  while (!step.done && !(step.value && step.value.fn)) {
    step = gen.next(step.value);
  }
  expect(step.value.fn).toBe(requests.questionGen2);
  expect(step.value.args[0]).toMatchObject({
    withAnalysis: flag === true, questionIdList: [1], TKTIdList: [2],
  });
  const response = {};
  expect(gen.next(response).value).toEqual(call(downloadWord, response, '试卷导出.docx'));
  expect(message.success).not.toHaveBeenCalled();
  expect(gen.next().done).toBe(true);
  expect(message.success).toHaveBeenCalled();
});

test.each([false, true])('history export forwards flag %s and reports download errors', flag => {
  const payload = { test_paper_uid: 'paper-1', withAnalysis: flag };
  const gen = historyModel.effects.reExportTestPaper({ payload }, { call });
  expect(gen.next().value).toEqual(call(requests.reExportTestPaper, payload));
  expect(gen.next({}).value.fn).toBe(downloadWord);
  expect(gen.throw(new Error('题目不存在')).done).toBe(true);
  expect(message.error).toHaveBeenCalledWith('题目不存在');
  expect(message.success).not.toHaveBeenCalled();
});
