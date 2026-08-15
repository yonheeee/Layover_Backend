# Layover Cleanup Progress

## 1. Dead code and mock data cleanup

Status: completed with one explicit external API exception.

Completed:
- Removed frontend mock modules under `src/mocks`.
- Removed frontend mock imports and mock-only user/mypage calls.
- Replaced the home popular course section with real community post API data.
- Connected community SHARE posts to saved courses through `courseId`.
- Added backend popular shared-course API based on `SHARE` posts with connected courses, sorted by `like_count`.
- Replaced the character collection page with `/api/characters` and `/api/user/me`.
- Replaced the place page hero banners with `/api/places` data.
- Updated stamp reward popup behavior to prefer backend `newCharacter` response.
- Removed unused frontend user-side placeholder types.

Verification:
- Frontend mock keyword search returned no remaining explicit mock/dummy/sample/hardcoded frontend data.
- Vue SFC parser passed for the modified pages/components.
- Full frontend build is still blocked by the existing local environment issue where Vite exits after module transform without an error body.
- TypeScript check is blocked by missing local `@types/node`.
- Backend compile is blocked by the existing Maven wrapper startup error.

Exception kept for now:
- `KorailApiClient` still calls the Korail public sample endpoint under `openapis.korail.com/samples/public/...`.
- This is intentionally left as-is until the real Korail production endpoint/key is confirmed.
