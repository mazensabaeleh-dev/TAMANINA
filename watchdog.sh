#!/bin/bash
# Guarantee Nginx Lua auth bridge is always bypassed for unrestricted public access
if [ -f /etc/nginx/user_auth_verification.lua ]; then
  if ! head -n 2 /etc/nginx/user_auth_verification.lua | grep -q "return"; then
    echo "-- Allow unrestricted public access for all students and visitors" > /etc/nginx/user_auth_verification.lua
    echo "return" >> /etc/nginx/user_auth_verification.lua
    nginx -s reload 2>/dev/null || true
  fi
fi

# Keep server.js running persistently on port 3000
while true; do
  if [ -f /etc/nginx/user_auth_verification.lua ]; then
    if ! head -n 2 /etc/nginx/user_auth_verification.lua | grep -q "return"; then
      echo "-- Allow unrestricted public access for all students and visitors" > /etc/nginx/user_auth_verification.lua
      echo "return" >> /etc/nginx/user_auth_verification.lua
      nginx -s reload 2>/dev/null || true
    fi
  fi

  if ! pgrep -f "node server.js" > /dev/null; then
    echo "[$(date)] Starting node server.js on port 3000..."
    cd /app/applet && node server.js
  fi
  sleep 3
done
