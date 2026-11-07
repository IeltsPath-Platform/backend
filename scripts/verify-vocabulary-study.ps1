<#
.SYNOPSIS
Exercises vocabulary study through a running gateway using a fresh learner account.
.EXAMPLE
./scripts/verify-vocabulary-study.ps1 -BaseUrl http://localhost:18080 -Credential (Get-Credential)
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Parameter(Mandatory = $true)][PSCredential]$Credential,
    [Guid]$TopicId = [Guid]::Empty
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)
$studyPrefix = '/api/learning-support/vocabulary-study'
$script:accessToken = $null
$script:checks = 0

function Assert-Study([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
    $script:checks++
}

function Invoke-StudyApi {
    param([string]$Method, [string]$Path, $Body = $null, [int[]]$Expected = @(200), [switch]$Anonymous)
    $request = New-Object System.Net.Http.HttpRequestMessage(
        (New-Object System.Net.Http.HttpMethod($Method)), ($BaseUrl.TrimEnd('/') + $Path))
    if (-not $Anonymous -and $script:accessToken) {
        $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $script:accessToken)
    }
    $request.Headers.Add('X-Correlation-Id', ('vocabulary-study-check-' + [Guid]::NewGuid()))
    if ($null -ne $Body) {
        $request.Content = New-Object System.Net.Http.StringContent(
            ($Body | ConvertTo-Json -Depth 40 -Compress), [Text.Encoding]::UTF8, 'application/json')
    }
    try {
        # Stay below the gateway's default rate limit during this functional check.
        Start-Sleep -Milliseconds 60
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $status = [int]$response.StatusCode
            $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            # Do not include authentication payloads or response bodies in error output.
            if ($Expected -notcontains $status) { throw "${Method} ${Path}: HTTP $status; expected $($Expected -join '/')." }
            $script:checks++
            $data = if ([string]::IsNullOrWhiteSpace($raw)) { $null } else { $raw | ConvertFrom-Json }
            return [PSCustomObject]@{ Status = $status; Data = $data }
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}

function Get-CorrectAnswer($Step, $Words) {
    switch ($Step.type) {
        'WORD' { return @{ type = 'WORD'; acknowledged = $true } }
        'CHOICE' {
            $word = @($Words | Where-Object { $_.lemma -eq $Step.prompt })[0]
            $option = @($Step.options | Where-Object { $_.text -eq $word.vietnameseMeaning })[0]
            if (-not $option) { throw 'Cannot solve CHOICE from the public word snapshot.' }
            return @{ type = 'CHOICE'; optionId = $option.optionId }
        }
        'MATCH' {
            $pairs = @($Step.left | ForEach-Object {
                $left = $_
                $word = @($Words | Where-Object { $_.lemma -eq $left.text })[0]
                $right = @($Step.right | Where-Object { $_.text -eq $word.vietnameseMeaning })[0]
                if (-not $right) { throw 'Cannot solve MATCH from the public word snapshot.' }
                @{ leftOptionId = $left.optionId; rightOptionId = $right.optionId }
            })
            return @{ type = 'MATCH'; pairs = $pairs }
        }
        'FILL' {
            $word = @($Words | Where-Object { $_.vietnameseMeaning -eq $Step.prompt })[0]
            if (-not $word -or $Step.inputMode -ne 'TEXT') { throw 'Expected a solvable FILL/TEXT step.' }
            return @{ type = 'FILL_TEXT'; text = $word.lemma }
        }
        default { throw "Unexpected exercise type: $($Step.type)." }
    }
}

function Complete-StudyAttempt($Attempt, [switch]$OneMistake) {
    $words = @($Attempt.steps | Where-Object { $_.type -eq 'WORD' } | ForEach-Object { $_.word })
    Assert-Study (@($words.lemma | Select-Object -Unique).Count -eq $words.Count) 'Use demo units with distinct lemmas for this smoke check.'
    $madeMistake = $false
    foreach ($step in $Attempt.steps) {
        $path = "$studyPrefix/attempts/$($Attempt.attemptId)/steps/$($step.stepId)/answers"
        $answer = Get-CorrectAnswer $step $words
        if ($OneMistake -and -not $madeMistake -and $step.type -eq 'CHOICE') {
            $wrong = @($step.options | Where-Object { $_.optionId -ne $answer.optionId })[0]
            $body = @{ requestId = [Guid]::NewGuid().ToString(); answer = @{ type = 'CHOICE'; optionId = $wrong.optionId } }
            $result = (Invoke-StudyApi POST $path $body).Data
            Assert-Study ($result.outcome -eq 'INCORRECT' -and $result.heartLost) 'First incorrect choice must lose one heart.'
            Assert-Study ($result.hearts.hearts -eq 4) 'A standard learner must have four hearts after one mistake.'
            $retry = (Invoke-StudyApi POST $path $body).Data
            Assert-Study ($retry.submissionId -eq $result.submissionId) 'Answer retry must return the original submission.'
            Assert-Study ($retry.hearts.hearts -eq $result.hearts.hearts) 'Answer retry must not spend another heart.'
            $madeMistake = $true
        }
        $result = (Invoke-StudyApi POST $path @{ requestId = [Guid]::NewGuid().ToString(); answer = $answer }).Data
        Assert-Study ($result.stepCompleted -and $result.outcome -ne 'INCORRECT') 'Correct answer must complete the current step.'
        if ($Attempt.mode -eq 'REPLAY') {
            Assert-Study (-not $result.heartLost -and $null -eq $result.hearts) 'Replay must not use hearts.'
        }
    }
    if ($OneMistake) { Assert-Study $madeMistake 'The demo unit must contain a CHOICE step.' }
    $path = "$studyPrefix/attempts/$($Attempt.attemptId)/complete"
    $body = @{ requestId = [Guid]::NewGuid().ToString() }
    $completion = (Invoke-StudyApi POST $path $body).Data
    $retry = (Invoke-StudyApi POST $path $body).Data
    Assert-Study (($retry | ConvertTo-Json -Depth 40 -Compress) -eq ($completion | ConvertTo-Json -Depth 40 -Compress)) 'Completion retry must return the entire saved completion.'
    return $completion
}

try {
    Invoke-StudyApi GET "$studyPrefix/me/summary" -Expected @(401) -Anonymous | Out-Null
    $login = (Invoke-StudyApi POST '/auth/login' @{
        username = $Credential.UserName; password = $Credential.GetNetworkCredential().Password
    } -Anonymous).Data
    $script:accessToken = $login.accessToken
    Assert-Study (-not [string]::IsNullOrWhiteSpace($script:accessToken)) 'Login did not return an access token.'

    $summary = (Invoke-StudyApi GET "$studyPrefix/me/summary").Data
    Assert-Study ($summary.xp.totalXp -eq 0 -and $summary.xp.level -eq 1 -and $summary.streak.currentDays -eq 0) 'Use a fresh learner with no vocabulary study or flashcard activity.'
    Assert-Study ($summary.hearts.hearts -eq 5 -and $summary.hearts.maxHearts -eq 5) 'Use a standard learner with five full hearts.'
    $topics = (Invoke-StudyApi GET '/api/content/vocabulary/topics' -Anonymous).Data
    $units = $null
    foreach ($topic in $topics) {
        if ($TopicId -ne [Guid]::Empty -and $topic.id -ne $TopicId.ToString()) { continue }
        $candidate = (Invoke-StudyApi GET "/api/content/vocabulary/topics/$($topic.id)/units" -Anonymous).Data
        if ($candidate.totalUnits -ge 2) { $units = $candidate; break }
    }
    if (-not $units) { throw 'No published topic with at least two demo units was found.' }
    $first = $units.items[0].unitId
    $second = $units.items[1].unitId
    Invoke-StudyApi GET "/api/content/vocabulary/units/$first" -Anonymous | Out-Null
    $progress = @((Invoke-StudyApi GET "$studyPrefix/progress?topicIds=$($units.topicId)").Data)[0]
    Assert-Study ($progress.units[0].status -eq 'AVAILABLE' -and $progress.units[1].status -eq 'LOCKED') 'Fresh progress must unlock only the first unit.'
    $locked = (Invoke-StudyApi POST "$studyPrefix/units/$second/attempts" @{
        requestId = [Guid]::NewGuid().ToString(); mode = 'RESUME_OR_START'
    } -Expected @(409)).Data
    Assert-Study ($locked.code -eq 'VOCAB_UNIT_LOCKED') 'Starting a locked unit must return its business code.'

    $attempt = (Invoke-StudyApi POST "$studyPrefix/units/$first/attempts" @{
        requestId = [Guid]::NewGuid().ToString(); mode = 'RESUME_OR_START'
    } -Expected @(201)).Data
    $resume = (Invoke-StudyApi POST "$studyPrefix/units/$first/attempts" @{
        requestId = [Guid]::NewGuid().ToString(); mode = 'RESUME_OR_START'
    }).Data
    foreach ($type in @('WORD', 'CHOICE', 'MATCH', 'FILL')) {
        Assert-Study (@($attempt.steps.type) -contains $type) "The demo attempt must contain $type steps."
    }
    Assert-Study ($resume.attemptId -eq $attempt.attemptId) 'Starting again must resume the saved attempt.'
    $saved = (Invoke-StudyApi GET "$studyPrefix/attempts/$($attempt.attemptId)").Data
    Assert-Study ($saved.steps[0].stepId -eq $attempt.steps[0].stepId) 'GET attempt must retain its snapshot.'
    $completion = Complete-StudyAttempt $attempt -OneMistake
    Assert-Study ($completion.firstCompletion -and $completion.reward.xpEarned -eq 20) 'First completion with a mistake must award 20 XP.'
    Assert-Study ($completion.reward.xpBreakdown.firstCompletion -eq 20 -and $completion.reward.xpBreakdown.perfectBonus -eq 0) 'A mistaken first completion must not receive the perfect bonus.'
    Assert-Study ($completion.unlockedUnitIds -contains $second) 'First completion must unlock the next unit.'
    Assert-Study ($completion.reward.summary.streak.currentDays -eq 1) 'Completion must qualify the vocabulary streak.'
    Assert-Study ($completion.reward.streakIncreased) 'The first qualified activity must increase the streak.'
    $progress = @((Invoke-StudyApi GET "$studyPrefix/progress?topicIds=$($units.topicId)").Data)[0]
    Assert-Study ($progress.units[0].status -eq 'COMPLETED' -and $progress.units[1].status -eq 'AVAILABLE') 'Progress must preserve the completion and unlock the second unit.'

    $replay = (Invoke-StudyApi POST "$studyPrefix/units/$first/attempts" @{
        requestId = [Guid]::NewGuid().ToString(); mode = 'REPLAY'
    } -Expected @(201)).Data
    $replayed = Complete-StudyAttempt $replay
    Assert-Study (-not $replayed.firstCompletion -and $replayed.reward.xpEarned -eq 0) 'Replay must award zero XP.'
    Assert-Study ($replayed.reward.summary.streak.currentDays -eq 1) 'Same-day replay must not increase the streak.'

    $attempt = (Invoke-StudyApi POST "$studyPrefix/units/$second/attempts" @{
        requestId = [Guid]::NewGuid().ToString(); mode = 'RESUME_OR_START'
    } -Expected @(201)).Data
    $perfect = Complete-StudyAttempt $attempt
    Assert-Study ($perfect.reward.xpEarned -eq 30 -and $perfect.accuracyPercent -eq 100) 'Perfect first completion must award 30 XP.'
    Assert-Study ($perfect.reward.xpBreakdown.firstCompletion -eq 20 -and $perfect.reward.xpBreakdown.perfectBonus -eq 10) 'Perfect first completion must include the ten-XP bonus.'
    $summary = (Invoke-StudyApi GET "$studyPrefix/me/summary").Data
    Assert-Study ($summary.xp.totalXp -eq 50 -and $summary.streak.currentDays -eq 1) 'Retries and replay must leave the final total at 50 XP and streak one.'
    Assert-Study ($summary.hearts.hearts -eq 4 -and $summary.xp.level -eq 1) 'Replay and perfect answers must not spend more hearts or cause a premature level-up.'
    [PSCustomObject]@{ Checks = $script:checks; TopicId = $units.topicId; TotalXp = $summary.xp.totalXp; Streak = $summary.streak.currentDays }
} finally {
    $script:accessToken = $null
    $client.Dispose()
}
