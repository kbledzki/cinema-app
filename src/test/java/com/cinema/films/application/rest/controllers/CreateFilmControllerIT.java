package com.cinema.films.application.rest.controllers;

import com.cinema.BaseIT;
import com.cinema.films.application.commands.CreateFilm;
import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmCategory;
import com.cinema.films.domain.FilmRepository;
import com.cinema.films.domain.exceptions.FilmTitleNotUniqueException;
import com.cinema.films.domain.exceptions.FilmYearOutOfRangeException;
import com.cinema.users.application.commands.CreateAdmin;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateAdminHandler;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.films.FilmFixture.createCreateFilmCommand;
import static com.cinema.films.FilmFixture.createFilm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateFilmControllerIT extends BaseIT {

    private static final String FILMS_BASE_ENDPOINT = "/films";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "12345";

    @Autowired
    private CreateUserHandler createUserHandler;

    @Autowired
    private CreateAdminHandler createAdminHandler;

    @Autowired
    private FilmRepository filmRepository;

    @Test
    void film_can_be_created_only_by_admin() {
        //given
        addCommonUser();

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILMS_BASE_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isForbidden();
    }

    @Test
    void film_is_created() {
        //given
        addAdminUser();

        Long id = 1L;
        String title = "Some filmId";
        FilmCategory category = FilmCategory.COMEDY;
        int year = 2023;
        int durationInMinutes = 100;
        CreateFilm command = new CreateFilm(
                title,
                category,
                year,
                durationInMinutes
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILMS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(filmRepository.readById(id))
                .isNotEmpty()
                .hasValueSatisfying(film -> {
                    assertEquals(command.title(), film.getTitle());
                    assertEquals(command.category(), film.getCategory());
                    assertEquals(command.year(), film.getYear());
                    assertEquals(command.durationInMinutes(), film.getDurationInMinutes());
                });
    }

    @Test
    void film_title_is_unique() {
        //given
        addAdminUser();
        Film film = filmRepository.add(createFilm());
        CreateFilm command = createCreateFilmCommand(film.getTitle());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILMS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then

        String expectedMessage = new FilmTitleNotUniqueException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @ParameterizedTest
    @MethodSource("com.cinema.films.FilmFixture#getWrongFilmYears")
    void film_year_is_previous_current_or_nex_one(Integer wrongYear) {
        //given
        addAdminUser();
        CreateFilm command = createCreateFilmCommand(wrongYear);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILMS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        String expectedMessage = new FilmYearOutOfRangeException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    private void addCommonUser() {
        CreateUser command = new CreateUser(
                USERNAME,
                PASSWORD
        );
        createUserHandler.handle(command);
    }

    private void addAdminUser() {
        CreateAdmin command = new CreateAdmin(
                USERNAME,
                PASSWORD
        );
        createAdminHandler.handle(command);
    }
}
