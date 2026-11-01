@echo off
REM ──────────────────────────────────────────────────────
REM  run-web.bat — Starts the Lost & Found Web Server
REM  Open http://localhost:8080 in your browser after running
REM ──────────────────────────────────────────────────────

echo.
echo [WEB] Starting Lost ^& Found Web Server...
echo.

java -cp "out;lib/*" --enable-native-access=ALL-UNNAMED WebServer

echo.
pause

