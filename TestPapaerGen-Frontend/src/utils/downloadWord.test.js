import { downloadWord } from './downloadWord';

function response(type, overrides = {}) {
  return {
    ok: true,
    status: 200,
    headers: { get: () => type },
    blob: jest.fn().mockResolvedValue(new Blob(['word'])),
    text: jest.fn().mockResolvedValue('{"code":500,"msg":"题目不存在"}'),
    ...overrides,
  };
}

beforeEach(() => {
  jest.useFakeTimers();
  URL.createObjectURL = jest.fn(() => 'blob:test');
  URL.revokeObjectURL = jest.fn();
  jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
  jest.restoreAllMocks();
});

test('downloads Word and releases the temporary URL', async () => {
  await downloadWord(response('application/octet-stream'), '试卷.docx');
  expect(HTMLAnchorElement.prototype.click).toHaveBeenCalledTimes(1);
  expect(document.querySelector('a')).toBeNull();
  jest.runOnlyPendingTimers();
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test');
});

test.each(['text/plain;charset=UTF-8', 'application/json'])('does not download %s errors', async type => {
  const res = response(type);
  await expect(downloadWord(res, '试卷.docx')).rejects.toThrow('题目不存在');
  expect(res.blob).not.toHaveBeenCalled();
  expect(URL.createObjectURL).not.toHaveBeenCalled();
});

test('reports expired sessions', async () => {
  await expect(downloadWord(response('text/html', { ok: false, status: 403 }), '试卷.docx'))
    .rejects.toThrow('登录已过期');
});

test('rejects empty files and HTML responses', async () => {
  await expect(downloadWord(response('application/octet-stream', {
    blob: jest.fn().mockResolvedValue(new Blob([])),
  }), '试卷.docx')).rejects.toThrow('导出文件为空');
  await expect(downloadWord(response('text/html', {
    text: jest.fn().mockResolvedValue('<html>error</html>'),
  }), '试卷.docx')).rejects.toThrow('试卷导出失败');
  expect(URL.createObjectURL).not.toHaveBeenCalled();
});
