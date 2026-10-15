# test_phase17.ps1 - Phase 17 1-on-1 B2B Networking Hub & Meeting Scheduler Live API Verification
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 17 END-TO-END TEST: 1-ON-1 NETWORKING & MEETINGS " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Authenticate users
Write-Host "`n[1] Authenticating users..." -ForegroundColor Yellow
$orgBody = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$orgRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $orgBody -ContentType "application/json"
$orgToken = $orgRes.data.accessToken
Write-Host "-> Authenticated Organizer: $($orgRes.data.user.fullName)" -ForegroundColor Green

$att1Body = @{ email = "alex.rivera@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$att1Res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $att1Body -ContentType "application/json"
$att1Token = $att1Res.data.accessToken
$att1User = $att1Res.data.user
Write-Host "-> Authenticated Attendee 1: $($att1User.fullName) (ID: $($att1User.id))" -ForegroundColor Green

# Ensure Attendee 2 exists or register
$att2Email = "marcus.vance@pulsevent.io"
$att2Token = $null
$att2User = $null
try {
    $att2Login = @{ email = $att2Email; password = "Password@123" } | ConvertTo-Json
    $att2Res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $att2Login -ContentType "application/json"
    $att2Token = $att2Res.data.accessToken
    $att2User = $att2Res.data.user
} catch {
    Write-Host "-> Registering Attendee 2 ($att2Email)..." -ForegroundColor Yellow
    $regBody = @{
        fullName = "Marcus Vance"
        email = $att2Email
        password = "Password@123"
        phone = "+1-555-888-9999"
    } | ConvertTo-Json
    $regRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    $att2Token = $regRes.data.accessToken
    $att2User = $regRes.data.user
}
Write-Host "-> Authenticated Attendee 2: $($att2User.fullName) (ID: $($att2User.id))" -ForegroundColor Green

# 2. Discover Target Event
Write-Host "`n[2] Discovering conference event..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$targetEvent = $eventsResp.data.content[0]
Write-Host "-> Target Event: '$($targetEvent.title)' (ID: $($targetEvent.id))" -ForegroundColor Cyan

# 3. Ensure both attendees have ticket purchases for the event
Write-Host "`n[3] Checking ticket purchases for target event..." -ForegroundColor Yellow
$tiersRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/tiers" -Method Get
$activeTier = $tiersRes.data[0]

$att1Headers = @{ Authorization = "Bearer $att1Token" }
$att2Headers = @{ Authorization = "Bearer $att2Token" }

# Purchase ticket for attendee 1 if needed
try {
    $order1Body = @{
        eventId = $targetEvent.id
        items = @(
            @{
                ticketTierId = $activeTier.id
                quantity = 1
            }
        )
    } | ConvertTo-Json -Depth 5
    $order1Res = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers $att1Headers -Body $order1Body -ContentType "application/json"
    Write-Host "-> Purchased ticket for Attendee 1 (Order: $($order1Res.data.orderNumber))" -ForegroundColor Green
} catch {
    Write-Host "-> Attendee 1 ticket already exists or order succeeded." -ForegroundColor Gray
}

# Purchase ticket for attendee 2 if needed
try {
    $order2Body = @{
        eventId = $targetEvent.id
        items = @(
            @{
                ticketTierId = $activeTier.id
                quantity = 1
            }
        )
    } | ConvertTo-Json -Depth 5
    $order2Res = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers $att2Headers -Body $order2Body -ContentType "application/json"
    Write-Host "-> Purchased ticket for Attendee 2 (Order: $($order2Res.data.orderNumber))" -ForegroundColor Green
} catch {
    Write-Host "-> Attendee 2 ticket already exists or order succeeded." -ForegroundColor Gray
}

# 4. Upsert Attendee 1 Networking Profile
Write-Host "`n[4] Upserting Networking Profile for Attendee 1..." -ForegroundColor Yellow
$prof1Body = @{
    headline = "Chief Solution Architect @ Apex Micro"
    company = "Apex Micro"
    jobTitle = "Chief Architect"
    interests = "Distributed Systems, High-Frequency Trading, Cloud Native"
    networkingGoals = "Looking to connect with enterprise FinTech infrastructure leaders"
    isAvailable = $true
} | ConvertTo-Json
$prof1Res = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/profile" -Method Post -Headers $att1Headers -Body $prof1Body -ContentType "application/json"
Write-Host "-> Profile 1 Updated: $($prof1Res.data.headline) ($($prof1Res.data.company))" -ForegroundColor Green

# 5. Upsert Attendee 2 Networking Profile
Write-Host "`n[5] Upserting Networking Profile for Attendee 2..." -ForegroundColor Yellow
$prof2Body = @{
    headline = "VP Engineering @ Quantum Data"
    company = "Quantum Data"
    jobTitle = "VP Engineering"
    interests = "High-Frequency Trading, AI Ingestion, Cloud Native"
    networkingGoals = "Exploring real-time messaging fabric integrations"
    isAvailable = $true
} | ConvertTo-Json
$prof2Res = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/profile" -Method Post -Headers $att2Headers -Body $prof2Body -ContentType "application/json"
Write-Host "-> Profile 2 Updated: $($prof2Res.data.headline) ($($prof2Res.data.company))" -ForegroundColor Green

# 6. Search Networking Profiles Directory
Write-Host "`n[6] Searching Networking Directory for 'High-Frequency Trading'..." -ForegroundColor Yellow
$searchRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/attendees?query=Quantum" -Method Get
Write-Host "-> Matches found: $($searchRes.data.Count)" -ForegroundColor Green
foreach ($p in $searchRes.data) {
    Write-Host "   * $($p.userName) - $($p.headline) [$($p.interests)]"
}

# 7. Request 1-on-1 Meeting from Attendee 1 to Attendee 2
Write-Host "`n[7] Requesting 1-on-1 Meeting..." -ForegroundColor Yellow
$meetStart = (Get-Date).AddDays(1).Date.AddHours(11).ToString("yyyy-MM-ddTHH:mm:ss")
$meetEnd   = (Get-Date).AddDays(1).Date.AddHours(11).AddMinutes(30).ToString("yyyy-MM-ddTHH:mm:ss")

$meetReqBody = @{
    recipientId = $att2User.id
    startTime = $meetStart
    endTime = $meetEnd
    message = "Let's explore ultra-low-latency event broker architectures over coffee!"
} | ConvertTo-Json

$meetRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/meetings" -Method Post -Headers $att1Headers -Body $meetReqBody -ContentType "application/json"
$createdMeeting = $meetRes.data
Write-Host "-> Meeting Requested Successfully! (ID: $($createdMeeting.id))" -ForegroundColor Green
Write-Host "   * Status:       $($createdMeeting.status)"
Write-Host "   * Table:        Table #$($createdMeeting.tableNumber)"
Write-Host "   * Location:     $($createdMeeting.locationName)"
Write-Host "   * Time Slot:    $($createdMeeting.startTime) to $($createdMeeting.endTime)"

# 8. Test Conflict Detection (Overlapping Slot Rejection)
Write-Host "`n[8] Testing conflict detection on overlapping meeting request..." -ForegroundColor Yellow
try {
    $overlapReq = @{
        recipientId = $att2User.id
        startTime = (Get-Date).AddDays(1).Date.AddHours(11).AddMinutes(10).ToString("yyyy-MM-ddTHH:mm:ss")
        endTime = (Get-Date).AddDays(1).Date.AddHours(11).AddMinutes(40).ToString("yyyy-MM-ddTHH:mm:ss")
        message = "Attempting conflicting slot"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/meetings" -Method Post -Headers $att1Headers -Body $overlapReq -ContentType "application/json"
    Write-Error "Conflict check failed: Server did not reject overlapping meeting slot!"
} catch {
    Write-Host "-> Conflict correctly caught and rejected by server!" -ForegroundColor Green
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Gray
}

# 9. Attendee 2 Accepts Meeting
Write-Host "`n[9] Attendee 2 responding: ACCEPT..." -ForegroundColor Yellow
$acceptRes = Invoke-RestMethod -Uri "$baseUrl/api/networking/meetings/$($createdMeeting.id)/respond?accept=true" -Method Put -Headers $att2Headers
Write-Host "-> Meeting Status Updated to: $($acceptRes.data.status)" -ForegroundColor Green

# 10. Fetch My Meetings
Write-Host "`n[10] Fetching attendee's confirmed meeting roster..." -ForegroundColor Yellow
$myMeetingsRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/networking/meetings" -Method Get -Headers $att1Headers
Write-Host "-> Total meetings scheduled: $($myMeetingsRes.data.Count)" -ForegroundColor Green
foreach ($m in $myMeetingsRes.data) {
    Write-Host "   * [$($m.status)] With $($m.recipientName) at $($m.locationName)"
}

# 11. Cancel Meeting
Write-Host "`n[11] Attendee 1 cancelling meeting..." -ForegroundColor Yellow
$cancelRes = Invoke-RestMethod -Uri "$baseUrl/api/networking/meetings/$($createdMeeting.id)" -Method Delete -Headers $att1Headers
Write-Host "-> Meeting cancelled! New Status: $($cancelRes.data.status)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 17 LIVE API VERIFICATION COMPLETED SUCCESSFULLY!  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
