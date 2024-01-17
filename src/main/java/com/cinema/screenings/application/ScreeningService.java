package com.cinema.screenings.application;

import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmRepository;
import com.cinema.films.domain.exceptions.FilmNotFoundException;
import com.cinema.halls.domain.Hall;
import com.cinema.halls.domain.HallRepository;
import com.cinema.halls.domain.exceptions.HallNotFoundException;
import com.cinema.screenings.application.dto.CreateScreeningDto;
import com.cinema.screenings.application.dto.GetScreeningsDto;
import com.cinema.screenings.application.dto.ScreeningDto;
import com.cinema.screenings.application.dto.ScreeningSeatDto;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningFactory;
import com.cinema.screenings.domain.ScreeningRepository;
import com.cinema.screenings.domain.exceptions.ScreeningNotFoundException;
import com.cinema.screenings.infrastructure.ScreeningMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Comparator.comparing;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {

    private final ScreeningFactory screeningFactory;
    private final ScreeningRepository screeningRepository;
    private final ScreeningMapper screeningMapper;
    private final FilmRepository filmRepository;
    private final HallRepository hallRepository;

    @Transactional
    public void createScreening(CreateScreeningDto dto) {
        log.info("Dto:{}", dto);
        Film film = filmRepository
                .getById(dto.filmId())
                .orElseThrow(FilmNotFoundException::new);
        log.info("Gotten film:{}", film);
        Hall hall = hallRepository
                .getById(dto.hallId())
                .orElseThrow(HallNotFoundException::new);
        log.info("Gotten hall:{}", hall);
        Screening screening = screeningFactory.createScreening(
                dto.date(),
                film,
                hall
        );
        Screening addedScreening = screeningRepository.add(screening);
        log.info("Screening added:{}", addedScreening);
    }

    public void deleteScreening(Long id) {
        log.info("Screening id:{}", id);
        Screening screening = screeningRepository
                .getById(id)
                .orElseThrow(ScreeningNotFoundException::new);
        screeningRepository.delete(screening);
    }

    public List<ScreeningDto> getScreenings(GetScreeningsDto dto) {
        log.info("Dto:{}", dto);
        return screeningRepository
                .getAll(dto)
                .stream()
                .sorted(comparing(Screening::getDate))
                .map(screeningMapper::mapScreeningToDto)
                .toList();
    }

    public List<ScreeningSeatDto> getSeatsByScreeningId(Long screeningId) {
        log.info("Screening id:{}", screeningId);
        return screeningRepository
                .getById(screeningId)
                .orElseThrow(ScreeningNotFoundException::new)
                .getSeats()
                .stream()
                .map(screeningMapper::mapScreeningSeatToDto)
                .toList();
    }
}
