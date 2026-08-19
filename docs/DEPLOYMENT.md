# Layover 배포 구성

## 구성

- 프론트엔드: Cloudflare Pages (`yonheeee/Layover`)
- 백엔드: Google Cloud Run (`yonheeee/Layover_Backend`)
- 데이터베이스: TiDB Cloud Starter (`daejeon_layover`)
- 이메일 인증/채팅 보조: Upstash Redis
- 게시글 이미지: AWS S3

Cloud Run과 TiDB를 모두 도쿄 리전으로 맞추기 위해 기본 배포 리전은
`asia-northeast1`을 사용합니다. S3는 기존 서울 리전을 유지합니다.

## 배포 순서

1. TiDB에 `resources/db`의 스키마를 적용합니다.
2. Google Cloud 프로젝트를 만들고 결제를 연결합니다.
3. Cloud Run에 백엔드를 최초 1회 배포하고 공개 호출을 허용합니다.
4. Cloud Run 환경 변수와 Secret Manager 비밀값을 등록합니다.
5. Cloudflare Pages를 GitHub 프론트엔드 저장소와 연결합니다.
6. Pages의 `VITE_API_BASE_URL`을 Cloud Run URL로 설정합니다.
7. Cloud Run의 CORS와 카카오 콜백 주소를 Pages URL로 갱신합니다.
8. GitHub Actions와 Google Cloud Workload Identity Federation을 연결합니다.

## TiDB JDBC

TiDB Cloud Starter는 TLS 연결이 필수입니다. Java는 시스템 루트 인증서를
사용할 수 있으므로 별도 CA 파일 없이 다음 형식을 사용합니다.

```text
jdbc:mysql://TIDB_HOST:4000/daejeon_layover?sslMode=VERIFY_IDENTITY&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

사용자 이름과 비밀번호는 저장소에 커밋하지 않고 Cloud Run에 주입합니다.

Cloud Run의 기본 송신 IP는 고정되어 있지 않습니다. 무료 구성을 유지하려면
TiDB의 `Settings > Networking`에서 기본 `Allow_all_public_connections` 규칙을
유지해야 합니다. 대신 TLS `VERIFY_IDENTITY`, 강한 DB 비밀번호, 최소 권한 DB
사용자를 반드시 사용합니다. 특정 IP만 허용하려면 Google Cloud의 고정 송신 IP
구성이 추가로 필요하며 비용이 발생할 수 있습니다.

## Cloud Run GitHub 변수

GitHub 저장소의 `Settings > Secrets and variables > Actions > Variables`에
다음 값을 등록합니다.

```text
GCP_PROJECT_ID
GCP_REGION=asia-northeast1
GCP_WORKLOAD_IDENTITY_PROVIDER
GCP_DEPLOY_SERVICE_ACCOUNT
```

`GCP_PROJECT_ID`가 비어 있으면 CD 작업은 안전하게 건너뜁니다. 최초 Cloud
Run 배포와 Workload Identity 설정을 마치면 `main` 푸시마다 테스트와 Docker
빌드 성공 후 자동 배포됩니다.

## Cloudflare Pages 설정

```text
Production branch: main
Build command: npm run build
Build output directory: dist
Environment variable: VITE_API_BASE_URL=https://CLOUD_RUN_SERVICE_URL
```

프론트엔드 저장소의 `public/_redirects`가 Vue Router의 직접 URL 접근을
`index.html`로 연결합니다.

## 운영 데이터 동기화

Cloud Run 인스턴스 내부의 `@Scheduled` 작업은 운영 프로필에서 비활성화됩니다.
Cloud Scheduler를 구성하기 전에는 관리자 인증을 사용해 장소 동기화 API를
수동 호출합니다. 운영 환경에서는 동기화 API를 공개하지 않습니다.
