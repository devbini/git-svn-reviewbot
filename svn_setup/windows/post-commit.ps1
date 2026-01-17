param (
    [string]$REPOS,
    [long]$REV
)

# 2. svnlook 경로
$SVNLOOK_PATH = "C:\Program Files\TortoiseSVN\bin\svnlook.exe"
if (-not (Test-Path $SVNLOOK_PATH)) {
    $SVNLOOK_PATH = "C:\Program Files (x86)\TortoiseSVN\bin\svnlook.exe"
}

# 2. 정보 추출
$AUTHOR = (& $SVNLOOK_PATH author -r $REV $REPOS | Out-String).Trim()
$LOG_MSG = (& $SVNLOOK_PATH log -r $REV $REPOS | Out-String).Trim()

# 3. 페이로드 구성
$payload = @{
    repo = $REPOS
    revision = $REV
    author = $AUTHOR
    message = $LOG_MSG
} | ConvertTo-Json -Compress

# 4. 리뷰 봇 서버로 전송 (서버 IP/포트를 환경에 맞게 수정 해 주세요.)
$apiUrl = "http://localhost:8080/webhook/svn"
Invoke-RestMethod -Uri $apiUrl -Method Post -Body $payload -ContentType "application/json; charset=utf-8"