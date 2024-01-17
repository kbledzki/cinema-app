package com.cinema.screenings.domain;

import com.cinema.films.domain.Film;
import com.cinema.halls.domain.Hall;
import com.cinema.screenings.domain.exceptions.ScreeningSeatNotFoundException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@ToString(exclude = {"film", "hall", "seats"})
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY)
    private Hall hall;

    @OneToMany(mappedBy = "screening", cascade = {CascadeType.PERSIST})
    private List<ScreeningSeat> seats;

    protected Screening() {}

    public Screening(LocalDateTime date, LocalDateTime endDate, Film film, Hall hall) {
        this.date = date;
        this.endDate = endDate;
        this.film = film;
        this.hall = hall;
    }

    public long timeToScreeningInHours(Clock clock) {
        LocalDateTime currentDate = LocalDateTime.now(clock);
        return Duration
                .between(currentDate, this.date)
                .abs()
                .toHours();
    }

    public void assignSeats(List<ScreeningSeat> seats) {
        this.seats = seats;
    }

    public ScreeningSeat findSeat(Long seatId) {
        return this
                .seats
                .stream()
                .filter(seat -> seat.getId().equals(seatId))
                .findFirst()
                .orElseThrow(ScreeningSeatNotFoundException::new);
    }
}
