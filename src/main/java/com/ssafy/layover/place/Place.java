package com.ssafy.layover.place;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Place {

    private String id;
    private String name;
    private String category;
    private String originalCategoryCode;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String tourApiId;
    private String contentTypeId;
    private String operatingHours;
    private String restDate;
    private String infoCenter;
    private String parking;
    private String useFee;
    private String reservation;
    private String imageUrl;
    private Boolean active;          // is_active 컬럼
    private String description;
    private String kakaoPlaceId;
    private String kakaoPlaceUrl;
    private String kakaoPhone;
    private String roadAddress;
    private String introRaw;
    private String detailInfoRaw;
    private LocalDateTime deletedAt;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;

    /**
     * 현재 영업 여부. 운영시간/휴무일 정보를 해석할 수 없으면 UNKNOWN이다.
     */
    public PlaceOpenStatus.Status getOpenStatus() {
        return PlaceOpenStatus.evaluate(operatingHours, restDate, LocalDateTime.now());
    }

    /**
     * 화면 표시용 영업 여부.
     *
     * <p>정보를 알 수 없는 경우(UNKNOWN)에는 true를 반환한다. 정보가 없다는 이유로
     * 후보에서 빼버리면 TourAPI에 운영시간이 없는 장소가 전부 사라지기 때문이다.
     * 확실히 닫힌 곳만 걸러내고, 불확실한 곳은 화면에서 "정보 확인 필요"로 안내한다.
     */
    public boolean isCurrentlyOpen() {
        return getOpenStatus() != PlaceOpenStatus.Status.CLOSED;
    }

    /** 코스 후보에서 제외해야 하는지 여부. 확실히 닫힌 경우에만 true. */
    public boolean isDefinitelyClosed() {
        return getOpenStatus() == PlaceOpenStatus.Status.CLOSED;
    }
}
