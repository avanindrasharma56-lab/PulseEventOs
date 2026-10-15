$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 7 END-TO-END VERIFICATION SCRIPT ===" -ForegroundColor Cyan

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

# 3. Find Published Event
$eventsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$targetEvent = $eventsRes.data.content[0]
$eventId = $targetEvent.id
Write-Host "3. Target Conference: '$($targetEvent.title)' (ID: $eventId)" -ForegroundColor Cyan

# 4. Register Keynote Speaker
Write-Host "4. Registering Conference Speaker..." -ForegroundColor Cyan
$spkPayload = @{
    eventId = $eventId
    name = "Dr. Elena Vance"
    title = "VP of Artificial Intelligence"
    company = "CognitiveScale Labs"
    bio = "Keynote speaker on large distributed model reasoning and high-throughput real-time architectures."
    avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80"
    socialUrl = "https://linkedin.com/in/elena-vance-ai"
} | ConvertTo-Json

$spkRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/speakers" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $spkPayload
$speakerId = $spkRes.data.id
Write-Host "   Registered Speaker: $($spkRes.data.name) (ID: $speakerId, Company: $($spkRes.data.company))" -ForegroundColor Green

# Query Speakers list
$speakersRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/speakers" -Method Get
Write-Host "   Total Event Speakers: $($speakersRes.data.Count)" -ForegroundColor Green

# 5. Add Agenda Sessions
Write-Host "5. Creating Agenda Sessions across Tracks..." -ForegroundColor Cyan
$now = [DateTime]::UtcNow.AddDays(10)
$session1Payload = @{
    eventId = $eventId
    title = "Keynote: Next-Gen Autonomous Event Systems"
    description = "Opening address exploring real-time edge processing and pessimistic lock patterns."
    speakerId = $speakerId
    room = "Grand Auditorium"
    track = "Artificial Intelligence"
    sessionType = "KEYNOTE"
    startTime = $now.ToString("yyyy-MM-ddTHH:mm:ss")
    endTime = $now.AddHours(1.5).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json

$sess1Res = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/sessions" -Method Post -Headers $adminHeaders -ContentType "application/json" -Body $session1Payload
$sessionId1 = $sess1Res.data.id
Write-Host "   Added Session 1: '$($sess1Res.data.title)' (Room: $($sess1Res.data.room), Track: $($sess1Res.data.track))" -ForegroundColor Green

# Query Master Agenda
$agendaRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/sessions" -Method Get -Headers $attHeaders
Write-Host "   Total Master Agenda Sessions: $($agendaRes.data.Count)" -ForegroundColor Green

# 6. Attendee Bookmarks Session into Personal Schedule
Write-Host "6. Attendee Bookmarking Session..." -ForegroundColor Cyan
$bookmarkRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/sessions/$sessionId1/bookmark" -Method Post -Headers $attHeaders
Write-Host "   Bookmark State: $($bookmarkRes.data.bookmarked) | Message: $($bookmarkRes.message)" -ForegroundColor Green

$mySchedRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/my-schedule" -Method Get -Headers $attHeaders
Write-Host "   Attendee Personal Schedule Count: $($mySchedRes.data.Count)" -ForegroundColor Green
Write-Host "   Bookmarked Title: $($mySchedRes.data[0].title)" -ForegroundColor DarkGray

# 7. Test RFC 5545 iCalendar (.ics) Stream Export
Write-Host "7. Testing RFC 5545 iCalendar (.ics) Stream Export..." -ForegroundColor Cyan
$icsContent = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/export/ical" -Method Get -Headers $attHeaders
$lines = $icsContent -split "`r`n"
Write-Host "   Exported .ics calendar stream ($($lines.Count) lines)" -ForegroundColor Green

if ($icsContent -match "BEGIN:VCALENDAR" -and $icsContent -match "BEGIN:VEVENT" -and $icsContent -match "SUMMARY:Keynote: Next-Gen Autonomous Event Systems") {
    Write-Host "   RFC 5545 iCalendar format successfully validated!" -ForegroundColor Green
} else {
    throw "iCalendar export content did not contain expected RFC 5545 tags."
}

Write-Host "=== ALL PHASE 7 INTEGRATION CHECKS PASSED PERFECTLY! ===" -ForegroundColor Green
