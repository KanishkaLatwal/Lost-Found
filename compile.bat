@echo off
REM ──────────────────────────────────────────────────────
REM  compile.bat — Compiles all Java sources into out/
REM  Usage: double-click or run from Command Prompt
REM ──────────────────────────────────────────────────────

echo.
echo [COMPILE] Compiling Lost ^& Found Management System...
echo.

if not exist out mkdir out

javac -d out -cp "lib/*" src/*.java

if %ERRORLEVEL% == 0 (
    echo [OK] Compilation successful. Run 'run.bat' to start the app.
) else (
    echo [FAIL] Compilation failed. Check the errors above.
)

echo.
pause
