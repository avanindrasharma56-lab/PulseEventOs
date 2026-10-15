# test_phase13.ps1
# End-to-end regression test for Phase 13: AI Marketing & Copywriter Assistant (Powered by Gemini)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 13 END-TO-END TEST: AI EVENT MARKETING COPYWRITER" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Login as Organizer
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

# 3. Call AI Copywriter Generation
Write-Host "`n[3] Generating complete multi-channel conference marketing assets via AI..." -ForegroundColor Yellow
$copyRequest = @{
    topic = "Autonomous AI Agents & Cloud Robotics"
    category = "Artificial Intelligence"
    targetAudience = "Robotics Engineers, AI Researchers, Cloud Architects"
    tone = "Visionary"
    keyHighlights = "Spatial intelligence, Real-time edge compute, Multimodal SLAM algorithms"
} | ConvertTo-Json

$aiResp = Invoke-RestMethod -Uri "$baseUrl/api/ai/generate-event-copy" -Method Post -Body $copyRequest -ContentType "application/json" -Headers @{ Authorization = "Bearer $orgToken" }
$data = $aiResp.data

Write-Host "`n-> Title Suggestions ($($data.titleSuggestions.Count)):" -ForegroundColor Green
foreach ($t in $data.titleSuggestions) {
    Write-Host "   * $t" -ForegroundColor Cyan
}

Write-Host "`n-> Recommended URL Slug: $($data.recommendedSlug)" -ForegroundColor Green
Write-Host "`n-> Short Summary:`n$($data.shortSummary)" -ForegroundColor Green
Write-Host "`n-> Email Invitation Marketing Copy:`n$($data.emailMarketingCopy)" -ForegroundColor Cyan
Write-Host "`n-> Social Media Announcement Post:`n$($data.socialMediaPost)" -ForegroundColor Cyan

if ($data.titleSuggestions.Count -lt 3 -or [string]::IsNullOrWhiteSpace($data.shortSummary)) {
    Write-Host "Failed: Incomplete AI copy payload!" -ForegroundColor Red
    exit 1
}

# 4. Verify Access Control (Attendee cannot invoke AI copy generation)
Write-Host "`n[4] Verifying security guard: Attendee invoking AI copywriter must be rejected..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/api/ai/generate-event-copy" -Method Post -Body $copyRequest -ContentType "application/json" -Headers @{ Authorization = "Bearer $attToken" }
    Write-Host "Failed: Attendee was able to generate copy without organizer role!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host "-> Access correctly denied for attendee: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ALL PHASE 13 AI COPYWRITER TESTS PASSED 100%!           " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
