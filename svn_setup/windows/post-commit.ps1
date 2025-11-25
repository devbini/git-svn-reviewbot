param (
    [string]$REPOS,
    [long]$REV
)

# ----------------------------------------------------
# 1. 인코딩 설정 및 로그 파일 준비
# ----------------------------------------------------
$LOG_FILE = "$REPOS\hooks\_hook_log.txt"
$TIMESTAMP = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
[Console]::OutputEncoding = [System.Text.Encoding]::Default

# UTF-8로 강제 저장
function Write-Log {
    param ([string]$Message)
    "[$TIMESTAMP] $Message" | Out-File -FilePath $LOG_FILE -Append -Encoding utf8
}

Write-Log "-----------------"
Write-Log "훅 실행됨 (REPOS: $REPOS, REV: $REV)"

# ----------------------------------------------------
# 2. svnlook 경로 및 실행
# ----------------------------------------------------
$SVNLOOK_PATH = "C:\Program Files\TortoiseSVN\bin\svnlook.exe"
if (-not (Test-Path $SVNLOOK_PATH)) {
    $SVNLOOK_PATH = "C:\Program Files (x86)\TortoiseSVN\bin\svnlook.exe"
}
Write-Log "svnlook 경로: $SVNLOOK_PATH"

try {
    $AUTHOR = (& $SVNLOOK_PATH author -r $REV $REPOS | Out-String).Trim()
    $LOG_MSG = (& $SVNLOOK_PATH log -r $REV $REPOS | Out-String).Trim()
    $DIFF_CONTENT = (& $SVNLOOK_PATH diff -r $REV $REPOS | Out-String).Trim()

    Write-Log "작성자: $AUTHOR"
    Write-Log "로그 메시지: $LOG_MSG"

    # ----------------------------------------------------
    # 3. [review] 키워드 검사
    # ----------------------------------------------------
    if ($LOG_MSG -notlike "*[review]*") {
        Write-Log "[review] 키워드 없음. 종료."
        exit 0
    }
    Write-Log "[review] 키워드 발견. API 호출 준비."

    # ----------------------------------------------------
    # 4. JSON 페이로드 생성
    # ----------------------------------------------------
    $payload = @{
        revision = $REV
        author = $AUTHOR
        logMessage = $LOG_MSG
        diffContent = $DIFF_CONTENT
    } | ConvertTo-Json -Compress

    # ----------------------------------------------------
    # 5. 리뷰 API 호출
    # ----------------------------------------------------
    $apiUrl = "http://localhost:8080/webhook/svn"

    Invoke-RestMethod -Uri $apiUrl -Method Post -Body $payload -ContentType "application/json; charset=utf-8"

    Write-Log "API 호출 성공."

} catch {
    # ----------------------------------------------------
    # 6. 오류 처리
    # ----------------------------------------------------
    $errorMsg = $_ | Out-String
    Write-Log "!!! 스크립트 오류 발생 !!!"
    Write-Log $errorMsg
}