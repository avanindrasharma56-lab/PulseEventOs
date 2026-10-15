# test_phase11.ps1
# End-to-end regression test for Phase 11: Conference Attendee PDF Badge & Lanyard Pass Engine

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 11 END-TO-END TEST: ATTENDEE BADGES & LANYARD PASS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Login as Admin / Organizer
Write-Host "`n[1] Authenticating as Organizer..." -ForegroundColor Yellow
$orgLogin = @{
    email = "admin@pulsevent.io"
    password = "AdminPassword@123"
} | ConvertTo-Json

$orgResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $orgLogin -ContentType "application/json"
$orgToken = $orgResp.data.accessToken
Write-Host "-> Organizer authenticated: $($orgResp.data.user.fullName)" -ForegroundColor Green

# 2. Login as Attendee
Write-Host "`n[2] Authenticating as Attendee..." -ForegroundColor Yellow
$attLogin = @{
    email = "alex.rivera@pulsevent.io"
    password = "Password@123"
} | ConvertTo-Json

$attResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $attLogin -ContentType "application/json"
$attToken = $attResp.data.accessToken
Write-Host "-> Attendee authenticated: $($attResp.data.user.fullName)" -ForegroundColor Green

# 3. Locate target event & attendee tickets
Write-Host "`n[3] Discovering event and attendee pass..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$event = $eventsResp.data.content[0]
$eventId = $event.id

$myTickets = Invoke-RestMethod -Uri "$baseUrl/api/tickets/my-tickets" -Method Get -Headers @{ Authorization = "Bearer $attToken" }
$ticket = $myTickets.data[0]
$ticketCode = $ticket.ticketCode
Write-Host "-> Target Event: '$($event.title)' (ID: $eventId)" -ForegroundColor Green
Write-Host "-> Attendee Pass: $ticketCode for '$($ticket.attendeeName)' ($($ticket.tierName))" -ForegroundColor Green

# 4. Generate & Download Individual 4x6 Lanyard Badge PDF
Write-Host "`n[4] Downloading individual printable lanyard badge PDF ($ticketCode)..." -ForegroundColor Yellow
$badgeUrl = "$baseUrl/api/tickets/$ticketCode/badge.pdf"
$singleFile = "$env:TEMP/single_badge_test.pdf"
Invoke-WebRequest -Uri $badgeUrl -Method Get -Headers @{ Authorization = "Bearer $attToken" } -OutFile $singleFile -UseBasicParsing

$badgeBytes = [System.IO.File]::ReadAllBytes($singleFile)
Write-Host "-> Badge PDF received! Size: $($badgeBytes.Length) bytes" -ForegroundColor Green

# Verify PDF signature (%PDF-)
$magic = [System.Text.Encoding]::ASCII.GetString($badgeBytes[0..4])
Write-Host "-> PDF Header Magic: $magic" -ForegroundColor Cyan
if ($magic -ne "%PDF-") {
    Write-Host "Failed: File does not start with %PDF- header!" -ForegroundColor Red
    exit 1
}

# 5. Generate & Download Bulk Badges PDF for Organizer
Write-Host "`n[5] Downloading bulk kiosk badges PDF for event ID $eventId..." -ForegroundColor Yellow
$bulkUrl = "$baseUrl/api/events/$eventId/badges.pdf"
$bulkFile = "$env:TEMP/bulk_badges_test.pdf"
Invoke-WebRequest -Uri $bulkUrl -Method Get -Headers @{ Authorization = "Bearer $orgToken" } -OutFile $bulkFile -UseBasicParsing

$bulkBytes = [System.IO.File]::ReadAllBytes($bulkFile)
Write-Host "-> Bulk Badges PDF received! Size: $($bulkBytes.Length) bytes" -ForegroundColor Green

$bulkMagic = [System.Text.Encoding]::ASCII.GetString($bulkBytes[0..4])
Write-Host "-> Bulk PDF Header Magic: $bulkMagic" -ForegroundColor Cyan
if ($bulkMagic -ne "%PDF-") {
    Write-Host "Failed: Bulk file does not start with %PDF- header!" -ForegroundColor Red
    exit 1
}

# 6. Verify Access Control (Attendee cannot download bulk badges)
Write-Host "`n[6] Verifying access control: Attendee downloading bulk badges should be rejected..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $bulkUrl -Method Get -Headers @{ Authorization = "Bearer $attToken" } -UseBasicParsing
    Write-Host "Failed: Attendee was able to download bulk badges without permission!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host "-> Access correctly denied for attendee: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ALL PHASE 11 CONFERENCE BADGE TESTS PASSED 100%!        " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
