# Docker 통합 실행

Vue 프론트엔드, Spring Boot 백엔드, MySQL을 한 번에 실행합니다.

두 저장소는 아래처럼 같은 폴더에 있어야 합니다.

```text
C:\Users\사용자\
├─ Layover\
└─ Layover_Backend\
```

## 한 번 클릭해서 실행

`Layover_Backend\start-layover.cmd`를 더블클릭합니다.

스크립트는 다음 작업을 자동으로 수행합니다.

1. Docker Desktop이 꺼져 있으면 실행하고 준비될 때까지 대기
2. `.env.docker`가 없으면 예제 파일로 생성
3. 프론트엔드, 백엔드, MySQL 이미지 빌드 및 실행
4. 서비스가 준비되면 `http://localhost:5173`을 브라우저로 열기

종료할 때는 `stop-layover.cmd`를 더블클릭합니다. 종료해도 DB와 업로드 볼륨은 보존됩니다.

## 주소

- 통합 웹 화면: `http://localhost:5173`
- 백엔드 직접 접근: `http://localhost:8080`
- MySQL 호스트 접근: `localhost:3307`
- DB 이름: `daejeon_layover`
- DB 사용자/비밀번호: `ssafy` / `ssafy`

프론트엔드 Nginx가 `/api`와 `/uploads` 요청을 백엔드 컨테이너로 전달하므로 브라우저에서는 프론트엔드 주소 하나만 사용합니다.

## 외부 API 설정

외부 API 없이도 서비스와 기본 화면은 실행됩니다. 카카오 지도, 메일 인증, 관광지 동기화 등 실제 연동 기능을 사용하려면 `.env.docker`에 해당 값을 입력합니다.

```dotenv
VITE_KAKAO_JS_KEY=카카오_JavaScript_키
KAKAO_REST_API_KEY=카카오_REST_API_키
KAKAO_CLIENT_ID=카카오_REST_API_키
KAKAO_CLIENT_SECRET=카카오_클라이언트_시크릿
KAKAO_ADMIN_KEY=카카오_관리자_키
TOUR_API_KEY=관광공사_디코딩_키
PLACE_SYNC_ON_STARTUP=true
MAIL_USERNAME=메일주소
MAIL_PASSWORD=앱_비밀번호
```

프론트엔드의 기존 `.env`에 `VITE_KAKAO_JS_KEY`가 있으면 실행 스크립트가 해당 값을 자동으로 사용합니다. 비밀키가 들어간 `.env.docker`는 Git에 커밋하지 않습니다.

`TOUR_API_KEY` 없이 `PLACE_SYNC_ON_STARTUP=true`만 설정하면 관광 API 인증 오류가 발생합니다. 두 값을 함께 설정해야 최초 빈 DB에 관광지가 자동으로 수집됩니다.

## 터미널에서 실행

```powershell
docker compose --env-file .env.docker up --build -d
docker compose --env-file .env.docker logs -f
docker compose --env-file .env.docker down
```

DB와 업로드 파일까지 완전히 삭제하려는 경우에만 다음 명령을 사용합니다.

```powershell
docker compose --env-file .env.docker down -v
```

`down -v`는 Docker 안의 Layover 로컬 DB와 업로드 파일을 삭제하며 복구할 수 없습니다.
