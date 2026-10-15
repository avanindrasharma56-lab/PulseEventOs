# test_phase16.ps1 - Phase 16 Multi-Track Breakout Session Room Booking & Schedule Verification
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 16 END-TO-END TEST: BREAKOUT ROOM BOOKING & RSVPS " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Authenticate as Organizer and Attendee
Write-Host "`n[1] Authenticating users..." -ForegroundColor Yellow
$organizerBody = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$orgRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $organizerBody -ContentType "application/json"
$orgToken = $orgRes.data.accessToken
Write-Host "-> Authenticated Organizer: $($orgRes.data.user.fullName)" -ForegroundColor Green

$attendeeBody = @{ email = "alex.rivera@pulsevent.io"; password = "Password@123" } | ConvertTo-Json
$attRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $attendeeBody -ContentType "application/json"
$attToken = $attRes.data.accessToken
Write-Host "-> Authenticated Attendee:  $($attRes.data.user.fullName)" -ForegroundColor Green

# 2. Discover Target Event
Write-Host "`n[2] Discovering conference event..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$targetEvent = $eventsResp.data.content[0]
Write-Host "-> Target Event: '$($targetEvent.title)' (ID: $($targetEvent.id))" -ForegroundColor Cyan

# 3. Create or Fetch Breakout Sessions
Write-Host "`n[3] Fetching conference sessions..." -ForegroundColor Yellow
$sessionsRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/sessions" -Method Get
$sessions = $sessionsRes.data

$targetSession = $null
if ($sessions.Count -gt 0) {
    $targetSession = $sessions[0]
} else {
    Write-Host "-> Creating new breakout session for event..." -ForegroundColor Yellow
    $orgHeaders = @{ Authorization = "Bearer $orgToken" }
    $createSessionBody = @{
        eventId = $targetEvent.id
        title = "Hands-On Kubernetes & Service Mesh Deep Dive"
        description = "Advanced traffic splitting, canary rollouts, and eBPF observability."
        room = "Breakout Pavilion A"
        track = "Cloud Architecture"
        sessionType = "WORKSHOP"
        startTime = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
        endTime = (Get-Date).AddDays(2).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss")
        maxCapacity = 75
    } | ConvertTo-Json
    $newSessionRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/sessions" -Method Post -Headers $orgHeaders -Body $createSessionBody -ContentType "application/json"
    $targetSession = $newSessionRes.data
}

Write-Host "-> Target Breakout Session: '$($targetSession.title)' (ID: $($targetSession.id))" -ForegroundColor Green
Write-Host "   * Room:     $($targetSession.room)"
Write-Host "   * Track:    $($targetSession.track)"
Write-Host "   * Capacity: $($targetSession.maxCapacity) seats"

# 4. Attendee Reserves Seat in Breakout Session
Write-Host "`n[4] Attendee reserving breakout seat..." -ForegroundColor Yellow
$attHeaders = @{ Authorization = "Bearer $attToken" }

$rsvpRes = Invoke-RestMethod -Uri "$baseUrl/api/sessions/$($targetSession.id)/rsvp" -Method Post -Headers $attHeaders
$rsvp = $rsvpRes.data
Write-Host "-> Breakout Seat Reserved Successfully!" -ForegroundColor Green
Write-Host "   * RSVP Code: $($rsvp.rsvpCode)" -ForegroundColor Cyan
Write-Host "   * Status:    $($rsvp.status)"
Write-Host "   * Room:      $($rsvp.room)"
Write-Host "   * Reserved:  $($rsvp.reservedAt)"

# 5. Check Live Room Capacity
Write-Host "`n[5] Querying live room seating capacity..." -ForegroundColor Yellow
$capRes = Invoke-RestMethod -Uri "$baseUrl/api/sessions/$($targetSession.id)/capacity" -Method Get
$cap = $capRes.data
Write-Host "-> Capacity Status:" -ForegroundColor Green
Write-Host "   * Total Seats:     $($cap.maxCapacity)"
Write-Host "   * Reserved:        $($cap.reservedSeats)"
Write-Host "   * Available Seats: $($cap.availableSeats)"
Write-Host "   * Occupancy:       $($cap.occupancyPercentage)%"

# 6. Retrieve Attendee Personal Schedule
Write-Host "`n[6] Fetching attendee's personal confirmed schedule..." -ForegroundColor Yellow
$schedRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/my-rsvps" -Method Get -Headers $attHeaders
Write-Host "-> Total Scheduled Sessions: $($schedRes.data.Count)" -ForegroundColor Green
foreach ($s in $schedRes.data) {
    Write-Host "   - [$($s.status)] '$($s.sessionTitle)' in $($s.room) ($($s.rsvpCode))" -ForegroundColor Cyan
}

# 7. Query Multi-Track Room Heatmap
Write-Host "`n[7] Querying multi-track room capacity heatmap..." -ForegroundColor Yellow
$heatmapRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/sessions/heatmap" -Method Get
Write-Host "-> Room Heatmap Loaded ($($heatmapRes.data.Count) rooms/zones):" -ForegroundColor Green
foreach ($h in $heatmapRes.data) {
    Write-Host "   * $($h.room) [$($h.track)]: $($h.totalReserved)/$($h.totalCapacity) seats ($($h.averageOccupancyPercentage)% avg occupancy)" -ForegroundColor Gray
}

# 8. Cancel RSVP & Release Seat
Write-Host "`n[8] Attendee cancelling breakout seat reservation..." -ForegroundColor Yellow
$cancelRes = Invoke-RestMethod -Uri "$baseUrl/api/sessions/$($targetSession.id)/rsvp" -Method Delete -Headers $attHeaders
Write-Host "-> Reservation Cancelled! Status: $($cancelRes.data.status)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  PHASE 16 VERIFICATION COMPLETED SUCCESSFULLY!           " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
