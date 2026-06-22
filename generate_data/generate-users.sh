#!/usr/bin/env bash
set -euo pipefail

API_URL="${1:-http://localhost:8080}"
USERS="${2:-10}"

register() {
  local body="$1" username="$2" role="$3" code
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL/auth/register" \
    -H "Content-Type: application/json" -d "$body")
  case "$code" in
    200|201) echo "created: $username ($role)" ;;
    409)     echo "exists:  $username" ;;
    *)       echo "FAILED:  $username -> $code" ;;
  esac
}

register '{"username":"admin","password":"admin123","firstName":"Admin","lastName":"Admin","email":"admin@example.com","role":"admin"}' "admin" "admin"
register '{"username":"courier","password":"courier123","firstName":"Courier","lastName":"One","email":"courier@example.com","role":"courier"}' "courier" "courier"

for i in $(seq 1 "$USERS"); do
  register "{\"username\":\"user$i\",\"password\":\"password123\",\"firstName\":\"Test\",\"lastName\":\"User$i\",\"email\":\"user$i@example.com\",\"role\":\"user\"}" "user$i" "user"
done
