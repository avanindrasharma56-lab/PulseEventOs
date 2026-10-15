# test_phase12.ps1
# End-to-end regression test for Phase 12: Sponsor & Exhibitor Booth Floor with Lead Retrieval Engine

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 12 END-TO-END TEST: SPONSORS & EXHIBITOR LEADS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Login as Organizer & Attendee
Write-Host "`n[1] Authenticating users..." -ForegroundColor Yellow
$orgLogin = @{
    email = "admin@pulsevent.io"
    password = "AdminPassword@123"
} | ConvertTo-Json
$orgResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $orgLogin -ContentType "application/json"
$orgToken = $orgResp.data.accessToken

$attLogin = @{
    email = "alex.rivera@pulsevent.io"
    password = "Password@123"
} | ConvertTo-Json
$attResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $attLogin -ContentType "application/json"
$attToken = $attResp.data.accessToken
Write-Host "-> Authenticated Organizer ($($orgResp.data.user.fullName)) and Attendee ($($attResp.data.user.fullName))" -ForegroundColor Green

# 2. Discover Target Event
Write-Host "`n[2] Discovering conference event..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$event = $eventsResp.data.content[0]
$eventId = $event.id
Write-Host "-> Target Event: '$($event.title)' (ID: $eventId)" -ForegroundColor Green

# 3. Organizer Registers Conference Sponsors
Write-Host "`n[3] Registering conference sponsors as Organizer..." -ForegroundColor Yellow

$sponsor1 = @{
    name = "CloudScale Technologies"
    tier = "TITLE"
    boothNumber = "Booth #T-01 (Grand Hallway)"
    websiteUrl = "https://cloudscale.io"
    description = "Global hyperscale serverless cloud platform with 99.999% SLA."
    displayOrder = 1
} | ConvertTo-Json

$sp1Resp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sponsors" -Method Post -Body $sponsor1 -ContentType "application/json" -Headers @{ Authorization = "Bearer $orgToken" }
$targetSponsor = $sp1Resp.data
$sponsorId = $targetSponsor.id
Write-Host "-> Registered Title Sponsor: $($targetSponsor.name) (ID: $sponsorId)" -ForegroundColor Green

$sponsor2 = @{
    name = "CognitiveScale AI Labs"
    tier = "PLATINUM"
    boothNumber = "Booth #P-12 (Expo A)"
    websiteUrl = "https://cognitivescale.ai"
    description = "Multimodal foundation models for autonomous cloud infrastructure."
    displayOrder = 2
} | ConvertTo-Json

$sp2Resp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sponsors" -Method Post -Body $sponsor2 -ContentType "application/json" -Headers @{ Authorization = "Bearer $orgToken" }
Write-Host "-> Registered Platinum Sponsor: $($sp2Resp.data.name)" -ForegroundColor Green

# 4. Query Public Sponsor Directory
Write-Host "`n[4] Querying public sponsor directory..." -ForegroundColor Yellow
$sponsorsResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sponsors" -Method Get
$sponsors = $sponsorsResp.data
Write-Host "-> Found $($sponsors.Count) active event sponsor(s):" -ForegroundColor Green
foreach ($sp in $sponsors) {
    Write-Host "   * [$($sp.tier)] $($sp.name) - $($sp.boothNumber)" -ForegroundColor Cyan
}

# 5. Attendee Scans Badge at Sponsor Booth
Write-Host "`n[5] Scanning attendee badge at sponsor booth '$($targetSponsor.name)'..." -ForegroundColor Yellow
$myTickets = Invoke-RestMethod -Uri "$baseUrl/api/tickets/my-tickets" -Method Get -Headers @{ Authorization = "Bearer $attToken" }
$ticket = $myTickets.data[0]
$ticketCode = $ticket.ticketCode

$leadScanBody = @{
    ticketCode = $ticketCode
    notes = "VP of Engineering evaluating cloud deployment pipeline. High intent."
    rating = 5
} | ConvertTo-Json

$scanResp = Invoke-RestMethod -Uri "$baseUrl/api/sponsors/$sponsorId/leads/scan" -Method Post -Body $leadScanBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $attToken" }
Write-Host "-> Lead Captured: $($scanResp.data.attendeeName) ($($scanResp.data.attendeeEmail))" -ForegroundColor Green
Write-Host "-> Lead Rating:   $($scanResp.data.rating)/5 stars" -ForegroundColor Green
Write-Host "-> Lead Notes:    $($scanResp.data.notes)" -ForegroundColor Green

# 6. Organizer/Exhibitor Queries Captured Leads
Write-Host "`n[6] Querying captured leads roster for sponsor..." -ForegroundColor Yellow
$leadsResp = Invoke-RestMethod -Uri "$baseUrl/api/sponsors/$sponsorId/leads" -Method Get -Headers @{ Authorization = "Bearer $orgToken" }
$leads = $leadsResp.data
Write-Host "-> Total Leads Retrieved: $($leads.Count)" -ForegroundColor Green

if ($leads.Count -lt 1) {
    Write-Host "Failed: No leads recorded for sponsor!" -ForegroundColor Red
    exit 1
}

# 7. Organizer Exports Leads as CSV
Write-Host "`n[7] Exporting leads as CSV spreadsheet..." -ForegroundColor Yellow
$csvFile = "$env:TEMP/sponsor_leads_export.csv"
Invoke-WebRequest -Uri "$baseUrl/api/sponsors/$sponsorId/leads/export.csv" -Method Get -Headers @{ Authorization = "Bearer $orgToken" } -OutFile $csvFile -UseBasicParsing

$csvText = [System.IO.File]::ReadAllText($csvFile)
Write-Host "-> CSV Content Preview:`n$($csvText.Trim())" -ForegroundColor Cyan

if (-not $csvText.Contains("Lead ID,Attendee Name,Attendee Email,Ticket Code")) {
    Write-Host "Failed: CSV missing expected header row!" -ForegroundColor Red
    exit 1
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ALL PHASE 12 SPONSOR & LEAD RETRIEVAL TESTS PASSED!     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
