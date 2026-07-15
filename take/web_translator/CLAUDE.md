# Web Translator

网页翻译应用，基于 FastAPI + Ollama（qwen2.5:7b 模型）。

## 启动方式

双击 `start.bat` 或运行：
```bash
uvicorn server:app --host 0.0.0.0 --port 8765 --reload
```
浏览器访问 http://localhost:8765

## 前置依赖

- Python 3.12+，依赖见 `requirements.txt`
- Ollama 运行在 `localhost:11434`，已拉取 `qwen2.5:7b` 模型

## 项目结构

```
server.py          # FastAPI 后端，/api/translate, /api/fetch-page, /api/health
static/index.html  # 前端页面，文本翻译 + 网页抓取翻译两个 Tab
start.bat          # 启动脚本（后台启动服务 + 自动打开浏览器）
requirements.txt   # Python 依赖
```

## 注意事项

- 翻译走 Ollama 本地模型，模型响应较慢时前端的 HTTP 超时是 120s
- 网页抓取会过滤掉 script/style/nav/footer/header/aside 标签，只保留文字段落
