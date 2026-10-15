# test_phase15.ps1 - Phase 15 Live Interactive Q&A and Polls Verification
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PHASE 15 END-TO-END TEST: REAL-TIME AUDIENCE Q&A & POLLS" -ForegroundColor Cyan
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

# 3. Attendee Submits Live Q&A Question
Write-Host "`n[3] Attendee submitting question to live stage..." -ForegroundColor Yellow
$attHeaders = @{ Authorization = "Bearer $attToken" }
$questionBody = @{
    questionText = "Will slides and reproducible benchmark scripts be shared on GitHub after this keynote?"
    anonymous = $false
    sessionId = $null
} | ConvertTo-Json

$qRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/qa" -Method Post -Headers $attHeaders -Body $questionBody -ContentType "application/json"
$question = $qRes.data
Write-Host "-> Question Submitted Successfully!" -ForegroundColor Green
Write-Host "   * Question ID: $($question.id)" -ForegroundColor Cyan
Write-Host "   * Author:      $($question.authorName)"
Write-Host "   * Text:        '$($question.questionText)'"
Write-Host "   * Status:      $($question.status)"
Write-Host "   * Upvotes:     $($question.upvotes)"

# 4. Attendee Upvotes the Question
Write-Host "`n[4] Attendee upvoting question #$($question.id)..." -ForegroundColor Yellow
$upvoteRes = Invoke-RestMethod -Uri "$baseUrl/api/qa/$($question.id)/upvote" -Method Post -Headers $attHeaders
$upvotedQ = $upvoteRes.data
Write-Host "-> Upvoted successfully! Total upvotes: $($upvotedQ.upvotes) | User has upvoted: $($upvotedQ.userHasUpvoted)" -ForegroundColor Green

# 5. Organizer Pins the Question on Live Stage
Write-Host "`n[5] Organizer pinning question #$($question.id) on stage display..." -ForegroundColor Yellow
$orgHeaders = @{ Authorization = "Bearer $orgToken" }
$pinBody = @{ status = "PINNED" } | ConvertTo-Json
$pinRes = Invoke-RestMethod -Uri "$baseUrl/api/qa/$($question.id)/status" -Method Put -Headers $orgHeaders -Body $pinBody -ContentType "application/json"
Write-Host "-> Question status updated to: $($pinRes.data.status)" -ForegroundColor Green

# 6. Organizer Creates Live Audience Poll
Write-Host "`n[6] Organizer launching live audience poll..." -ForegroundColor Yellow
$pollBody = @{
    question = "Which vector index architecture do you run in production for RAG?"
    options = @("HNSW In-Memory", "IVF-PQ Approximate", "DiskANN Hybrid", "Native pgvector Flat")
} | ConvertTo-Json

$pollRes = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/polls" -Method Post -Headers $orgHeaders -Body $pollBody -ContentType "application/json"
$poll = $pollRes.data
Write-Host "-> Live Poll Created Successfully!" -ForegroundColor Green
Write-Host "   * Poll ID:     $($poll.id)" -ForegroundColor Cyan
Write-Host "   * Question:    '$($poll.question)'"
Write-Host "   * Status:      $($poll.status)"
Write-Host "   * Options:     $($poll.options.Count) options registered"

$selectedOption = $poll.options[0]

# 7. Attendee Votes in the Live Poll
Write-Host "`n[7] Attendee voting for Option '$($selectedOption.optionText)' (ID: $($selectedOption.id))..." -ForegroundColor Yellow
$voteBody = @{ optionId = $selectedOption.id } | ConvertTo-Json
$voteRes = Invoke-RestMethod -Uri "$baseUrl/api/polls/$($poll.id)/vote" -Method Post -Headers $attHeaders -Body $voteBody -ContentType "application/json"
$votedPoll = $voteRes.data
Write-Host "-> Vote Recorded!" -ForegroundColor Green
Write-Host "   * Total Votes: $($votedPoll.totalVotes)" -ForegroundColor Cyan
Write-Host "   * Has Voted:   $($votedPoll.hasVoted)"
Write-Host "   * Breakdown:"
foreach ($opt in $votedPoll.options) {
    Write-Host "     - $($opt.optionText): $($opt.voteCount) votes ($($opt.percentage)%)" -ForegroundColor Gray
}

# 8. Organizer Closes Poll
Write-Host "`n[8] Organizer closing poll #$($poll.id)..." -ForegroundColor Yellow
$closeBody = @{ status = "CLOSED" } | ConvertTo-Json
$closeRes = Invoke-RestMethod -Uri "$baseUrl/api/polls/$($poll.id)/status" -Method Put -Headers $orgHeaders -Body $closeBody -ContentType "application/json"
Write-Host "-> Poll status updated to: $($closeRes.data.status)" -ForegroundColor Green

# 9. Verify Public Read-Only Feeds
Write-Host "`n[9] Fetching public event Q&A and Poll feeds..." -ForegroundColor Yellow
$qaFeed = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/qa" -Method Get
Write-Host "-> Total Q&A questions retrieved: $($qaFeed.data.Count)" -ForegroundColor Green

$pollsFeed = Invoke-RestMethod -Uri "$baseUrl/api/events/$($targetEvent.id)/polls" -Method Get
Write-Host "-> Total Live Polls retrieved:     $($pollsFeed.data.Count)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  PHASE 15 VERIFICATION COMPLETED SUCCESSFULLY!           " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
