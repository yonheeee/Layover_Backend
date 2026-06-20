package com.ssafy.layover.faq;

import com.ssafy.layover.faq.dto.FaqResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FaqMapper {

    List<FaqResponse> findAll();
}
