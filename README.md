# Git-SVN Code Review Bot

이 프로젝트는 폐쇄망 환경이나 레거시 SVN 환경에서 AI 기반의 자동 코드 리뷰를 제공하기 위해 개발되었습니다.
GitLab과 SVN의 변경 사항을 감지하고, 로컬에 구축된 LLM(Ollama)을 사용하여 코드 리뷰를 수행합니다.

## 배경

보안상의 이유로 외부 인터넷 접속이 제한되거나, 클라우드 기반의 AI 코딩 어시스턴트(GitHub Copilot 등)를 사용할 수 없는 환경을 위한 솔루션입니다.
온프레미스 환경에서 동작하는 GitLab, SVN 서버와 연동되며, 로컬 LLM을 활용하여 데이터 유출 걱정 없이 코드 리뷰를 받을 수 있습니다.

## 요구 환경

*   **OS:** Windows (권장), macOS, Linux
*   **Java:** JDK 17 이상
*   **Docker:** Docker Desktop (GitLab, SVN, Ollama 구동용)
*   **SVN Client:** TortoiseSVN (Windows 환경 테스트용)

## 사용법

### 1. 인프라 구축 (Docker)

프로젝트 루트의 `docker` 디렉토리에서 다음 명령어를 실행하여 GitLab, SVN, Ollama 서버를 실행합니다.
```
bash cd docker docker-compose up -d
```

### 2. 초기 설정

컨테이너가 실행된 후, 다음 초기화 작업을 수행해야 합니다.

**2-1. SVN 저장소 생성**
```
bash docker exec -it svn-server svnadmin create my-repo
``` 

**2-2. Ollama 모델 다운로드**

bash docker exec -it ollama-server ollama pull llama3.2```

**2-3. GitLab 토큰 발급**
1. 브라우저에서 `http://localhost:8929` 접속
2. 로그인 (ID: `root`, PW: `Password1234!`)
3. 우측 상단 사용자 메뉴 -> **Edit profile** -> **Access Tokens**
4. `api`, `read_repository`, `write_repository` 권한을 체크하여 토큰 발급 후 복사

### 3. 애플리케이션 설정

`src/main/resources/application.yml` 파일을 열고 환경에 맞게 설정을 수정합니다.
(`application.yml`이 없다면 `application.yml.example`을 복사하여 생성하세요.)
```

yaml gitlab: token: "여기에_발급받은_토큰_입력"
review: storage: path: C:/code-review # 리뷰 파일이 저장될 로컬 경로 (폴더 미리 생성 필요) prompt: instruction: | 다음 코드 변경 사항에 대한 코드 리뷰를 한국어로 작성해 주세요. ... (원하는 리뷰 지침 작성)
``` 

### 4. 실행

IntelliJ IDEA 또는 터미널에서 Spring Boot 애플리케이션을 실행합니다.
```

bash ./gradlew bootRun
```

---
### 테스트 접속 정보

*   **GitLab:** `http://localhost:8929` (root / Password1234!)
*   **SVN:** `svn://localhost:3690/my-repo`
*   **Ollama:** `http://localhost:11434`
```
