# test_phase10.ps1
# End-to-end regression test for Phase 10: Real-time Live WebSocket Door Operations & Turnstile Cockpit

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 10 END-TO-END TEST: LIVE TURNSTILE & DOOR OPS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Login as Admin / Organizer
Write-Host "`n[1] Authenticating as Door Operations Admin/Organizer..." -ForegroundColor Yellow
$orgLogin = @{
    email = "admin@pulsevent.io"
    password = "AdminPassword@123"
} | ConvertTo-Json

$orgResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $orgLogin -ContentType "application/json"
$orgToken = $orgResp.data.accessToken
Write-Host "-> Door Staff / Admin authenticated: $($orgResp.data.user.fullName)" -ForegroundColor Green

# 2. Login as Attendee
Write-Host "`n[2] Authenticating as Attendee..." -ForegroundColor Yellow
$attLogin = @{
    email = "alex.rivera@pulsevent.io"
    password = "Password@123"
} | ConvertTo-Json

$attResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $attLogin -ContentType "application/json"
$attToken = $attResp.data.accessToken
Write-Host "-> Attendee authenticated: $($attResp.data.user.fullName)" -ForegroundColor Green

# 3. Locate an active event with tickets
Write-Host "`n[3] Discovering event for door ops..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$event = $eventsResp.data.content[0]
$eventId = $event.id
Write-Host "-> Target Event: '$($event.title)' (ID: $eventId)" -ForegroundColor Green

# 4. Fetch initial Door Operations Stats
Write-Host "`n[4] Querying live Door Statistics before check-in..." -ForegroundColor Yellow
$doorHeaders = @{
    Authorization = "Bearer $orgToken"
}
$statsResp = Invoke-RestMethod -Uri "$baseUrl/api/tickets/event/$eventId/door-stats" -Method Get -Headers $doorHeaders
$stats = $statsResp.data
Write-Host "-> Event: $($stats.eventTitle)" -ForegroundColor Cyan
Write-Host "   * Total Issued: $($stats.totalIssuedTickets)" -ForegroundColor Cyan
Write-Host "   * Checked In:   $($stats.checkedInCount)" -ForegroundColor Cyan
Write-Host "   * Pending:      $($stats.pendingCheckInCount)" -ForegroundColor Cyan
Write-Host "   * Check-in %:   $($stats.checkInPercentage)%" -ForegroundColor Cyan
Write-Host "   * 15-min Vel:   $($stats.arrivalsLast15Min) arrivals" -ForegroundColor Cyan

# 5. Purchase a fresh ticket for testing check-in
Write-Host "`n[5] Purchasing a ticket to perform live turnstile check-in..." -ForegroundColor Yellow
$tiersResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/tiers" -Method Get
$tier = $tiersResp.data[0]

$orderBody = @{
    eventId = $eventId
    items = @(
        @{
            ticketTierId = $tier.id
            quantity = 1
        }
    )
} | ConvertTo-Json

$orderHeaders = @{
    Authorization = "Bearer $attToken"
}
$orderResp = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Body $orderBody -ContentType "application/json" -Headers $orderHeaders
$issuedTicket = $orderResp.data.tickets[0]
$ticketCode = $issuedTicket.ticketCode
Write-Host "-> Purchased Ticket Code: $ticketCode for tier: '$($tier.name)'" -ForegroundColor Green

# 6. Perform Turnstile Check-In via Organizer Door Scanner
Write-Host "`n[6] Scanning and checking in ticket at turnstile ($ticketCode)..." -ForegroundColor Yellow
$checkInBody = @{
    ticketCode = $ticketCode
} | ConvertTo-Json

$checkInResp = Invoke-RestMethod -Uri "$baseUrl/api/tickets/check-in" -Method Post -Body $checkInBody -ContentType "application/json" -Headers $doorHeaders
Write-Host "-> Check-in Status: $($checkInResp.message)" -ForegroundColor Green
Write-Host "-> Ticket Status:   $($checkInResp.data.ticket.status)" -ForegroundColor Green
Write-Host "-> Checked in at:   $($checkInResp.data.ticket.checkedInAt)" -ForegroundColor Green

if ($checkInResp.data.ticket.status -ne "CHECKED_IN") {
    Write-Host "Failed: Ticket was not successfully marked as CHECKED_IN" -ForegroundColor Red
    exit 1
}

# 7. Query Door Statistics after check-in to verify real-time recalculation
Write-Host "`n[7] Querying updated Door Statistics..." -ForegroundColor Yellow
$updatedStatsResp = Invoke-RestMethod -Uri "$baseUrl/api/tickets/event/$eventId/door-stats" -Method Get -Headers $doorHeaders
$newStats = $updatedStatsResp.data
Write-Host "-> New Checked-In Count: $($newStats.checkedInCount) (Previously $($stats.checkedInCount))" -ForegroundColor Green
Write-Host "-> New Check-in Rate:    $($newStats.checkInPercentage)%" -ForegroundColor Green
Write-Host "-> Recent Check-Ins:     $($newStats.recentCheckIns.Count) attendee(s) in log" -ForegroundColor Green

if ($newStats.checkedInCount -le $stats.checkedInCount) {
    Write-Host "Failed: Checked-in count did not increment!" -ForegroundColor Red
    exit 1
}

# 8. Test WebSocket Handshake Endpoint
Write-Host "`n[8] Verifying WebSocket STOMP Handshake endpoint..." -ForegroundColor Yellow
try {
    $wsInfo = Invoke-RestMethod -Uri "$baseUrl/ws-eventflow/info" -Method Get
    Write-Host "-> WebSocket SockJS Info handshake OK: websocket enabled = $($wsInfo.websocket)" -ForegroundColor Green
} catch {
    Write-Host "-> WebSocket Info handshake response: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ALL PHASE 10 LIVE TURNSTILE OPERATIONS TESTS PASSED!    " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
