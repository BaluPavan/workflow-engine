#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "Registering order-fulfillment workflow..."
curl -s -X POST "$BASE_URL/api/workflows" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "order-fulfillment",
    "description": "Validate, charge, reserve, ship, and notify",
    "steps": [
      {"stepName": "validate_order", "stepOrder": 1, "critical": true, "maxRetries": 3, "timeoutSeconds": 30},
      {"stepName": "charge_payment", "stepOrder": 2, "critical": true, "maxRetries": 3, "timeoutSeconds": 30},
      {"stepName": "reserve_inventory", "stepOrder": 3, "critical": false, "maxRetries": 1, "timeoutSeconds": 30},
      {"stepName": "ship_order", "stepOrder": 4, "critical": true, "maxRetries": 3, "timeoutSeconds": 30},
      {"stepName": "send_notification", "stepOrder": 5, "critical": false, "maxRetries": 1, "timeoutSeconds": 30}
    ]
  }' | tee /tmp/workflow-register.json
echo ""

poll_instance() {
  local id="$1"
  local label="$2"
  for _ in $(seq 1 40); do
    local resp status
    resp=$(curl -s "$BASE_URL/api/instances/$id")
    status=$(echo "$resp" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])")
    echo "[$label] status=$status"
    if [ "$status" = "COMPLETED" ] || [ "$status" = "FAILED" ]; then
      echo "$resp" | python3 -m json.tool
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for $label"
  return 1
}

echo "Starting happy-path instance..."
HAPPY=$(curl -s -X POST "$BASE_URL/api/workflows/order-fulfillment/instances" \
  -H 'Content-Type: application/json' \
  -d '{"context":"{\"orderId\":\"ORD-123\",\"amount\":99.99}"}')
HAPPY_ID=$(echo "$HAPPY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
poll_instance "$HAPPY_ID" "happy-path"

echo ""
echo "Starting non-critical skip instance..."
SKIP=$(curl -s -X POST "$BASE_URL/api/workflows/order-fulfillment/instances" \
  -H 'Content-Type: application/json' \
  -d '{"context":"{\"orderId\":\"ORD-456\",\"simulateFailure\":\"reserve_inventory\"}"}')
SKIP_ID=$(echo "$SKIP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
poll_instance "$SKIP_ID" "non-critical-skip"

echo ""
echo "Starting retry demo instance (first charge attempt fails, retry succeeds after ~30s)..."
RETRY=$(curl -s -X POST "$BASE_URL/api/workflows/order-fulfillment/instances" \
  -H 'Content-Type: application/json' \
  -d '{"context":"{\"orderId\":\"ORD-789\",\"simulateFailure\":\"charge_payment\"}"}')
RETRY_ID=$(echo "$RETRY" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
poll_instance "$RETRY_ID" "retry-demo"

echo ""
echo "Health check:"
curl -s "$BASE_URL/actuator/health"
echo ""
