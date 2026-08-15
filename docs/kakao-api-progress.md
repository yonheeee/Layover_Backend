# Kakao API Progress

## 2026-08-15

- Kept both email login and Kakao login flows.
- Added `KakaoRouteApiClient` for route calculations.
- Replaced `CourseService` route calculations from TMap calls to Kakao route APIs.
- Preserved `TMapApiClient` file for compatibility, but `CourseService` no longer depends on it.
- Added `kakao.rest-api-key` as the preferred backend route API key setting.
- Kept `kakao.api.key` as a fallback-compatible legacy setting.

## Route API Mapping

- Walk: Kakao Map REST walk routing.
- Public transit: Kakao Map REST public transit routing, with existing bus estimation as fallback.
- Taxi/car: Kakao Mobility Directions API.

## Verification

- Maven wrapper could not run in this shell because `mvnw.cmd` fails while resolving its internal PowerShell script.
- Local `mvn` is not installed in PATH.
