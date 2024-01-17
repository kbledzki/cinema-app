package com.cinema.screenings.domain;

import com.cinema.films.domain.Film;
import com.cinema.halls.domain.Hall;
import com.cinema.screenings.domain.exceptions.ScreeningsCollisionsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScreeningFactory {

    private final ScreeningDatePolicy screeningDatePolicy;
    private final ScreeningRepository screeningRepository;

    public Screening createScreening(LocalDateTime date, Film film, Hall hall) {
        screeningDatePolicy.checkScreeningDate(date);
        LocalDateTime endDate = date.plusMinutes(film.getDurationInMinutes());
        List<Screening> collisions = screeningRepository.getScreeningCollisions(date, endDate, hall.getId());
        if (!collisions.isEmpty()) {
            throw new ScreeningsCollisionsException();
        }
        Screening screening = new Screening(date, endDate, film, hall);
        List<ScreeningSeat> screeningSeats = hall
                .getSeats()
                .stream()
                .map(hallSeat -> new ScreeningSeat(true, hallSeat, screening))
                .toList();
        screening.assignSeats(screeningSeats);
        return screening;
    }
}
