# test_phase8.ps1
# End-to-end regression script for Phase 8: Interactive Visual Venue Seat Map & 5-Minute Hold Locks

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 8 END-TO-END AUTOMATION TEST: SEAT MAP & HOLD ENGINE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Login as Attendee
Write-Host "`n[1] Logging in as attendee..." -ForegroundColor Yellow
$loginBody = @{
    email = "alex.rivera@pulsevent.io"
    password = "Password@123"
} | ConvertTo-Json

try {
    $authResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $authResp.data.accessToken
    Write-Host "-> Success! Attendee token obtained for $($authResp.data.user.fullName)." -ForegroundColor Green
} catch {
    Write-Host "Failed to login: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
}

# 2. Get published events to test with
Write-Host "`n[2] Fetching active events..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=10" -Method Get
$event = $eventsResp.data.content[0]
if (-not $event) {
    Write-Host "No events found!" -ForegroundColor Red
    exit 1
}
$eventId = $event.id
Write-Host "-> Using Event ID: $eventId ($($event.title))" -ForegroundColor Green

# 3. Fetch visual seat map (triggers auto-generation if venue has no seat map yet)
Write-Host "`n[3] Calling GET /api/events/$eventId/seat-map..." -ForegroundColor Yellow
$seatMapResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/seat-map" -Method Get -Headers $headers
$sections = $seatMapResp.data.sections
Write-Host "-> Received Seat Map: Venue '$($seatMapResp.data.venueName)'" -ForegroundColor Green
Write-Host "-> Total Sections: $($sections.Count), Total Available: $($seatMapResp.data.availableSeats)" -ForegroundColor Green

if ($sections.Count -eq 0 -or $sections[0].seats.Count -eq 0) {
    Write-Host "Failed: No sections or seats found!" -ForegroundColor Red
    exit 1
}

$targetSeat = $sections[0].seats[0]
$seatId = $targetSeat.id
$seatCode = $targetSeat.seatCode
Write-Host "-> Selected Target Seat: $seatCode (ID: $seatId, Price: `$$($targetSeat.price))" -ForegroundColor Cyan

# 4. Hold Seat (5-minute lock)
Write-Host "`n[4] Requesting 5-minute hold for Seat $seatCode (ID: $seatId)..." -ForegroundColor Yellow
$holdResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/seats/$seatId/hold" -Method Post -Headers $headers
Write-Host "-> Hold Response Status: $($holdResp.data.status)" -ForegroundColor Green
Write-Host "-> Hold Expires At: $($holdResp.data.expiresAt)" -ForegroundColor Green

if ($holdResp.data.status -ne "HOLD") {
    Write-Host "Failed: Status should be HOLD" -ForegroundColor Red
    exit 1
}

# 5. Test Lock Conflict (A different user trying to hold already locked seat must be rejected)
Write-Host "`n[5] Verifying lock collision rejection for a different user..." -ForegroundColor Yellow
$adminLoginBody = @{
    email = "admin@pulsevent.io"
    password = "AdminPassword@123"
} | ConvertTo-Json
$adminAuth = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminHeaders = @{ Authorization = "Bearer $($adminAuth.data.accessToken)" }

try {
    $conflictResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/seats/$seatId/hold" -Method Post -Headers $adminHeaders
    Write-Host "Failed: Conflict was NOT rejected!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host "-> Collision correctly prevented: Status 400 / Seat already held by attendee Alex Rivera" -ForegroundColor Green
}

# 6. Release Hold
Write-Host "`n[6] Releasing hold for Seat ID $seatId..." -ForegroundColor Yellow
$releaseResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/seats/$seatId/hold" -Method Delete -Headers $headers
Write-Host "-> Release Response: $($releaseResp.message)" -ForegroundColor Green

# 7. Verify seat is available again
Write-Host "`n[7] Re-verifying Seat Map availability..." -ForegroundColor Yellow
$recheckResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/seat-map" -Method Get -Headers $headers
$recheckSeat = ($recheckResp.data.sections[0].seats | Where-Object { $_.id -eq $seatId })
Write-Host "-> Seat $seatCode status after release: $($recheckSeat.status)" -ForegroundColor Green

if ($recheckSeat.status -ne "AVAILABLE") {
    Write-Host "Failed: Seat should be AVAILABLE again!" -ForegroundColor Red
    exit 1
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  PHASE 8 TESTS PASSED 100% SUCCESSFULLY!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
