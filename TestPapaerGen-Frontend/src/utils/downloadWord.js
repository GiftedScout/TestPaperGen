// Export endpoints return a Response, including JSON/text error responses.
export async function downloadWord(response, filename) {
  if (!response || typeof response.blob !== 'function') {
    throw new Error('试卷导出失败，请稍后重试');
  }
  const contentType = (response.headers.get('content-type') || '').toLowerCase();
  const isWord = contentType.includes('application/octet-stream') ||
    contentType.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document');
  if (!response.ok || !isWord) {
    let message = response.status === 403 ? '登录已过期，请重新登录' : '试卷导出失败，请稍后重试';
    if (response.ok) {
      try {
        const error = JSON.parse(await response.text());
        if (typeof error.msg === 'string' && error.msg) message = error.msg;
      } catch (_) {
        // Do not display an HTML error page or save it as a Word document.
      }
    }
    throw new Error(message);
  }
  const blob = await response.blob();
  if (!blob.size) throw new Error('导出文件为空，请稍后重试');
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  try {
    anchor.href = url;
    anchor.download = filename;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
  } finally {
    anchor.remove();
    // Give the browser time to start reading the download before releasing it.
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
