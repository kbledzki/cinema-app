package com.cinema.screenings.application.rest;

import com.cinema.screenings.application.dto.ScreeningCreateDto;
import com.cinema.screenings.application.dto.ScreeningDto;
import com.cinema.screenings.application.dto.ScreeningQueryDto;
import com.cinema.screenings.application.dto.SeatDto;
import com.cinema.screenings.application.services.ScreeningService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/screenings")
@RequiredArgsConstructor
class ScreeningController {

    private final ScreeningService screeningService;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    @SecurityRequirement(name = "basic")
    void createScreening(
            @RequestBody
            @Valid
            ScreeningCreateDto dto
    ) {
        screeningService.createScreening(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "basic")
    void deleteScreening(@PathVariable Long id) {
        screeningService.delete(id);
    }

    @GetMapping
    List<ScreeningDto> readAllScreeningsBy(@RequestParam(required = false) LocalDate date) {
        var queryDto = ScreeningQueryDto
                .builder()
                .date(date)
                .build();
        return screeningService.readAllScreeningsBy(queryDto);
    }

    @GetMapping("/{id}/seats")
    List<SeatDto> readSeatsByScreeningId(@PathVariable Long id) {
        return screeningService.readSeatsByScreeningId(id);
    }
}
