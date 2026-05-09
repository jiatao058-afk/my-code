@echo off
cd /d "%~dp0"
echo Starting Web Translator...
echo.
start /b uvicorn server:app --host 0.0.0.0 --port 8765 --reload
timeout /t 2 /nobreak >nul
start "" http://localhost:8765
echo Press Ctrl+C to stop
echo.
pause
