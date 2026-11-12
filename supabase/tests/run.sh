#!/usr/bin/env bash
# 로컬 PostgreSQL에서 권한 시험을 돌린다. 사용: PGHOST=<소켓 폴더> PGPORT=<포트> PGUSER=postgres bash supabase/tests/run.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
db=shiftwork_rls_test
psql -q -d postgres -c "drop database if exists $db" -c "create database $db"
psql -q -d $db -v ON_ERROR_STOP=1 -f supabase/tests/stub_supabase.sql $(for f in supabase/migrations/*.sql; do printf -- "-f %s " "$f"; done)
psql -q -d $db -f supabase/tests/rls_test.sql
psql -q -d $db -f supabase/tests/share_summary_test.sql
