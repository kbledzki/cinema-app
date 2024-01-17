package com.cinema.screenings.domain;

import com.cinema.screenings.domain.exceptions.ScreeningDateOutOfRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScreeningDatePolicy {

    private final Clock clock;

    public void checkScreeningDate(LocalDateTime screeningDate) {
        long daysDifference = Duration
                .between(LocalDateTime.now(clock), screeningDate)
                .abs()
                .toDays();
        boolean isScreeningDateOutOfRange = daysDifference < 7 || daysDifference > 21;
        if (isScreeningDateOutOfRange) {
            throw new ScreeningDateOutOfRangeException();
        }
    }
}
