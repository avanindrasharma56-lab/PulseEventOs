$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 4 LIVE ORDER & NOTIFICATION VERIFICATION ===" -ForegroundColor Cyan

# 1. Attendee (Alex Rivera) places an order
$attLoginPayload = @{ email = "alex.rivera@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$attRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $attLoginPayload
$attToken = $attRes.data.accessToken
$attHeaders = @{ Authorization = "Bearer $attToken" }

# Get published event and tier
$publishedEvents = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$event = $publishedEvents.data.content[0]
$tiers = (Invoke-RestMethod -Uri "http://localhost:8085/api/events/$($event.id)/tiers" -Method Get).data
$tier = $tiers[0]

$orderPayload = @{
    eventId = $event.id
    items = @(
        @{
            ticketTierId = $tier.id
            quantity = 1
            attendeeName = "Alex Rivera"
            attendeeEmail = "alex.rivera@pulsevent.io"
        }
    )
} | ConvertTo-Json

Write-Host "1. Placing order for '$($event.title)' tier '$($tier.name)'..." -ForegroundColor Yellow
$orderRes = Invoke-RestMethod -Uri "http://localhost:8085/api/orders" -Method Post -Headers $attHeaders -ContentType "application/json" -Body $orderPayload
$order = $orderRes.data
Write-Host "   Order Created: $($order.orderNumber) (Total: `$$($order.totalAmount))" -ForegroundColor Green

# 2. Check Notification Hub for Attendee
$notifsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications" -Method Get -Headers $attHeaders
$notifs = $notifsRes.data
Write-Host "2. Notification Hub for Alex Rivera ($($notifs.Count) notification(s)):" -ForegroundColor Cyan
foreach ($n in $notifs) {
    Write-Host "   - [$($n.type)] $($n.title) | Unread: $(-not $n.read) | Msg: $($n.message)" -ForegroundColor Green
}

$unreadRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/unread-count" -Method Get -Headers $attHeaders
Write-Host "   Unread Badge Count: $($unreadRes.data.unreadCount)" -ForegroundColor Green

# 3. Check-In the ticket and verify second notification
$ticket = $order.tickets[0]
$adminPayload = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $adminPayload
$adminHeaders = @{ Authorization = "Bearer $($adminRes.data.accessToken)" }

Write-Host "3. Scanning and checking in ticket '$($ticket.ticketCode)'..." -ForegroundColor Yellow
$checkInPayload = @{ ticketCode = $ticket.ticketCode } | ConvertTo-Json
$checkInRes = Invoke-RestMethod -Uri "http://localhost:8085/api/tickets/check-in" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $checkInPayload
Write-Host "   $($checkInRes.message)" -ForegroundColor Green

# 4. Check Updated Notification Hub
$updatedNotifsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications" -Method Get -Headers $attHeaders
$updatedNotifs = $updatedNotifsRes.data
Write-Host "4. Updated Notification Hub ($($updatedNotifs.Count) notification(s)):" -ForegroundColor Cyan
foreach ($n in $updatedNotifs) {
    Write-Host "   - [$($n.type)] $($n.title) | Msg: $($n.message)" -ForegroundColor Green
}

Write-Host "`n=== LIVE NOTIFICATION TRIGGER TESTS COMPLETED SUCCESSFULLY! ===" -ForegroundColor Green
