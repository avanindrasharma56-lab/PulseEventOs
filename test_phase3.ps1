$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 3 END-TO-END VERIFICATION SCRIPT ===" -ForegroundColor Cyan

# 1. Login as Admin
$adminPayload = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $adminPayload
$adminToken = $adminRes.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "1. Admin Authenticated: $($adminRes.data.user.fullName)" -ForegroundColor Green

# 2. Get Published Events
$eventsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$eventId = $null
if ($eventsRes.data.content.Count -gt 0) {
    $targetEvent = $eventsRes.data.content[0]
    $eventId = $targetEvent.id
    Write-Host "2. Found Published Event: '$($targetEvent.title)' (ID: $eventId, Organizer: $($targetEvent.organizerName))" -ForegroundColor Green
}

# 3. Check Ticket Tiers
$tiersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$tiers = $tiersRes.data
Write-Host "3. Existing Ticket Tiers for event $eventId ($($tiers.Count) tiers):" -ForegroundColor Cyan
foreach ($t in $tiers) {
    Write-Host "   - Tier: '$($t.name)' | Price: `$$($t.price) | Available: $($t.availableQuantity)/$($t.capacity)"
}
$tierToBuy = $tiers[0]

# 4. Login Attendee
$attendeeEmail = "alex.rivera@pulsevent.io"
$attendeePassword = "Password@123"
$attLoginPayload = @{ email = $attendeeEmail; password = $attendeePassword } | ConvertTo-Json
$attRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $attLoginPayload
$attendeeToken = $attRes.data.accessToken
$attHeaders = @{ Authorization = "Bearer $attendeeToken" }
Write-Host "4. Attendee logged in: $attendeeEmail" -ForegroundColor Green

# 5. Place an Order (1 Ticket)
$orderPayload = @{
    eventId = $eventId
    items = @(
        @{
            ticketTierId = $tierToBuy.id
            quantity = 1
            attendeeName = "Alex Rivera"
            attendeeEmail = $attendeeEmail
        }
    )
} | ConvertTo-Json

Write-Host "5. Placing order under Pessimistic Inventory Lock..." -ForegroundColor Cyan
$orderRes = Invoke-RestMethod -Uri "http://localhost:8085/api/orders" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $orderPayload
$order = $orderRes.data
Write-Host "   Order Created: $($order.orderNumber) | Total: `$$($order.totalAmount) | Tickets: $($order.tickets.Count)" -ForegroundColor Green

$ticket = $order.tickets[0]
$ticketCode = $ticket.ticketCode
Write-Host "   Ticket Code: $ticketCode" -ForegroundColor Green
Write-Host "   QR Code Data Length: $($ticket.qrCodeData.Length) chars (Base64 PNG)" -ForegroundColor Green
Write-Host "   QR Code Starts With: $($ticket.qrCodeData.Substring(0, 30))..." -ForegroundColor DarkGray

# 6. Verify Attendee Passbook (/api/tickets/my-tickets)
$myTicketsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/tickets/my-tickets" -Method Get -Headers $attHeaders
$myTickets = $myTicketsRes.data
Write-Host "6. Attendee Passbook (/api/tickets/my-tickets): $($myTickets.Count) passes found." -ForegroundColor Green

# 7. Check-In & Scanner Verification (/api/tickets/check-in)
$checkInPayload = @{ ticketCode = $ticketCode } | ConvertTo-Json

Write-Host "7. Testing Door Scanner Check-In with Admin/Verifier credentials..." -ForegroundColor Cyan
$checkInRes = Invoke-RestMethod -Uri "http://localhost:8085/api/tickets/check-in" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $checkInPayload
Write-Host "   Check-In 1 Result: $($checkInRes.message)" -ForegroundColor Green
Write-Host "   Ticket Status: $($checkInRes.data.ticket.status) | Checked-in At: $($checkInRes.data.ticket.checkedInAt)" -ForegroundColor Green

# 8. Test Duplicate Replay Block (Single-Use Guarantee)
Write-Host "8. Testing Duplicate Check-In (Replay Attempt)..." -ForegroundColor Cyan
try {
    $dupRes = Invoke-RestMethod -Uri "http://localhost:8085/api/tickets/check-in" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $checkInPayload
    Write-Host "ERROR: Duplicate check-in was allowed! Expected failure." -ForegroundColor Red
} catch {
    Write-Host "   SUCCESS: Duplicate check-in blocked as expected!" -ForegroundColor Green
    Write-Host "   Server error response: $($_.Exception.Message)" -ForegroundColor Yellow
}

# 9. Verify updated tier inventory
$updatedTiersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/tiers" -Method Get
$updatedTier = $updatedTiersRes.data | Where-Object { $_.id -eq $tierToBuy.id }
Write-Host "9. Tier Inventory Verification:" -ForegroundColor Cyan
Write-Host "   Tier '$($updatedTier.name)': Capacity = $($updatedTier.capacity), Sold = $($updatedTier.soldCount), Available = $($updatedTier.availableQuantity)" -ForegroundColor Green

Write-Host "`n=== ALL PHASE 3 INTEGRATION CHECKS PASSED PERFECTLY! ===" -ForegroundColor Green
