package com.cinema.screenings.ui;

import com.cinema.screenings.application.ScreeningService;
import com.cinema.screenings.application.dto.CreateScreeningDto;
import com.cinema.screenings.application.dto.GetScreeningsDto;
import com.cinema.screenings.application.dto.ScreeningDto;
import com.cinema.screenings.application.dto.ScreeningSeatDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
class ScreeningController {

    private final ScreeningService screeningService;

    @PostMapping("admin/screenings")
    @SecurityRequirement(name = "basic")
    ResponseEntity<Object> createScreening(
            @RequestBody
            @Valid
            CreateScreeningDto dto
    ) {
        log.info("Dto:{}", dto);
        screeningService.createScreening(dto);
        ResponseEntity<Object> responseEntity = ResponseEntity.created(URI.create("/screenings")).build();
        log.info("Response entity:{}", responseEntity);
        return responseEntity;
    }

    @DeleteMapping("/admin/screenings/{id}")
    @SecurityRequirement(name = "basic")
    ResponseEntity<Object> deleteScreening(@PathVariable Long id) {
        log.info("Screening id:{}", id);
        screeningService.deleteScreening(id);
        ResponseEntity<Object> responseEntity = ResponseEntity.noContent().build();
        log.info("Response entity:{}", responseEntity);
        return responseEntity;
    }

    @GetMapping("/public/screenings")
    ScreeningsResponse getScreenings(@RequestParam(required = false) LocalDate date) {
        GetScreeningsDto getScreeningsDto = GetScreeningsDto
                .builder()
                .date(date)
                .build();
        log.info("Dto:{}", getScreeningsDto);
        List<ScreeningDto> screenings = screeningService.getScreenings(getScreeningsDto);
        return new ScreeningsResponse(screenings);
    }

    @GetMapping("/public/screenings/{id}/seats")
    List<ScreeningSeatDto> getSeatsByScreeningId(@PathVariable Long id) {
        log.info("Screening id:{}", id);
        return screeningService.getSeatsByScreeningId(id);
    }
}
