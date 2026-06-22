package com.ssafy.layover.train;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GetMapping
    public ResponseEntity<List<TrainResponse>> getTrains(
            @RequestParam String station,
            @RequestParam(required = false) String date
    ) {
        String resolvedDate = (date != null && !date.isBlank())
                ? date
                : LocalDate.now().format(DATE_FMT);

        return ResponseEntity.ok(trainService.getTrains(station, resolvedDate));
    }
}
