# test_phase9.ps1
# End-to-end regression test for Phase 9: Dynamic Surge / Early-Bird Pricing & Multi-Currency Engine

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 9 END-TO-END TEST: DYNAMIC SURGE & MULTI-CURRENCY" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8085"

# 1. Fetch supported currencies and real-time rates
Write-Host "`n[1] Fetching supported currencies..." -ForegroundColor Yellow
$currResp = Invoke-RestMethod -Uri "$baseUrl/api/pricing/currencies" -Method Get
$currencies = $currResp.data
Write-Host "-> Found $($currencies.Count) supported currencies:" -ForegroundColor Green
foreach ($c in $currencies) {
    Write-Host "   * $($c.currencyCode) ($($c.currencyName)) Symbol: $($c.symbol), Rate to USD: $($c.rateToUsd)" -ForegroundColor Cyan
}

if ($currencies.Count -lt 4) {
    Write-Host "Failed: Expected at least 4 currencies (USD, EUR, GBP, INR)" -ForegroundColor Red
    exit 1
}

# 2. Test Arbitrary Multi-Currency Conversions
Write-Host "`n[2] Testing multi-currency conversion (100 USD to EUR, GBP, INR)..." -ForegroundColor Yellow

$targets = @("EUR", "GBP", "INR")
foreach ($t in $targets) {
    $convBody = @{
        amount = 100.00
        from = "USD"
        to = $t
    } | ConvertTo-Json

    $convResp = Invoke-RestMethod -Uri "$baseUrl/api/pricing/convert" -Method Post -Body $convBody -ContentType "application/json"
    $data = $convResp.data
    Write-Host "-> `$100.00 USD = $($data.formatted) ($($data.toCurrency), Exchange Rate: $($data.exchangeRate))" -ForegroundColor Green
    if ($data.convertedAmount -le 0) {
        Write-Host "Failed: Converted amount must be positive!" -ForegroundColor Red
        exit 1
    }
}

# 3. Find published event & ticket tier
Write-Host "`n[3] Fetching active event and tier..." -ForegroundColor Yellow
$eventsResp = Invoke-RestMethod -Uri "$baseUrl/api/events?page=0&size=5" -Method Get
$event = $eventsResp.data.content[0]
$eventId = $event.id

$tiersResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/tiers" -Method Get
$tier = $tiersResp.data[0]
$tierId = $tier.id
Write-Host "-> Event: '$($event.title)' (ID: $eventId)" -ForegroundColor Green
Write-Host "-> Tier: '$($tier.name)' (ID: $tierId, Base Price: `$$($tier.price))" -ForegroundColor Green

# 4. Get Dynamic Price Quote in USD and EUR
Write-Host "`n[4] Requesting live dynamic price quote..." -ForegroundColor Yellow
$quoteUsd = Invoke-RestMethod -Uri "$baseUrl/api/pricing/tiers/$tierId/quote?currency=USD" -Method Get
$quoteData = $quoteUsd.data
Write-Host "-> Dynamic USD Quote: $($quoteData.formattedPrice)" -ForegroundColor Green
Write-Host "   Surge Active: $($quoteData.surgeActive), Multiplier: $($quoteData.surgeMultiplier)x" -ForegroundColor Cyan

$quoteEur = Invoke-RestMethod -Uri "$baseUrl/api/pricing/tiers/$tierId/quote?currency=EUR" -Method Get
Write-Host "-> Dynamic EUR Quote: $($quoteEur.data.formattedPrice) (Symbol: $($quoteEur.data.currencySymbol))" -ForegroundColor Green

# 5. Login as Organizer and create a custom Surge Pricing Rule
Write-Host "`n[5] Logging in as organizer to register custom dynamic surge rule..." -ForegroundColor Yellow
$loginBody = @{
    email = "organizer.p9@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $authResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $authResp.data.accessToken
} catch {
    # Fallback to admin
    $loginBody = @{
        email = "admin@pulsevent.io"
        password = "AdminPassword@123"
    } | ConvertTo-Json
    $authResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $authResp.data.accessToken
}

$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "-> Registering 25% surge rule (+25% when sold >= 20%)..." -ForegroundColor Yellow
$ruleUri = "$baseUrl/api/events/$eventId/pricing-rules?ruleType=CAPACITY_SURGE&thresholdPercent=20&multiplier=1.25"
$ruleResp = Invoke-RestMethod -Uri $ruleUri -Method Post -Headers $headers
Write-Host "-> Rule registered! Rule ID: $($ruleResp.data.id), Multiplier: $($ruleResp.data.multiplier)" -ForegroundColor Green

# 6. Verify event pricing rules listing
Write-Host "`n[6] Listing rules for event..." -ForegroundColor Yellow
$rulesResp = Invoke-RestMethod -Uri "$baseUrl/api/events/$eventId/pricing-rules" -Method Get
Write-Host "-> Total active pricing rules for event: $($rulesResp.data.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  PHASE 9 TESTS PASSED 100% SUCCESSFULLY!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
