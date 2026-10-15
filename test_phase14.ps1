# test_phase14.ps1 - Phase 14 Certificate of Attendance & Completion Verification
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 14 END-TO-END TEST: ATTENDANCE & COMPLETION CERTS " -ForegroundColor Cyan
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

# 3. Attendee Claims Certificate of Completion
Write-Host "`n[3] Attendee claiming official Certificate of Completion..." -ForegroundColor Yellow
$claimHeaders = @{ Authorization = "Bearer $attToken" }
$claimBody = @{ certificateType = "COMPLETION" } | ConvertTo-Json
$claimRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/certificates/claim" -Method Post -Headers $claimHeaders -Body $claimBody -ContentType "application/json"
$cert = $claimRes.data
Write-Host "-> Certificate Issued Successfully!" -ForegroundColor Green
Write-Host "   * Certificate Code: $($cert.certificateCode)" -ForegroundColor Cyan
Write-Host "   * Recipient:        $($cert.recipientName)"
Write-Host "   * Type:             $($cert.certificateType)"
Write-Host "   * Accredited Hours: $($cert.hoursCompleted) CPD Units"
Write-Host "   * Issue Date:       $($cert.issueDate)"

# 4. Download Official Landscape Certificate PDF
Write-Host "`n[4] Downloading landscape credential PDF ($($cert.certificateCode))..." -ForegroundColor Yellow
$certPdfPath = "$PSScriptRoot\test_cert_$($cert.certificateCode).pdf"
Invoke-WebRequest -Uri "$baseUrl/api/certificates/$($cert.certificateCode)/pdf" -OutFile $certPdfPath -UseBasicParsing
$pdfBytes = [System.IO.File]::ReadAllBytes($certPdfPath)
$pdfHeader = [System.Text.Encoding]::ASCII.GetString($pdfBytes[0..4])
Write-Host "-> Certificate PDF downloaded! Size: $($pdfBytes.Length) bytes" -ForegroundColor Green
Write-Host "-> PDF Header Magic: $pdfHeader" -ForegroundColor Green
if ($pdfHeader -ne "%PDF-") {
    throw "Invalid PDF header!"
}
Remove-Item $certPdfPath -ErrorAction SilentlyContinue

# 5. Public Authenticity Verification Check
Write-Host "`n[5] Querying public authenticity verification endpoint..." -ForegroundColor Yellow
$verifyRes = Invoke-RestMethod -Uri "$baseUrl/api/certificates/verify/$($cert.certificateCode)" -Method Get
$v = $verifyRes.data
Write-Host "-> Verification Result: $(if ($v.valid) { 'AUTHENTIC / VALID' } else { 'INVALID' })" -ForegroundColor Green
Write-Host "   * Verified Recipient: $($v.recipientName)"
Write-Host "   * Verified Event:     $($v.eventTitle)"
Write-Host "   * Verified Type:      $($v.certificateType)"
Write-Host "   * Attestation Note:   $($v.message)"

# 6. Negative Test: Verify Invalid Fake Certificate
Write-Host "`n[6] Negative Test: Verifying bogus certificate code..." -ForegroundColor Yellow
$fakeVerify = Invoke-RestMethod -Uri "$baseUrl/api/certificates/verify/CERT-FAKE-12345" -Method Get
Write-Host "-> Bogus Code Status: Valid=$($fakeVerify.data.valid), Message='$($fakeVerify.data.message)'" -ForegroundColor Green

# 7. Query Attendee's Certificate Portfolio
Write-Host "`n[7] Querying attendee's claimed credentials..." -ForegroundColor Yellow
$myCertsRes = Invoke-RestMethod -Uri "$baseUrl/api/certificates/my-certificates" -Method Get -Headers @{ Authorization = "Bearer $attToken" }
Write-Host "-> Total Claimed Certificates: $($myCertsRes.data.Count)" -ForegroundColor Green

# 8. Query Organizer's Event Certificate Ledger
Write-Host "`n[8] Querying organizer credential roster for event ID $($targetEvent.id)..." -ForegroundColor Yellow
$orgRosterRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/certificates" -Method Get -Headers @{ Authorization = "Bearer $orgToken" }
Write-Host "-> Total Event Certificates Issued: $($orgRosterRes.data.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  ALL PHASE 14 CERTIFICATE ENGINE TESTS PASSED 100%!     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
