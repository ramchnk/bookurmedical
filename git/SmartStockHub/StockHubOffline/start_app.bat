@echo off
TITLE StockHub Offline Server
echo ====================================================
echo      Starting StockHub Offline Application
echo ====================================================
echo.
echo 1. Opening your default browser...
start "" "http://localhost:8080/stockhub/dashboard/"

echo 2. Starting local server...
echo.
echo    PLEASE DO NOT CLOSE THIS WINDOW while using the app.
echo    You can minimize it.
echo.
python -m http.server 8080

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Python is not installed or not found in PATH.
    echo Please install Python from python.org and try again.
    pause
)
