# 🤖 AI Code Review Bot (GitLab & SVN)
> **"인터넷이 안 되는 폐쇄망에서도 OK! 보안 걱정 없는 AI 코드 리뷰어"**

이 프로젝트는 외부 인터넷 접속이 제한된 폐쇄망(Closed Network) 또는 온프레미스(On-Premise) 환경을 위해 개발된 AI 자동 코드 리뷰 시스템입니다.  
로컬 LLM인 Ollama를 활용하여, 소스 코드를 외부로 유출하지 않고도 안전하게 코드 리뷰를 자동화할 수 있습니다.  

---

### 🌟 Key Features
- 🔒 Security First: 외부 인터넷 연결 없이 로컬에서 완벽하게 동작합니다.
- 🔗 Multi-VCS Support: GitLab Merge Request와 SVN Commit을 모두 지원합니다.
- ⚡ Easy Setup: Docker 이미지 하나로 즉시 실행 가능하며, 복잡한 설정은 환경 변수로 제어합니다.

---

### 🚀 Quick Start

Docker Hub에 등록된 이미지를 사용하여 즉시 서비스를 실행할 수 있습니다.  
> 👉 [Docker Hub: chanbeen/internal-code-review-bot](https://hub.docker.com/repository/docker/chanbeen/internal-code-review-bot/general)

**실행을 위한 `docker-compose.yml` 예시**  
가장 간단한 실행 방법입니다. `environment` 데이터를 다음 섹션에 맞춰 수정 해 주세요.  

```yaml
services:
  review-bot:
    image: chanbeen/internal-code-review-bot:latest
    container_name: review-bot
    ports:
      # 리뷰 봇 실행 포트
      - "8080:8080"
    environment:
      # 봇 서버 설정 (본 서버)
      - SERVER_BASE_URL=http://your-server-ip:8080

      # 연동할 GitLab / SVN / Ollama 정보
      - GITLAB_BASE_URL=http://your-gitlab-ip
      - GITLAB_PRIVATE_TOKEN=your-access-token
      - SVN_BASE_URL=svn://your-svn-ip:3690
      - SVN_KEYWORD=[review]
      - OLLAMA_BASE_URL=http://your-ollama-ip:11434
      - OLLAMA_MODEL=llama3

    # SVN 기반 리뷰 데이터를 저장할 공간
    volumes:
      - ./reviews:/app/reviews
```

```bash
docker-compose up -d
```

---

## ⚙️ Configuration

> application.yml을 수정할 필요 없이, 아래 환경 변수만으로 모든 설정을 제어할 수 있습니다.

|        환경 변수         | 기본값 | 설명 |
|:--------------------:|:---|:----|
|   `SERVER_BASE_URL`    | http://localhost:8080 | 리뷰 봇 서버의 기본 주소
   |   `GITLAB_BASE_URL`    | http://localhost:8929 | 연동할 GitLab 서버 주소
 | `GITLAB_PRIVATE_TOKEN` | - | GitLab API 호출을 위한 Access Token
     |     `SVN_BASE_URL`     | svn://localhost:3690 | 연동할 SVN 서버 주소
     |     `SVN_KEYWORD`      | [review] | SVN 커밋 시 리뷰를 트리거할 키워드
   |   `OLLAMA_BASE_URL`    | http://localhost:11434 | Ollama 서버 주소
     |     `OLLAMA_MODEL`     | llama3 | "사용할 LLM 모델명 (예: llama3 |  qwen2 등) 모델은 사용자가 직접 설치 해 주어야 합니다."
    |    `REVIEW_PROMPT`     | (기본 프롬프트) | AI에게 전달할 시스템 프롬프트 (커스텀 가능)


---

### 📂 SVN Hook Setup (Manual)

SVN 서버를 직접 운영 중이라면, 아래 스크립트를 **SVN 서버의 hooks 폴더에 넣어주세요.**  
(이 스크립트는 봇 서버로 알림만 보낼 뿐, 별도의 로직 수정이 필요 없는 범용 버전입니다.)
- **Linux**: ./docker/svn-hooks/post-commit (복사 후 chmod +x 실행 필수)
- **Windows**: ./docker/svn-hooks/windows/ 폴더 내의 .bat 및 .ps1 파일 모두 복사

---

### 🧪 Test Environment (All-in-One)

> 개발이나 테스트를 위해 GitLab + SVN + Ollama + Review Bot을 한 번에 띄우고 싶다면 이 방법을 사용하세요.  

#### 1. Docker 실행
> docker 폴더로 이동하여 docker-compose를 실행합니다.

#### 2. 초기 설정
> 인프라가 구동된 후, 다음 두 가지만 챙겨주세요.
1. **AI 모델 다운로드** (`docker exec -it ollama-server ollama pull llama3`)
2. **GitLab 토큰 설정**
   1. http://localhost:8929 접속 (root / Password1234!)
   2. User Settings > Access Tokens에서 토큰 발급 
   3. `docker/.env` 파일을 생성하고 `GITLAB_TOKEN=발급받은토큰` 입력 후 봇 재시작 (`docker-compose restart review-bot`)

#### 3. Webhook 등록
> GitLab: 리뷰 대상 프로젝트 Settings > Webhooks > http://review-bot:8080/webhook/gitlab 등록 (Merge Request)
> SVN: 이미 자동으로 설정되어 있습니다. svn://localhost:3690/example-svn에 [review] 키워드로 커밋해보세요.

---

### 📝 License
This project is licensed under the MIT License.

### ☁️ 구조도
<img width="1659" height="463" alt="Image" src="https://github.com/user-attachments/assets/6037e196-e0f8-4f31-8a6f-851d6aa56275" />