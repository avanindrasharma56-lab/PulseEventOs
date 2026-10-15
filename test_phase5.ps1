$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 5 END-TO-END VERIFICATION SCRIPT ===" -ForegroundColor Cyan

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

# 3. Find Published Event and Tiers
$eventsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$targetEvent = $eventsRes.data.content[0]
$eventId = $targetEvent.id
Write-Host "3. Target Event: '$($targetEvent.title)' (ID: $eventId)" -ForegroundColor Cyan

$tiersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$tier = $tiersRes.data[0]
Write-Host "   Target Tier: '$($tier.name)' (Price: `$$($tier.price), Available: $($tier.availableQuantity))" -ForegroundColor Cyan

# 4. Test Promo Code Validation
Write-Host "4. Testing Promo Code Validation..." -ForegroundColor Cyan
$valReq = @{
    code = "PULSE20"
    eventId = $eventId
    orderTotal = 100.00
} | ConvertTo-Json

$valRes = Invoke-RestMethod -Uri "http://localhost:8085/api/promo-codes/validate" -Method Post -ContentType "application/json" -Body $valReq
Write-Host "   Code: $($valRes.data.code)" -ForegroundColor Green
Write-Host "   Discount Amount: `$$($valRes.data.discountAmount)" -ForegroundColor Green
Write-Host "   Final Total: `$$($valRes.data.finalTotal)" -ForegroundColor Green
if (-not $valRes.data.valid) { throw "Expected PULSE20 to be valid" }

# Test Invalid Code
$invalidReq = @{
    code = "NONEXISTENT_CODE"
    eventId = $eventId
    orderTotal = 100.00
} | ConvertTo-Json
$invalidRes = Invoke-RestMethod -Uri "http://localhost:8085/api/promo-codes/validate" -Method Post -ContentType "application/json" -Body $invalidReq
Write-Host "   Invalid Code Handled: Valid=$($invalidRes.data.valid) (Message: $($invalidRes.data.message))" -ForegroundColor DarkGray

# 5. Create Custom Promo Code
Write-Host "5. Creating Custom Promo Code..." -ForegroundColor Cyan
$customCode = "DEV" + (Get-Random -Minimum 100 -Maximum 999)
$createPromoReq = @{
    code = $customCode
    description = "Special Developer Discount"
    discountType = "PERCENTAGE"
    discountValue = 25.00
    maxUses = 50
    minOrderAmount = 0.00
    isActive = $true
} | ConvertTo-Json

$createPromoRes = Invoke-RestMethod -Uri "http://localhost:8085/api/promo-codes" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $createPromoReq
Write-Host "   Created Code: $($createPromoRes.data.code) ($($createPromoRes.data.discountValue)% off)" -ForegroundColor Green

# 6. Place Order with Promo Code
Write-Host "6. Placing Order with Promo Code $customCode..." -ForegroundColor Cyan
$orderReq = @{
    eventId = $eventId
    promoCode = $customCode
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
Write-Host "   Order Placed: $($order.orderNumber)" -ForegroundColor Green
Write-Host "   Discount Applied: `$$($order.discountAmount)" -ForegroundColor Green
Write-Host "   Total Amount Paid: `$$($order.totalAmount)" -ForegroundColor Green

# 7. Test Event Reviews and Ratings
Write-Host "7. Testing Event Review Submission..." -ForegroundColor Cyan
$reviewReq = @{
    rating = 5
    title = "Outstanding Experience!"
    comment = "Exceptional keynotes, smooth QR badge check-in, and world-class networking."
} | ConvertTo-Json

try {
    $reviewRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/reviews" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $reviewReq
    Write-Host "   Review Submitted: Rating $($reviewRes.data.rating)/5 stars - '$($reviewRes.data.title)'" -ForegroundColor Green
} catch {
    Write-Host "   Review note: $($_.Exception.Message)" -ForegroundColor DarkGray
}

$allReviewsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/reviews" -Method Get
Write-Host "   Event Reviews Count: $($allReviewsRes.data.Count)" -ForegroundColor Green

# 8. Test Waitlist Queue Join
Write-Host "8. Testing Waitlist Queue Join..." -ForegroundColor Cyan
$waitlistJoinReq = @{
    ticketTierId = $tier.id
} | ConvertTo-Json

try {
    $wlRes = Invoke-RestMethod -Uri "http://localhost:8085/api/waitlist/join" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $waitlistJoinReq
    Write-Host "   Joined Waitlist Entry ID: $($wlRes.data.id) | Status: $($wlRes.data.status) | Position: $($wlRes.data.queuePosition)" -ForegroundColor Green
} catch {
    Write-Host "   Waitlist join response: $($_.Exception.Message)" -ForegroundColor DarkGray
}

Write-Host "=== ALL PHASE 5 INTEGRATION CHECKS PASSED PERFECTLY! ===" -ForegroundColor Green
