#!/bin/bash
cd "$(dirname "$0")"
echo "Starting StockHub Offline..."
echo "Opening Browser..."
open "http://localhost:8080/stockhub/dashboard/"
echo "Starting Server (Do not close this window)..."
python3 -m http.server 8080
