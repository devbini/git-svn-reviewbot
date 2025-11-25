# 🤖 AI Code Review Bot (GitLab & SVN)
Local LLM(Ollama)을 활용하여 GitLab Merge Request와 SVN Commit을 자동으로 분석하고 코드 리뷰를 수행하는 봇입니다.  
외부 API를 사용하지 않아 보안이 중요한 내부망 환경에서도 안전하게 동작하며,  
최신 Git 환경과 Legacy SVN 환경을 동시에 지원하는 하이브리드 아키텍처를 갖추고 있습니다.  

---

## 🛠 Tech Stack
- Language: Kotlin (JDK 17+)
- Framework: Spring Boot 3 (WebFlux)
- VCS Libraries:
  - GitLab API (WebClient)
- SVNKit (Subversion)
- AI Engine: Ollama
- Infrastructure: Docker & Docker Compose

---

## 🚀 Getting Started
1. **Prerequisites**
   - Docker & Docker Compose
   - JDK 17+

2. **Infrastructure Setup (Docker)**  
   - GitLab, SVN Server, Ollama를 한 번에 실행하기 위해 docker-compose.yml을 사용합니다.
   - SVN 컨테이너는 실행 시 자동으로 Hook 스크립트를 배포하도록 구성되어 있습니다.

```bash
cd ./docker 
docker-compose up -d
```

3. **Configuration (application.yml)**
   - `src/main/resources/application.yml` 파일에 환경 설정을 입력합니다.

```yaml
server:
  port: 봇 서버 실행 포트
  base-url: 봇 서버 실행 경로

gitlab:
  baseUrl: 대상 Gitlab 서버 경로
  privateToken: GitLab 리뷰 봇 대상 AccessToken (api 권한)

ollama:
  baseUrl: 대상 ollama 대상 경로
  model: 사용 할 모델명

review:
  storage:
    path: SVN의 경우 파일을 저장 할 디렉터리 위치
  prompt:
    instruction: 커스텀 프롬프트 입력 부분
```

4. **Run Application**
   - 시작 후 commit 또는 MergeRequest를 발생시킵니다.

---

## 📖 Usage Guide  
### Case A: GitLab Merge Request
1. GitLab에서 새로운 기능 브랜치를 생성하고 코드를 수정합니다.
2. main 브랜치로 Merge Request를 생성합니다.
3. 봇이 변경 사항(Diff)을 감지하고, 잠시 후 MR 페이지에 AI 리뷰 코멘트가 달립니다.

### Case B: SVN Commit
1. SVN 저장소를 체크아웃 받습니다. (svn://localhost:3690/example-svn)
2. 코드를 수정합니다.
3. 커밋 메시지에 [review] 키워드를 포함하여 커밋합니다. 

봇이 로그에 리뷰 생성 URL을 출력하며,  
브라우저 혹은 C드라이브에서 결과를 확인합니다.

---

## 📂 Project Structure
```
src
├── main
│   ├── kotlin/com/cbkim/code_review
│   │   ├── controller  # Webhook 수신 및 결과 조회 API 정리
│   │   ├── service     # GitLab/SVN 비즈니스 로직
│   │   ├── dto         # 데이터 전송 객체
│   └── resources
│       └── application.yml
docker
├── docker-compose.yml  # 인프라 구성
│   └── docker-data     # gitlab & svn 등 데이터 백업 위치
svn_setup
    ├── linux
    │   └── post-commit # SVN Hook 스크립트
    └── windows
        └── post-commit.bat & ps1 # SVN Hook 스크립트
```