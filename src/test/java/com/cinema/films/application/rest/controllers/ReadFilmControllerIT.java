package com.cinema.films.application.rest.controllers;

import com.cinema.BaseIT;
import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmCategory;
import com.cinema.films.domain.FilmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.films.FilmFixture.createFilm;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

class ReadFilmControllerIT extends BaseIT {

    private static final String FILMS_BASE_ENDPOINT = "/films";

    @Autowired
    private FilmRepository filmRepository;

    @Test
    void films_are_read() {
        //given
        Film film = filmRepository.add(createFilm());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(FILMS_BASE_ENDPOINT)
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*]").value(hasSize(1))
                .jsonPath("$[0].title").isEqualTo(film.getTitle())
                .jsonPath("$[0].category").isEqualTo(film.getCategory().name())
                .jsonPath("$[0].year").isEqualTo(film.getYear())
                .jsonPath("$[0].durationInMinutes").isEqualTo(film.getDurationInMinutes());
    }

    @Test
    void films_are_read_by_title() {
        //given
        String title = "Film";
        String otherTitle = "Other Film";
        filmRepository.add(createFilm(title));
        filmRepository.add(createFilm(otherTitle));

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(FILMS_BASE_ENDPOINT)
                        .queryParam("title", title)
                        .build())
                .exchange();

        //then
        spec
                .expectBody()
                .jsonPath("$.*.title").value(everyItem(equalTo(title)));
    }

    @Test
    void films_are_read_by_category() {
        //given
        FilmCategory category = FilmCategory.COMEDY;
        FilmCategory otherCategory = FilmCategory.DRAMA;
        filmRepository.add(createFilm(category));
        filmRepository.add(createFilm(otherCategory));

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(FILMS_BASE_ENDPOINT)
                        .queryParam("category", category)
                        .build())
                .exchange();

        //then
        spec
                .expectBody()
                .jsonPath("$.*.category").value(everyItem(equalTo(category.name())));
    }
}
