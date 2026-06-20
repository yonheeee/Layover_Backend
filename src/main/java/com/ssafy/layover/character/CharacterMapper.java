package com.ssafy.layover.character;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CharacterMapper {

    List<Character> findAll();

    List<Character> findByUserId(@Param("userId") String userId);

    Character findByRequiredStamps(@Param("count") int count);

    void insertUserCharacter(UserCharacter userCharacter);

    boolean existsUserCharacter(@Param("userId") String userId, @Param("characterId") String characterId);
}
