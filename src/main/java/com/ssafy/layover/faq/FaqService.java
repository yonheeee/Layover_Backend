package com.ssafy.layover.faq;

import com.ssafy.layover.faq.dto.FaqResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqMapper faqMapper;

    public List<FaqResponse> getFaqs() {
        return faqMapper.findAll();
    }
}
