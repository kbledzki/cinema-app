package com.cinema.screenings.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "seats")
@Getter
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rowNumber;

    private int number;

    @Enumerated(value = EnumType.STRING)
    private SeatStatus status;

    protected Seat() {
    }

    public Seat(int rowNumber, int number, SeatStatus status) {
        this.rowNumber = rowNumber;
        this.number = number;
        this.status = status;
    }

    public boolean hasId(Long id) {
        return this.id.equals(id);
    }

    public void take() {
        this.status = SeatStatus.TAKEN;
    }

    public void free() {
        this.status = SeatStatus.FREE;
    }
}