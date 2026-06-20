package com.ssafy.layover.faq;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.faq.dto.FaqResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getFaqs() {
        return ResponseEntity.ok(ApiResponse.success(faqService.getFaqs()));
    }
}
