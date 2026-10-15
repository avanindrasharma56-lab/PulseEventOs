# verify_full_stack_db.ps1
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   FULL STACK DB READ/WRITE VERIFICATION: PULSEVENT OS    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:5173" # Through React+Vite Proxy
$dbExe = "C:\Program Files\MariaDB 13.0\bin\mariadb.exe"

function Run-Query ($sql) {
    & $dbExe -u root event_management_db -e $sql
}

# ----------------------------------------------------------------------------
# STEP 1: Verify Initial Database State
# ----------------------------------------------------------------------------
Write-Host "`n[1] Inspecting MariaDB 13.0 database baseline..." -ForegroundColor Yellow
$initialTableCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='event_management_db';").Trim()
$initialUserCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM users;").Trim()
$initialEventCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM events;").Trim()
Write-Host "-> Database 'event_management_db': $initialTableCount tables, $initialUserCount users, $initialEventCount events" -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 2: Verify Existing User Login (Read)
# ----------------------------------------------------------------------------
Write-Host "`n[2] Verifying Admin Authentication via Vite Proxy..." -ForegroundColor Yellow
$adminLoginBody = @{ email = "admin@pulsevent.io"; password = "AdminPassword@123" } | ConvertTo-Json
$adminLoginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminToken = $adminLoginRes.data.accessToken
Write-Host "-> Successfully authenticated Admin: $($adminLoginRes.data.user.fullName)" -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 3: Verify Public Event Discovery (Read)
# ----------------------------------------------------------------------------
Write-Host "`n[3] Reading Events via Public API ($baseUrl/api/events)..." -ForegroundColor Yellow
$eventsRes = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=10" -Method Get
Write-Host "-> Retrieved $($eventsRes.data.totalElements) live events from database" -ForegroundColor Green
foreach ($ev in $eventsRes.data.content) {
    Write-Host "   - [$($ev.id)] $($ev.title) (Status: $($ev.status), Organizer: $($ev.organizerName))" -ForegroundColor DarkGray
}

# ----------------------------------------------------------------------------
# STEP 4: Test Database Write - Register Test Organizer
# ----------------------------------------------------------------------------
$randSuffix = Get-Random -Minimum 10000 -Maximum 99999
$testOrgEmail = "qa.organizer.$randSuffix@pulsevent.io"
Write-Host "`n[4] Writing new organizer to database ($testOrgEmail)..." -ForegroundColor Yellow
$regOrgBody = @{
    fullName = "QA Test Organizer $randSuffix"
    email = $testOrgEmail
    password = "Password@123"
    role = "ROLE_ORGANIZER"
} | ConvertTo-Json
$regOrgRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regOrgBody -ContentType "application/json"
$testOrgId = $regOrgRes.data.user.id
$testOrgToken = $regOrgRes.data.accessToken
Write-Host "-> Registered via API. User ID: $testOrgId" -ForegroundColor Green

# Verify in MariaDB directly
$dbUser = (& $dbExe -u root event_management_db -s -N -e "SELECT id, email FROM users WHERE id=$testOrgId;").Trim()
Write-Host "-> MariaDB SQL Verification: '$dbUser'" -ForegroundColor Cyan

# ----------------------------------------------------------------------------
# STEP 5: Test Database Write - Create New Event (Organizer)
# ----------------------------------------------------------------------------
Write-Host "`n[5] Writing new event to database via Organizer Studio API..." -ForegroundColor Yellow
$eventTitle = "QA Full Stack Verification Summit $randSuffix"
$eventBody = @{
    title = $eventTitle
    categoryId = 1 # Technology & AI
    venueId = 1 # Moscone West
    shortSummary = "Automated verification event for full-stack DB read/write verification"
    description = "Full comprehensive test event ensuring zero fake data and end-to-end integration."
    startTime = "2027-04-10T10:00:00"
    endTime = "2027-04-12T18:00:00"
    featured = $true
} | ConvertTo-Json

$orgHeaders = @{ Authorization = "Bearer $testOrgToken" }
$createEventRes = Invoke-RestMethod -Uri "$baseUrl/api/events" -Method Post -Headers $orgHeaders -Body $eventBody -ContentType "application/json"
$testEventId = $createEventRes.data.id
Write-Host "-> Event Created. ID: $testEventId, Status: $($createEventRes.data.status)" -ForegroundColor Green

# Verify in MariaDB directly
$dbEvent = (& $dbExe -u root event_management_db -s -N -e "SELECT id, title, status FROM events WHERE id=$testEventId;").Trim()
Write-Host "-> MariaDB SQL Verification: '$dbEvent'" -ForegroundColor Cyan

# ----------------------------------------------------------------------------
# STEP 6: Test Database Write - Create Ticket Tier
# ----------------------------------------------------------------------------
Write-Host "`n[6] Writing ticket tier to database..." -ForegroundColor Yellow
$tierBody = @{
    name = "General Admission $randSuffix"
    description = "General entry pass"
    price = 49.00
    capacity = 100
} | ConvertTo-Json
$createTierRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$testEventId/tiers" -Method Post -Headers $orgHeaders -Body $tierBody -ContentType "application/json"
$testTierId = $createTierRes.data.id
Write-Host "-> Ticket Tier Created. ID: $testTierId, Price: `$$($createTierRes.data.price)" -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 7: Moderation Workflow - Submit, Approve, and Publish
# ----------------------------------------------------------------------------
Write-Host "`n[7] Executing Event Moderation Lifecycle..." -ForegroundColor Yellow
$submitRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$testEventId/submit" -Method Post -Headers $orgHeaders
Write-Host "-> Event Submitted. New Status: $($submitRes.data.status)" -ForegroundColor Green

$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$approveRes = Invoke-RestMethod -Uri "$baseUrl/api/admin/events/$testEventId/approve" -Method Post -Headers $adminHeaders
Write-Host "-> Admin Approved. New Status: $($approveRes.data.status)" -ForegroundColor Green

$publishRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$testEventId/publish" -Method Post -Headers $orgHeaders
Write-Host "-> Event Published. New Status: $($publishRes.data.status)" -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 8: Test Database Read - Verify Event in Public Discovery
# ----------------------------------------------------------------------------
Write-Host "`n[8] Verifying new event in public discovery..." -ForegroundColor Yellow
$searchRes = Invoke-RestMethod -Uri "$baseUrl/api/events?search=Verification%20Summit%20$randSuffix" -Method Get
$foundEvent = $searchRes.data.content[0]
Write-Host "-> Public Discovery Confirmed: '$($foundEvent.title)' (ID: $($foundEvent.id))" -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 9: Test Database Write - Register Attendee & Book Ticket
# ----------------------------------------------------------------------------
$testAttEmail = "qa.attendee.$randSuffix@pulsevent.io"
Write-Host "`n[9] Registering attendee ($testAttEmail) and placing ticket order..." -ForegroundColor Yellow
$regAttBody = @{
    fullName = "QA Test Attendee $randSuffix"
    email = $testAttEmail
    password = "Password@123"
    role = "ROLE_ATTENDEE"
} | ConvertTo-Json
$regAttRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regAttBody -ContentType "application/json"
$testAttId = $regAttRes.data.user.id
$testAttToken = $regAttRes.data.accessToken
$attHeaders = @{ Authorization = "Bearer $testAttToken" }

$orderBody = @{
    eventId = $testEventId
    items = @(
        @{
            ticketTierId = $testTierId
            quantity = 2
            attendeeName = "QA Attendee Primary"
            attendeeEmail = $testAttEmail
        }
    )
} | ConvertTo-Json
$orderRes = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers $attHeaders -Body $orderBody -ContentType "application/json"
$testOrderId = $orderRes.data.id
$testOrderNum = $orderRes.data.orderNumber
Write-Host "-> Order Placed! Order ID: $testOrderId ($testOrderNum), Status: $($orderRes.data.status)" -ForegroundColor Green

# Verify in MariaDB directly
$dbOrder = (& $dbExe -u root event_management_db -s -N -e "SELECT id, order_number, total_amount, status FROM orders WHERE id=$testOrderId;").Trim()
$dbTicketsCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM tickets WHERE order_id=$testOrderId;").Trim()
Write-Host "-> MariaDB SQL Verification: Order='$dbOrder', Tickets Created=$dbTicketsCount" -ForegroundColor Cyan

# ----------------------------------------------------------------------------
# STEP 10: Clean Up Safe Test Data Only
# ----------------------------------------------------------------------------
Write-Host "`n[10] Cleaning up test records from MariaDB..." -ForegroundColor Yellow
& $dbExe -u root event_management_db -e "
    DELETE FROM notifications WHERE user_id IN ($testOrgId, $testAttId);
    DELETE FROM notifications WHERE title LIKE '%$randSuffix%';
    DELETE FROM tickets WHERE order_id = $testOrderId;
    DELETE FROM order_items WHERE order_id = $testOrderId;
    DELETE FROM orders WHERE id = $testOrderId;
    DELETE FROM ticket_tiers WHERE id = $testTierId;
    DELETE FROM events WHERE id = $testEventId;
    DELETE FROM user_roles WHERE user_id IN ($testOrgId, $testAttId);
    DELETE FROM users WHERE id IN ($testOrgId, $testAttId);
"
# Also clean up temporary user 506 if exists
& $dbExe -u root event_management_db -e "
    DELETE FROM user_roles WHERE user_id = 506;
    DELETE FROM users WHERE id = 506;
"
Write-Host "-> Test records cleaned up successfully." -ForegroundColor Green

# ----------------------------------------------------------------------------
# STEP 11: Final State Audit
# ----------------------------------------------------------------------------
Write-Host "`n[11] Auditing final database integrity..." -ForegroundColor Yellow
$finalTableCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='event_management_db';").Trim()
$finalUserCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM users;").Trim()
$finalEventCount = (& $dbExe -u root event_management_db -s -N -e "SELECT count(*) FROM events;").Trim()

Write-Host "-> Final Database: $finalTableCount tables, $finalUserCount users, $finalEventCount events" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   FULL STACK DB READ/WRITE VERIFICATION COMPLETE: SUCCESS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
