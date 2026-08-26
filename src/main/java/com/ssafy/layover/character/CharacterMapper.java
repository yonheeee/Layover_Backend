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
     * 마스터 데이터라 거의 바뀌지 않으므로 서비스 계층에서 캐시한다.
     */
    List<Character> findDrawPool();

    /** 지금 활성인 테마의 카드들 */
    List<Character> findByThemes(@Param("themes") List<String> themes);

    /** 내가 보유한 캐릭터를 code 단위로 집계 */
    List<OwnedCharacterResponse> findOwnedByUserId(@Param("userId") String userId);

    void insertUserCharacter(UserCharacter userCharacter);
}
