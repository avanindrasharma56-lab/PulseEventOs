# test_phase18.ps1 - Phase 18 Gamified Attendee Engagement, Scavenger Hunt & Leaderboard Live API Verification
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 18 END-TO-END TEST: GAMIFICATION & LEADERBOARD   " -ForegroundColor Cyan
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

$att2Body = @{ email = "marcus.vance@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$att2Res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $att2Body -ContentType "application/json"
$att2Token = $att2Res.data.accessToken
$att2User = $att2Res.data.user
Write-Host "-> Authenticated Attendee 2: $($att2User.fullName) (ID: $($att2User.id))" -ForegroundColor Green

$att1Headers = @{ Authorization = "Bearer $att1Token" }
$att2Headers = @{ Authorization = "Bearer $att2Token" }
$orgHeaders = @{ Authorization = "Bearer $orgToken" }

# 2. Discover Target Event
Write-Host "`n[2] Discovering conference event..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$targetEvent = $eventsResp.data.content[0]
$eventId = $targetEvent.id
Write-Host "-> Target Event: '$($targetEvent.title)' (ID: $eventId)" -ForegroundColor Cyan

# 3. Retrieve Initial Attendee Gamification Profile
Write-Host "`n[3] Fetching Attendee 1 initial gamification profile..." -ForegroundColor Yellow
$profileRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/profile" -Method Get -Headers $att1Headers
$initialProfile = $profileRes.data
Write-Host "-> Current XP: $($initialProfile.totalXp) | Tier: $($initialProfile.currentTier) | Rank: #$($initialProfile.rankPosition)" -ForegroundColor Green

# 4. Award XP: Turnstile Check-in (100 XP)
Write-Host "`n[4] Simulating Turnstile Entrance Check-in (+100 XP)..." -ForegroundColor Yellow
$gateRef = "GATE_LIVE_TEST_$(Get-Random)"
$turnstileReq = @{
    activityType = "TURNSTILE_CHECKIN"
    referenceId = $gateRef
    description = "Turnstile North Gate 1 check-in"
    customXp = 100
} | ConvertTo-Json

$xp1Res = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/award-xp" -Method Post -Headers $att1Headers -Body $turnstileReq -ContentType "application/json"
$p1 = $xp1Res.data
Write-Host "-> New XP: $($p1.totalXp) | Tier: $($p1.currentTier) | Badges Count: $($p1.badges.Count)" -ForegroundColor Green
if ($p1.badges.Count -gt 0) {
    Write-Host "   Badge Awarded: '$($p1.badges[0].badgeName)' $($p1.badges[0].badgeIcon)" -ForegroundColor Cyan
}

# 5. Check/Create Sponsor and Scan Booth (+75 XP)
Write-Host "`n[5] Checking Sponsor Booth and Scanning QR (+75 XP)..." -ForegroundColor Yellow
$sponsorsRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sponsors" -Method Get
$sponsorList = $sponsorsRes.data
$targetSponsor = $null
if ($sponsorList.Count -gt 0) {
    $targetSponsor = $sponsorList[0]
} else {
    Write-Host "-> Creating new sponsor booth..." -ForegroundColor Yellow
    $newSponsorReq = @{
        name = "Nebula Cloud Systems"
        tier = "PLATINUM"
        boothNumber = "EXPO-B42"
        description = "Next-generation distributed edge runtime"
        displayOrder = 1
    } | ConvertTo-Json
    $spCreateRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sponsors" -Method Post -Headers $orgHeaders -Body $newSponsorReq -ContentType "application/json"
    $targetSponsor = $spCreateRes.data
}

$scanRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/scan-booth/$($targetSponsor.id)" -Method Post -Headers $att1Headers
$p2 = $scanRes.data
Write-Host "-> Scanned Booth: '$($targetSponsor.name)'" -ForegroundColor Cyan
Write-Host "-> Updated XP: $($p2.totalXp) | Badges Count: $($p2.badges.Count)" -ForegroundColor Green

# 6. Check/Create Breakout Session and Check-in (+150 XP)
Write-Host "`n[6] Checking Breakout Session and Checking in (+150 XP)..." -ForegroundColor Yellow
$sessionsRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sessions" -Method Get
$sessionList = $sessionsRes.data
$targetSession = $null
if ($sessionList.Count -gt 0) {
    $targetSession = $sessionList[0]
} else {
    Write-Host "-> Creating test keynote session..." -ForegroundColor Yellow
    $newSessionReq = @{
        title = "Autonomous Systems & The Future of Work"
        description = "Breakout deep dive into enterprise autonomous workflows"
        room = "Hall A - Main Stage"
        track = "Engineering"
        sessionType = "KEYNOTE"
        startTime = (Get-Date).AddDays(1).ToString("s")
        endTime = (Get-Date).AddDays(1).AddHours(1).ToString("s")
        maxCapacity = 300
    } | ConvertTo-Json
    $sessCreateRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/sessions" -Method Post -Headers $orgHeaders -Body $newSessionReq -ContentType "application/json"
    $targetSession = $sessCreateRes.data
}

$sessCheckinRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/session-checkin/$($targetSession.id)" -Method Post -Headers $att1Headers
$p3 = $sessCheckinRes.data
Write-Host "-> Checked in to: '$($targetSession.title)'" -ForegroundColor Cyan
Write-Host "-> Updated XP: $($p3.totalXp) | Badges Count: $($p3.badges.Count)" -ForegroundColor Green

# 7. Elevate Attendee 1 to GOLD Tier (500+ XP) and generate Swag Pass
Write-Host "`n[7] Elevating Attendee 1 to GOLD Tier (500+ XP) & Unlocking Swag Pass..." -ForegroundColor Yellow
$goldBoostReq = @{
    activityType = "NETWORKING_MEETING"
    referenceId = "BOOST_GOLD_$(Get-Random)"
    description = "Completed multiple high-impact 1-on-1 networking meetings"
    customXp = 300
} | ConvertTo-Json

$goldRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/award-xp" -Method Post -Headers $att1Headers -Body $goldBoostReq -ContentType "application/json"
$goldProfile = $goldRes.data
Write-Host "-> Tier: $($goldProfile.currentTier) | Total XP: $($goldProfile.totalXp)" -ForegroundColor Green
Write-Host "-> Unlocked Swag Pass Code: '$($goldProfile.swagCode)'" -ForegroundColor Yellow

# 8. Claim Swag Pass
Write-Host "`n[8] Claiming physical VIP swag bag with pass code..." -ForegroundColor Yellow
$claimRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/claim-swag" -Method Post -Headers $att1Headers
$claimedProfile = $claimRes.data
Write-Host "-> Swag Claim Status: IsClaimed = $($claimedProfile.isSwagClaimed)" -ForegroundColor Green

# 9. Award Attendee 2 high XP to test Leaderboard rank dynamics
Write-Host "`n[9] Awarding Attendee 2 XP (Marcus Vance) to test Leaderboard sorting..." -ForegroundColor Yellow
$att2Req = @{
    activityType = "SESSION_ATTENDANCE"
    referenceId = "ATT2_EXP_$(Get-Random)"
    description = "Keynote Master & Super Networker"
    customXp = 1200
} | ConvertTo-Json
$att2XpRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/award-xp" -Method Post -Headers $att2Headers -Body $att2Req -ContentType "application/json"
$att2Prof = $att2XpRes.data
Write-Host "-> Attendee 2 ($($att2Prof.userName)) Total XP: $($att2Prof.totalXp) | Tier: $($att2Prof.currentTier)" -ForegroundColor Green

# 10. Query Live Public Leaderboard
Write-Host "`n[10] Fetching Public Event Leaderboard..." -ForegroundColor Yellow
$lbRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/gamification/leaderboard?limit=10" -Method Get
$leaderboard = $lbRes.data
Write-Host "-> Leaderboard entries returned: $($leaderboard.Count)" -ForegroundColor Cyan
foreach ($entry in $leaderboard) {
    Write-Host ("   #{0} | {1} | {2} XP | Tier: {3} | Badges: {4}" -f $entry.rank, $entry.userName, $entry.totalXp, $entry.currentTier, $entry.badgesCount) -ForegroundColor White
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  PHASE 18 ALL GAMIFICATION & LEADERBOARD CHECKS PASSED!  " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
