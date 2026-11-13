#!/usr/bin/env bash
# 사용: PGHOST=<소켓 폴더> PGPORT=<포트> bash supabase/tests/e2e/run.sh   (Playwright와 psql 필요)
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"; repo="$(cd "$here/../../.." && pwd)"
work="$(mktemp -d)"; trap 'kill $(jobs -p) 2>/dev/null || true; rm -rf "$work"' EXIT
mkdir -p "$work/app" "$work/local"
cp "$repo/index.html" "$repo/sync.js" "$here/fake-supabase.js" "$work/app/"
mkdir -p "$work/app/ios" && cp "$repo/ios/widget.js" "$work/app/ios/"
echo "window.SHIFTWORK_CONFIG = { supabaseUrl: 'http://fake.local', supabaseAnonKey: 'anon', supabaseModule: '/fake-supabase.js' }" > "$work/app/config.js"
cp "$repo/index.html" "$repo/sync.js" "$work/local/"
echo "window.SHIFTWORK_CONFIG = { supabaseUrl: '', supabaseAnonKey: '' }" > "$work/local/config.js"
psql -q -U postgres -d postgres -c "drop database if exists e2e" -c "create database e2e"
psql -q -U postgres -d e2e -v ON_ERROR_STOP=1 -f "$repo/supabase/tests/stub_supabase.sql" $(for f in "$repo"/supabase/migrations/*.sql; do printf -- "-f %s " "$f"; done)
node "$here/server.mjs" "$work/app" "${APP_PORT:-8765}" "$PGHOST" "${PGPORT:-5432}" e2e > "$work/app.log" 2>&1 &
node "$here/server.mjs" "$work/local" "${LOCAL_PORT:-8766}" "$PGHOST" "${PGPORT:-5432}" e2e > "$work/local.log" 2>&1 &
sleep 1
node "$here/test.cjs"
