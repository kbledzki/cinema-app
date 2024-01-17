package com.cinema.halls.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Entity
@Getter
@ToString(exclude = "seats")
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "hall_id")
    private List<HallSeat> seats;

    protected Hall() {}

    public Hall(List<HallSeat> seats) {
        this.seats = seats;
    }
}