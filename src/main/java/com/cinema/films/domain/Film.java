package com.cinema.films.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "films")
@Getter
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private FilmCategory category;

    private int year;

    private int durationInMinutes;

    protected Film() {
    }

    public Film(String title, FilmCategory category, int year, int durationInMinutes) {
        this.title = title;
        this.category = category;
        this.year = year;
        this.durationInMinutes = durationInMinutes;
    }
}
