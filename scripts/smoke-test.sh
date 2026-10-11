#!/usr/bin/env bash
# End-to-end smoke test against a RUNNING backend (Docker or local).
# Uploads a 2.5 MiB file, verifies it, downloads it, simulates a node failure, downloads again
# (replica fallback), restores the node, and deletes the file. Exits non-zero on the first failure.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

pass() { echo "  [PASS] $1"; }
fail() { echo "  [FAIL] $1" >&2; exit 1; }

echo "Smoke test against $BASE_URL"

# 1. Backend reachable
curl -sf "$BASE_URL/api/dashboard/stats" > /dev/null || fail "backend is not reachable at $BASE_URL"
pass "backend is reachable"

# 2. Create a 2.5 MiB random sample file (3 chunks at 1 MiB)
SAMPLE="$WORK_DIR/sample-2.5mb.bin"
head -c 2621440 /dev/urandom > "$SAMPLE"
pass "generated sample file ($(stat -c%s "$SAMPLE") bytes)"

# 3. Upload
UPLOAD_JSON=$(curl -sf -F "file=@$SAMPLE" "$BASE_URL/api/files/upload")
FILE_ID=$(echo "$UPLOAD_JSON" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
[ -n "$FILE_ID" ] || fail "upload did not return a file id: $UPLOAD_JSON"
echo "$UPLOAD_JSON" | grep -q '"chunkCount":3' || fail "expected 3 chunks for 2.5 MiB"
pass "uploaded as file id $FILE_ID with 3 chunks"

# 4. Verify integrity
VERIFY_JSON=$(curl -sf -X POST "$BASE_URL/api/files/$FILE_ID/verify")
echo "$VERIFY_JSON" | grep -q '"status":"VERIFIED"' || fail "integrity check did not return VERIFIED"
pass "integrity check returned VERIFIED"

# 5. Download and compare bytes
DOWNLOAD="$WORK_DIR/downloaded.bin"
curl -sf -o "$DOWNLOAD" "$BASE_URL/api/files/$FILE_ID/download"
cmp -s "$SAMPLE" "$DOWNLOAD" || fail "downloaded file differs from the original"
pass "download matches original byte-for-byte"

# 6. Simulate failure of node1 and download again (replica fallback)
NODES_JSON=$(curl -sf "$BASE_URL/api/nodes")
NODE1_ID=$(echo "$NODES_JSON" | grep -o '"id":[0-9]*,"name":"node1"' | grep -o '"id":[0-9]*' | cut -d: -f2 || true)
[ -n "$NODE1_ID" ] || fail "could not find node1"
OFFLINE_JSON=$(curl -sf -X POST "$BASE_URL/api/nodes/$NODE1_ID/offline")
echo "$OFFLINE_JSON" | grep -q '"status":"OFFLINE"' || fail "node1 did not go offline"
pass "node1 simulated offline"

DOWNLOAD_FALLBACK="$WORK_DIR/downloaded-fallback.bin"
curl -sf -o "$DOWNLOAD_FALLBACK" "$BASE_URL/api/files/$FILE_ID/download"
cmp -s "$SAMPLE" "$DOWNLOAD_FALLBACK" || fail "download during node failure differs from the original"
pass "download succeeded from replicas while node1 was offline"

# 7. Restore node1
ONLINE_JSON=$(curl -sf -X POST "$BASE_URL/api/nodes/$NODE1_ID/online")
echo "$ONLINE_JSON" | grep -q '"status":"ONLINE"' || fail "node1 did not come back online"
pass "node1 restored"

# 8. Delete
DELETE_JSON=$(curl -sf -X DELETE "$BASE_URL/api/files/$FILE_ID")
echo "$DELETE_JSON" | grep -q '"deleted":true' || fail "delete did not complete: $DELETE_JSON"
STATUS_AFTER=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/files/$FILE_ID")
[ "$STATUS_AFTER" = "404" ] || fail "file still exists after delete (HTTP $STATUS_AFTER)"
pass "file deleted and metadata removed"

echo "All smoke-test checks passed."
