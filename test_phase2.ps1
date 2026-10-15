$ErrorActionPreference = "Stop"

# Register unique test organizer
$orgEmail = "org_" + (Get-Random) + "@test.com"
$regOrg = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/register" -Method Post -ContentType "application/json" -Body (@{
    fullName = "Test Organizer"
    email = $orgEmail
    password = "OrgPassword@123"
    role = "ROLE_ORGANIZER"
} | ConvertTo-Json)

$orgToken = $regOrg.data.accessToken
Write-Host "1. Registered Organizer: $orgEmail"

# Create Draft Event
$createRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events" -Method Post -Headers @{ Authorization = "Bearer $orgToken" } -ContentType "application/json" -Body (@{
    title = "Pulse Developer Summit 2026"
    categoryId = 1
    venueId = 1
    shortSummary = "Premier developer gathering for modern cloud architecture"
    description = "Hands on deep dives into reactive systems and distributed transactions."
    bannerImageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87"
    startTime = "2026-11-20T10:00:00"
    endTime = "2026-11-22T18:00:00"
    featured = $true
} | ConvertTo-Json)

$eventId = $createRes.data.id
Write-Host "2. Created Draft Event ID: $eventId (Status: $($createRes.data.status))"

# Submit Draft for Approval
$submitRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/submit" -Method Post -Headers @{ Authorization = "Bearer $orgToken" }
Write-Host "3. Submitted for Approval: $eventId (Status: $($submitRes.data.status))"

# 4. Login as Seeded Administrator
$loginAdmin = Invoke-RestMethod -Uri "http://localhost:8085/api/auth/login" -Method Post -ContentType "application/json" -Body (@{
    email = "admin@pulsevent.io"
    password = "AdminPassword@123"
} | ConvertTo-Json)

$adminToken = $loginAdmin.data.accessToken
Write-Host "4. Logged in as Administrator: admin@pulsevent.io"

# Admin Approves Event
$approveRes = Invoke-RestMethod -Uri "http://localhost:8085/api/admin/events/$eventId/approve" -Method Post -Headers @{ Authorization = "Bearer $adminToken" }
Write-Host "5. Admin Approved Event ID: $eventId (Status: $($approveRes.data.status))"

# Organizer Publishes Event
$publishRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events/$eventId/publish" -Method Post -Headers @{ Authorization = "Bearer $orgToken" }
Write-Host "6. Organizer Published Event ID: $eventId (Status: $($publishRes.data.status))"

# Public Discovery Search
$searchRes = Invoke-RestMethod -Uri "http://localhost:8085/api/events?search=Summit" -Method Get
Write-Host "7. Public Discovery: Found $($searchRes.data.content.Count) published event(s) matching 'Summit'"
Write-Host "   Title: $($searchRes.data.content[0].title)"
Write-Host "   Slug: $($searchRes.data.content[0].slug)"
Write-Host "   Category: $($searchRes.data.content[0].category.name)"
Write-Host "   Status: $($searchRes.data.content[0].status)"
