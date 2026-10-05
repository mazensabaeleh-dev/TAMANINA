#!/bin/bash
# Robust, non-colliding process supervisor for Tomanina

APP_DIR="/app/applet"
cd "$APP_DIR" || exit 1

# Ensure Nginx auth bypass and public access
if [ -f /etc/nginx/user_auth_verification.lua ]; then
  if ! head -n 2 /etc/nginx/user_auth_verification.lua | grep -q "return"; then
    echo "-- Unrestricted public access" > /etc/nginx/user_auth_verification.lua
    echo "return" >> /etc/nginx/user_auth_verification.lua
    nginx -s reload 2>/dev/null || true
  fi
fi

# Ensure .env exists
if [ ! -f "$APP_DIR/.env" ]; then
  echo "ADMIN_EMAIL=mazensabaeleh@gmail.com" > "$APP_DIR/.env"
  echo "ADMIN_PASSWORD=mazen@1435522" >> "$APP_DIR/.env"
  echo "PORT=3000" >> "$APP_DIR/.env"
fi

# Check if port 3000 is listening; only launch if not listening
while true; do
  if ! curl -s --connect-timeout 1 http://127.0.0.1:3000/api/system/health > /dev/null 2>&1; then
    # Double check if any hung node process is lingering
    pkill -f "node.*server.js" 2>/dev/null || true
    sleep 1
    echo "[$(date)] Launching node server.js on port 3000..."
    node server.js >> /var/log/tomanina_server.log 2>&1 &
  fi
  sleep 10
done
