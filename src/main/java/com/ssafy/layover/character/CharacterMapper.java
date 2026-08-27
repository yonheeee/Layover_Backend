package com.ssafy.layover.character;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CharacterMapper {

    /** 전체 147종. 관리/디버그용 */
    List<Character> findAll();

    Character findById(@Param("id") String id);

    /**
     * 평상시 뽑기 풀. 테마 카드(theme IS NOT NULL)는 제외된다.
     *
     * <p>캐시하지 않는다. 다시 찍을 때마다 144행을 읽는다. 마스터 데이터라
     * 캐시해도 되지만, 그러면 이미지를 추가하고 시드를 다시 돌린 뒤 서버를
     * 재시작해야 반영된다. 도감을 계속 손보는 동안에는 그대로 두는 편이 낫다.
     */
    List<Character> findDrawPool();

    /** 지금 활성인 테마의 카드들 */
    List<Character> findByThemes(@Param("themes") List<String> themes);

    /** 내가 보유한 캐릭터를 code 단위로 집계 */
    List<OwnedCharacterResponse> findOwnedByUserId(@Param("userId") String userId);

    void insertUserCharacter(UserCharacter userCharacter);
}
