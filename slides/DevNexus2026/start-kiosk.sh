#!/usr/bin/env bash
chromium-browser --kiosk index.html || google-chrome --kiosk index.html || chromium --kiosk index.html
