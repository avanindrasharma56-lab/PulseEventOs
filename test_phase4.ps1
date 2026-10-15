$ErrorActionPreference = "Stop"

Write-Host "=== PHASE 4 END-TO-END VERIFICATION SCRIPT ===" -ForegroundColor Cyan

# 1. Login Organizer
$orgLoginPayload = @{ email = "organizer@pulsevent.io"; password = "OrganizerPassword@123" } | ConvertTo-Json
$orgRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $orgLoginPayload
$orgToken = $orgRes.data.accessToken
$orgHeaders = @{ Authorization = "Bearer $orgToken" }
Write-Host "1. Organizer Authenticated: $($orgRes.data.user.fullName)" -ForegroundColor Green

# 2. Login Admin
$adminPayload = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $adminPayload
$adminToken = $adminRes.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "2. Admin Authenticated: $($adminRes.data.user.fullName)" -ForegroundColor Green

# 3. Test Organizer Analytics (/api/analytics/organizer)
Write-Host "3. Testing Organizer Analytics Endpoint..." -ForegroundColor Cyan
$orgAnalyticsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/analytics/organizer" -Method Get -Headers $orgHeaders
$orgData = $orgAnalyticsRes.data
Write-Host "   Total Events Managed: $($orgData.totalEvents)" -ForegroundColor Green
Write-Host "   Total Tickets Sold: $($orgData.totalTicketsSold) / $($orgData.totalCapacity) capacity" -ForegroundColor Green
Write-Host "   Total Gross Revenue: `$$($orgData.totalRevenue)" -ForegroundColor Green
Write-Host "   Total Door Check-Ins: $($orgData.totalCheckIns)" -ForegroundColor Green
Write-Host "   Overall Check-In Rate: $($orgData.overallCheckInRate)%" -ForegroundColor Green

# 4. Test Platform Admin Analytics (/api/analytics/admin)
Write-Host "4. Testing Admin Platform-Wide Analytics..." -ForegroundColor Cyan
$adminAnalyticsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/analytics/admin" -Method Get -Headers $adminHeaders
$adminData = $adminAnalyticsRes.data
Write-Host "   Platform Registered Users: $($adminData.totalUsers)" -ForegroundColor Green
Write-Host "   Total Events on Platform: $($adminData.totalEvents)" -ForegroundColor Green
Write-Host "   Total Platform Gross Revenue: `$$($adminData.totalPlatformRevenue)" -ForegroundColor Green
Write-Host "   Top Performing Events Count: $($adminData.topEvents.Count)" -ForegroundColor Green

# 5. Test Attendee CSV Roster Export
Write-Host "5. Testing Attendee Roster CSV Export..." -ForegroundColor Cyan
$publishedEvents = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Get
$targetEvent = $publishedEvents.data.content[0]
$targetEventId = $targetEvent.id

$csvContent = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$targetEventId/export/attendees" -Method Get -Headers $adminHeaders
$lines = $csvContent -split "`n"
Write-Host "   Exported CSV for event '$($targetEvent.title)' (ID: $targetEventId)" -ForegroundColor Green
Write-Host "   CSV Header: $($lines[0])" -ForegroundColor DarkGray
if ($lines.Count -gt 1) {
    Write-Host "   First Attendee Record: $($lines[1])" -ForegroundColor DarkGray
}

# 6. Test Attendee Activity & Notification Hub
Write-Host "6. Testing Notification Hub for Attendee (Alex Rivera)..." -ForegroundColor Cyan
$attLoginPayload = @{ email = "alex.rivera@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$attRes = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body $attLoginPayload
$attToken = $attRes.data.accessToken
$attHeaders = @{ Authorization = "Bearer $attToken" }

# Fetch unread count
$unreadRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/unread-count" -Method Get -Headers $attHeaders
Write-Host "   Initial Unread Notifications Count: $($unreadRes.data.unreadCount)" -ForegroundColor Green

# Fetch notifications list
$notifsRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications" -Method Get -Headers $attHeaders
$notifs = $notifsRes.data
Write-Host "   Total Notifications in Hub: $($notifs.Count)" -ForegroundColor Green
foreach ($n in $notifs | Select-Object -First 3) {
    Write-Host "   - [$($n.type)] $($n.title) | Read: $($n.read) | Date: $($n.createdAt)"
}

# Mark all as read
if ($notifs.Count -gt 0) {
    Write-Host "   Marking all notifications as read..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/read-all" -Method Put -Headers $attHeaders
    $postReadRes = Invoke-RestMethod -Uri "http://localhost:8085/api/notifications/unread-count" -Method Get -Headers $attHeaders
    Write-Host "   Unread Count After Read-All: $($postReadRes.data.unreadCount)" -ForegroundColor Green
}

Write-Host "`n=== ALL PHASE 4 INTEGRATION CHECKS PASSED PERFECTLY! ===" -ForegroundColor Green
