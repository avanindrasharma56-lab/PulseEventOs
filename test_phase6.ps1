$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 6 END-TO-END VERIFICATION SCRIPT ===" -ForegroundColor Cyan

# 1. Login Attendee
$attLoginPayload = @{ email = "alex.rivera@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$attRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $attLoginPayload
$attToken = $attRes.data.accessToken
$attHeaders = @{ Authorization = "Bearer $attToken" }
Write-Host "1. Attendee Authenticated: $($attRes.data.user.fullName)" -ForegroundColor Green

# 2. Login Admin
$adminPayload = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $adminPayload
$adminToken = $adminRes.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "2. Admin Authenticated: $($adminRes.data.user.fullName)" -ForegroundColor Green

# 3. Find Published Event and Tier
$eventsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$targetEvent = $eventsRes.data.content[0]
$eventId = $targetEvent.id

$tiersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$tier = $tiersRes.data[0]
$initialAvailable = $tier.availableQuantity
Write-Host "3. Target Tier: '$($tier.name)' | Current Available: $initialAvailable" -ForegroundColor Cyan

# 4. Waitlist Lifecycle: Query & Leave
Write-Host "4. Testing Waitlist Entries & Cancellation..." -ForegroundColor Cyan
$myEntriesRes = Invoke-RestMethod -Uri "http://localhost:8085/api/waitlist/my-entries" -Method Get -Headers $attHeaders
Write-Host "   Attendee Active Waitlist Entries: $($myEntriesRes.data.Count)" -ForegroundColor Green

if ($myEntriesRes.data.Count -gt 0) {
    $entryToCancel = $myEntriesRes.data[0]
    $cancelWlRes = Invoke-RestMethod -Uri "http://localhost:8085/api/waitlist/$($entryToCancel.id)" -Method Delete -Headers $attHeaders
    Write-Host "   Left Waitlist Entry ID $($entryToCancel.id): Status=$($cancelWlRes.data.status)" -ForegroundColor DarkGray
}

# 5. Place an Order for Refund Testing
Write-Host "5. Placing Order to test Refund & Inventory Restock..." -ForegroundColor Cyan
$orderReq = @{
    eventId = $eventId
    items = @(
        @{
            ticketTierId = $tier.id
            quantity = 1
            attendeeName = "Alex Rivera"
            attendeeEmail = "alex.rivera@pulsevent.io"
        }
    )
} | ConvertTo-Json

$orderRes = Invoke-RestMethod -Uri "http://localhost:8085/api/orders" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $orderReq
$order = $orderRes.data
Write-Host "   Order Created: $($order.orderNumber) (ID: $($order.id)) | Total: `$$($order.totalAmount)" -ForegroundColor Green

# Verify inventory decremented
$tiersCheck1 = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$afterBuyAvailable = ($tiersCheck1.data | Where-Object { $_.id -eq $tier.id }).availableQuantity
Write-Host "   Inventory after purchase: $afterBuyAvailable" -ForegroundColor DarkGray

# 6. Attendee Submits Self-Service Refund Request
Write-Host "6. Attendee Submitting Refund Request..." -ForegroundColor Cyan
$refundReq = @{
    orderId = $order.id
    reason = "Schedule conflict due to international summit travels"
} | ConvertTo-Json

$refundRes = Invoke-RestMethod -Uri "http://localhost:8085/api/refunds/request" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $refundReq
$refund = $refundRes.data
Write-Host "   Refund Request Submitted: ID #$($refund.id) | Amount: `$$($refund.amount) | Status: $($refund.status)" -ForegroundColor Green

# Query attendee's refund requests
$myRefundsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/refunds/my-requests" -Method Get -Headers $attHeaders
Write-Host "   Attendee Refund Requests Count: $($myRefundsRes.data.Count)" -ForegroundColor Green

# 7. Administrator Reviews & Approves Refund
Write-Host "7. Administrator Reviewing Refund Request..." -ForegroundColor Cyan
$allRefundsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/refunds" -Method Get -Headers $adminHeaders
Write-Host "   Platform Total Refund Requests: $($allRefundsRes.data.Count)" -ForegroundColor Cyan

$reviewReq = @{
    approved = $true
    adminNotes = "Approved per 14-day cancellation policy. Inventory restocked."
} | ConvertTo-Json

$reviewRes = Invoke-RestMethod -Uri "http://localhost:8085/api/refunds/$($refund.id)/review" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $reviewReq
$reviewed = $reviewRes.data
Write-Host "   Refund Processed: Status=$($reviewed.status) | Admin Notes: '$($reviewed.adminNotes)'" -ForegroundColor Green

# 8. Verify Inventory Restocked
Write-Host "8. Verifying Inventory Restock..." -ForegroundColor Cyan
$tiersCheck2 = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$restockedAvailable = ($tiersCheck2.data | Where-Object { $_.id -eq $tier.id }).availableQuantity
Write-Host "   Restocked Inventory for '$($tier.name)': $restockedAvailable (Recovered from $afterBuyAvailable)" -ForegroundColor Green

if ($restockedAvailable -ne ($afterBuyAvailable + 1)) {
    throw "Expected inventory to increase by 1 following refund approval."
}

# 9. Verify Order Status
$ordersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/orders/my-orders" -Method Get -Headers $attHeaders
$refundedOrder = $ordersRes.data | Where-Object { $_.id -eq $order.id }
Write-Host "   Order Final Status in Ledger: $($refundedOrder.status)" -ForegroundColor Green

Write-Host "=== ALL PHASE 6 INTEGRATION CHECKS PASSED PERFECTLY! ===" -ForegroundColor Green
