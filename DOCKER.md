# Docker 로컬 실행

MySQL과 Spring Boot 백엔드를 한 번에 실행합니다.

## 실행

```powershell
docker compose up --build
```

- 백엔드: `http://localhost:8080`
- MySQL(호스트 접속): `localhost:3307`
- DB: `daejeon_layover`
- 로컬 DB 사용자: `ssafy`
- 로컬 DB 비밀번호: `ssafy`

처음 실행할 때 `src/main/resources/db/schema.sql`이 새 Docker 볼륨에 자동 적용됩니다.
이 스키마에는 DROP TABLE 구문이 있지만, 최초 생성되는 Docker 전용 볼륨 안에서만 실행됩니다.
PC에 설치된 기존 MySQL 데이터베이스에는 영향을 주지 않습니다.

MySQL과 백엔드가 준비되면 서버가 관광지 데이터 존재 여부를 확인합니다.
TourAPI 관광지가 0개일 때만 내부 서비스로 최초 동기화를 한 번 실행하며,
이미 데이터가 있으면 외부 API 호출 한도를 보호하기 위해 건너뜁니다.

`POST /api/admin/places/sync`는 외부에 무인증으로 공개하지 않고 기존처럼
ADMIN 권한을 유지합니다.

관광지 동기화에는 `src/main/resources/application-local.properties`의 TourAPI 및
Kakao API 키를 사용합니다. 이 파일은 백엔드 컨테이너에 읽기 전용으로 연결되며
Docker 이미지에는 포함되지 않습니다.

동기화 결과 확인:

```powershell
docker compose logs backend | Select-String "PlaceSyncOnStartup|TourAPI"
```

## 백그라운드 실행

```powershell
docker compose up --build -d
docker compose logs -f backend
```

## 종료

```powershell
docker compose down
```

## DB까지 완전히 초기화

다음 명령은 Docker의 Layover DB와 업로드 파일을 삭제합니다.

```powershell
docker compose down -v
```

## 외부 API 및 S3 설정

`.env.docker`는 Git에 포함되지 않습니다. 외부 API 또는 S3가 필요하면
`.env.docker.example`을 참고하여 현재 `.env.docker`에 값을 추가하세요.

S3를 사용할 경우 최소 설정:

```dotenv
STORAGE_TYPE=s3
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=버킷이름
AWS_S3_PUBLIC_BASE_URL=공개기본URL
AWS_ACCESS_KEY_ID=액세스키
AWS_SECRET_ACCESS_KEY=비밀액세스키
```

비밀키가 들어간 `.env.docker`는 커밋하지 마세요.
