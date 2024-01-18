package com.cinema.films.ui;

import com.cinema.BaseIT;
import com.cinema.films.FilmFixture;
import com.cinema.films.application.dto.CreateFilmDto;
import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmCategory;
import com.cinema.films.domain.FilmRepository;
import com.cinema.films.domain.exceptions.FilmTitleNotUniqueException;
import com.cinema.users.UserFixture;
import com.cinema.users.domain.User;
import com.cinema.users.domain.UserRepository;
import com.cinema.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.films.FilmFixture.createFilm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FilmControllerIT extends BaseIT {

    private static final String FILM_PUBLIC_ENDPOINT = "/public/films";
    private static final String FILM_ADMIN_ENDPOINT = "/admin/films";

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void film_is_created() {
        //given
        User user = addUser();
        long id = 1L;
        String title = "Some filmId";
        FilmCategory category = FilmCategory.COMEDY;
        int year = 2023;
        int durationInMinutes = 100;
        CreateFilmDto createFilmDto = new CreateFilmDto(
                title,
                category,
                year,
                durationInMinutes
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILM_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createFilmDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(filmRepository.getById(id))
                .isNotEmpty()
                .hasValueSatisfying(film -> {
                    assertEquals(createFilmDto.title(), film.getTitle());
                    assertEquals(createFilmDto.category(), film.getCategory());
                    assertEquals(createFilmDto.year(), film.getYear());
                    assertEquals(createFilmDto.durationInMinutes(), film.getDurationInMinutes());
                });
    }

    @Test
    void film_title_is_unique() {
        //given
        User user = addUser();
        Film film = filmRepository.add(createFilm());
        CreateFilmDto createFilmDto = FilmFixture.createCreateFilmDto(film.getTitle());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(FILM_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createFilmDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then

        String expectedMessage = new FilmTitleNotUniqueException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void film_is_deleted() {
        //given
        User user = addUser();
        Film film = filmRepository.add(createFilm());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(FILM_ADMIN_ENDPOINT + "/" + film.getId())
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isNoContent();
        assertThat(filmRepository.existsByTitle(film.getTitle())).isFalse();
    }

    @Test
    void films_are_gotten() {
        //given
        Film film = filmRepository.add(createFilm());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(FILM_PUBLIC_ENDPOINT)
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*]").value(hasSize(1))
                .jsonPath("$.films[0].title").isEqualTo(film.getTitle())
                .jsonPath("$.films[0].category").isEqualTo(film.getCategory().name())
                .jsonPath("$.films[0].year").isEqualTo(film.getYear())
                .jsonPath("$.films[0].durationInMinutes").isEqualTo(film.getDurationInMinutes());
    }

    @Test
    void films_are_gotten_by_title() {
        //given
        String title = "Film";
        String otherTitle = "Other Film";
        filmRepository.add(createFilm(title));
        filmRepository.add(createFilm(otherTitle));

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(FILM_PUBLIC_ENDPOINT)
                        .queryParam("title", title)
                        .build()
                )
                .exchange();

        //then
        spec
                .expectBody()
                .jsonPath("$.*.title").value(everyItem(equalTo(title)));
    }

    @Test
    void films_are_gotten_by_category() {
        //given
        FilmCategory category = FilmCategory.COMEDY;
        FilmCategory otherCategory = FilmCategory.DRAMA;
        filmRepository.add(createFilm(category));
        filmRepository.add(createFilm(otherCategory));

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(FILM_PUBLIC_ENDPOINT)
                        .queryParam("category", category)
                        .build()
                )
                .exchange();

        //then
        spec
                .expectBody()
                .jsonPath("$.*.category").value(everyItem(equalTo(category.name())));
    }

    private User addUser() {
        return userRepository.add(UserFixture.createUser(UserRole.ADMIN));
    }
}