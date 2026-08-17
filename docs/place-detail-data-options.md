# 관광지 상세정보 보강 검토

## 결론

현재 서비스는 TourAPI를 주 데이터로 유지하는 것이 가장 안전하다. Kakao Local API는 장소 기본정보 보강과 카카오맵 상세 페이지 연결에는 유용하지만, REST 응답으로 휴무일, 운영시간, 특이사항을 직접 제공하지 않는다.

따라서 다음 구현 방향을 권장한다.

1. TourAPI `detailIntro2` 응답에서 카테고리별 상세 필드를 더 많이 저장한다.
2. TourAPI `detailInfo2`를 추가 호출해 반복 상세정보를 보강한다.
3. Kakao Local API는 전화번호, 도로명주소, 카카오맵 상세 URL 보강 용도로만 사용한다.
4. 자유 텍스트를 억지로 컬럼 분리하기보다 원문 필드와 핵심 요약 필드를 함께 제공한다.

## 현재 구현 상태

`TourApiService`는 현재 다음 TourAPI를 사용한다.

- `areaBasedList2`: 대전 관광지 목록 수집
- `detailCommon2`: `overview` 설명 수집
- `detailIntro2`: 이용시간 계열 일부 필드 수집
- `detailImage2`: 대표 이미지 보강

현재 부족한 점은 다음과 같다.

- 휴무일 필드를 별도 저장하지 않는다.
- 문의/전화, 주차, 예약, 이용요금, 대표메뉴, 특이사항 성격의 필드를 저장하지 않는다.
- `detailInfo2` 반복정보를 아직 사용하지 않는다.
- Kakao 장소 상세 URL을 저장하지 않는다.

## 대안 1. TourAPI 상세 필드 확장

가장 우선순위가 높다. 공공데이터포털의 한국관광공사 국문 관광정보 서비스는 `detailCommon2`, `detailIntro2`, `detailInfo2`, `detailImage2` API를 제공한다.

권장 저장 필드:

- `description`: `detailCommon2.overview`
- `operatingHours`: contentType별 이용시간 필드
- `restDate`: contentType별 쉬는날/휴무일 필드
- `infoCenter`: 문의 및 안내
- `parking`: 주차 정보
- `useFee`: 이용요금 또는 입장료
- `reservation`: 예약/예매 안내
- `detailInfoRaw`: `detailInfo2` 반복정보 원문 JSON
- `introRaw`: `detailIntro2` 원문 JSON

contentType별 우선 필드:

| contentTypeId | 유형 | 운영시간 후보 | 휴무일 후보 | 추가 후보 |
| --- | --- | --- | --- | --- |
| 12 | 관광지 | `usetime` | `restdate` | `infocenter`, `parking`, `expguide` |
| 14 | 문화시설 | `usetimeculture` | `restdateculture` | `infocenterculture`, `parkingculture`, `usefee` |
| 15 | 축제/행사 | `eventstartdate` ~ `eventenddate` | 없음 또는 원문 | `eventplace`, `playtime`, `usetimefestival` |
| 28 | 레포츠 | `usetimeleports` | `restdateleports` | `infocenterleports`, `parkingleports`, `reservation` |
| 32 | 숙박 | `checkintime` / `checkouttime` | 없음 또는 원문 | `reservationlodging`, `parkinglodging` |
| 38 | 쇼핑 | `opentime` | `restdateshopping` | `infocentershopping`, `parkingshopping` |
| 39 | 음식점 | `opentimefood` | `restdatefood` | `infocenterfood`, `firstmenu`, `treatmenu`, `parkingfood` |

장점:

- 공모전 필수 데이터인 한국관광공사 OpenAPI 활용도가 높아진다.
- 현재 동기화 구조에 자연스럽게 붙일 수 있다.
- 데이터 출처를 심사에서 설명하기 쉽다.

단점:

- contentType별 필드명이 다르다.
- 운영시간/휴무일 텍스트가 자유 형식이라 완전한 영업 여부 계산은 어렵다.

## 대안 2. Kakao Local API 보강

Kakao Local API의 키워드/카테고리 장소 검색은 장소명, 카테고리, 전화번호, 주소, 좌표, 카카오맵 장소 상세 페이지 URL을 제공한다.

활용 가능 필드:

- Kakao 장소 ID
- 전화번호
- 도로명주소
- 카카오 카테고리명
- Kakao Map 장소 상세 URL

활용하기 어려운 항목:

- 휴무일
- 운영시간
- 브레이크타임
- 라스트오더
- 실시간 영업 상태

권장 사용 방식:

- TourAPI 장소명 + 좌표 기준으로 Kakao Local API를 검색한다.
- 가장 가까운 후보를 매칭해 `kakaoPlaceId`, `phone`, `roadAddress`, `kakaoPlaceUrl`만 보강한다.
- 앱 상세 화면에는 `카카오맵에서 최신 정보 확인` 버튼을 제공한다.

장점:

- 사용자가 최신 매장 정보를 확인할 수 있는 외부 상세 페이지로 연결 가능하다.
- 음식점/카페류의 전화번호와 도로명주소 보강에 유용하다.

단점:

- Kakao REST 응답만으로 카카오맵 앱처럼 운영시간을 앱 내부에 표시하기는 어렵다.
- Kakao Map 상세 페이지를 크롤링해 운영시간을 가져오는 방식은 안정성과 약관 측면에서 권장하지 않는다.

## 대안 3. 원문 표시 + 요약 표시 병행

TourAPI 텍스트가 자유 형식이라면 DB 컬럼을 너무 잘게 쪼개기보다 다음 구조가 현실적이다.

- 구조화 가능한 필드: 운영시간, 휴무일, 전화번호, 주차, 요금
- 원문 보존 필드: `introRaw`, `detailInfoRaw`
- 사용자 표시 필드: `detailNotice`

예시 UI:

- 운영시간: TourAPI 원문 그대로 표시
- 휴무일: TourAPI 원문 그대로 표시
- 안내: 주차/요금/예약/대표메뉴 중 있는 정보만 카드로 표시
- 최신 정보: Kakao Map 상세 링크 제공

## 구현 우선순위

1. DB/DTO에 상세정보 필드 추가
   - `rest_date`
   - `info_center`
   - `parking`
   - `use_fee`
   - `reservation`
   - `kakao_place_url`
   - `kakao_phone`
   - `intro_raw`
   - `detail_info_raw`

2. `TourApiService` 확장
   - `detailIntro2`에서 contentType별 휴무일/문의/주차/요금 필드 매핑
   - `detailInfo2` 호출 추가

3. Kakao 보강 배치 추가
   - 장소명 + 좌표로 후보 검색
   - 거리 기준 가장 가까운 후보 매칭
   - 전화번호/상세 URL 저장

4. 상세 화면 표시 개선
   - 운영시간/휴무일/문의/주차/요금/예약 표시
   - 값이 없으면 `정보 확인 필요`로 표시
   - Kakao 상세 URL이 있으면 외부 확인 버튼 제공

## 참고 자료

- Kakao Developers Local API: https://developers.kakao.com/docs/latest/ko/local/dev-guide
- 공공데이터포털 한국관광공사 국문 관광정보 서비스: https://www.data.go.kr/data/15101578/openapi.do
