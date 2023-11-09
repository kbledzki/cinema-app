package com.cinema.films.application.rest.controllers;

import com.cinema.SpringIT;
import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmRepository;
import com.cinema.users.application.commands.CreateAdmin;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateAdminHandler;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.films.FilmFixture.createFilm;
import static org.assertj.core.api.Assertions.assertThat;

class DeleteFilmControllerIT extends SpringIT {

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
    void film_is_deleted_only_by_admin() {
        //given
        addCommonUser();

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(FILMS_BASE_ENDPOINT + "/Film 1")
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void film_is_deleted() {
        //given
        addAdminUser();
        Film film = filmRepository.add(createFilm());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(FILMS_BASE_ENDPOINT + "/" + film.getId())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isNoContent();
        assertThat(filmRepository.existsByTitle(film.getTitle())).isFalse();
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
