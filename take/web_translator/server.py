import json
import re
from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import httpx

app = FastAPI(title="Web Translator")

OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL = "qwen2.5:7b"
TIMEOUT = httpx.Timeout(120.0, connect=10.0)


class TranslateRequest(BaseModel):
    text: str
    source_lang: str = "auto"
    target_lang: str = "Chinese"


class FetchRequest(BaseModel):
    url: str


def build_translate_prompt(text: str, source_lang: str, target_lang: str) -> str:
    if source_lang == "auto":
        return (
            f"Translate the following text into {target_lang}. "
            f"Output ONLY the translation, nothing else — no explanations, no notes, no quotation marks.\n\n"
            f"{text}"
        )
    else:
        return (
            f"Translate the following text from {source_lang} to {target_lang}. "
            f"Output ONLY the translation, nothing else — no explanations, no notes, no quotation marks.\n\n"
            f"{text}"
        )


async def call_ollama(prompt: str) -> str:
    payload = {
        "model": MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {"temperature": 0.1, "num_predict": 4096},
    }
    async with httpx.AsyncClient(timeout=TIMEOUT) as client:
        resp = await client.post(OLLAMA_URL, json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["response"].strip()


@app.post("/api/translate")
async def translate(req: TranslateRequest):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="Text is empty")
    prompt = build_translate_prompt(req.text, req.source_lang, req.target_lang)
    try:
        result = await call_ollama(prompt)
        return {"translated_text": result}
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Ollama error: {e}")


@app.post("/api/fetch-page")
async def fetch_page(req: FetchRequest):
    from bs4 import BeautifulSoup

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                      "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    async with httpx.AsyncClient(timeout=httpx.Timeout(30.0), follow_redirects=True) as client:
        try:
            resp = await client.get(req.url, headers=headers)
            resp.raise_for_status()
        except httpx.HTTPError as e:
            raise HTTPException(status_code=502, detail=f"Failed to fetch URL: {e}")

    soup = BeautifulSoup(resp.text, "html.parser")

    for tag in soup(["script", "style", "nav", "footer", "header", "aside", "noscript"]):
        tag.decompose()

    title = soup.title.string.strip() if soup.title and soup.title.string else ""

    content_tags = soup.find_all(["p", "h1", "h2", "h3", "h4", "h5", "h6", "li", "td", "th"])
    paragraphs = []
    for tag in content_tags:
        text = tag.get_text(strip=True)
        if text and len(text) > 2:
            paragraphs.append({
                "tag": tag.name,
                "text": text,
            })

    return {"title": title, "paragraphs": paragraphs, "url": req.url}


@app.get("/api/health")
async def health():
    return {"status": "ok"}


app.mount("/", StaticFiles(directory="static", html=True), name="static")
