package com.cinema.screenings.application.rest.controllers;

import com.cinema.BaseIT;
import com.cinema.films.application.commands.handlers.CreateFilmHandler;
import com.cinema.rooms.application.commands.handlers.CreateRoomHandler;
import com.cinema.rooms.domain.exceptions.RoomsNoAvailableException;
import com.cinema.screenings.application.commands.CreateScreening;
import com.cinema.screenings.application.queries.dto.ScreeningDto;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningRepository;
import com.cinema.screenings.domain.exceptions.ScreeningDateOutOfRangeException;
import com.cinema.users.application.commands.CreateAdmin;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateAdminHandler;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.List;

import static com.cinema.screenings.ScreeningFixture.*;
import static org.hamcrest.Matchers.equalTo;

class CreateScreeningControllerIT extends BaseIT {

    private static final String SCREENINGS_BASE_ENDPOINT = "/screenings";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "12345";

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private CreateFilmHandler createFilmHandler;

    @Autowired
    private CreateRoomHandler createRoomHandler;

    @Autowired
    private CreateAdminHandler createAdminHandler;

    @Autowired
    private CreateUserHandler createUserHandler;

    @Test
    void screening_is_created_only_by_admin() {
        //given
        addCommonUser();

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isForbidden();
    }

    @Test
    void screening_is_created() {
        //given
        Long filmId = 1L;
        String filmTitle = "Sample title";
        String roomId = "1";
        addAdminUser();
        addFilm(filmTitle);
        addRoom();
        CreateScreening command = new CreateScreening(SCREENING_DATE, filmId);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        var expectedDto = List.of(
                new ScreeningDto(
                        1L,
                        command.date(),
                        filmTitle,
                        roomId
                )
        );
        webTestClient
                .get()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .exchange()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(expectedDto.get(0).id())
                .jsonPath("$[0].date").isEqualTo(expectedDto.get(0).date().toString())
                .jsonPath("$[0].filmTitle").isEqualTo(expectedDto.get(0).filmTitle());
    }

    @Test
    void screening_and_current_date_difference_is_min_7_days() {
        //given
        Long filmId = 1L;
        String filmTitle = "Sample title";
        addAdminUser();
        addFilm(filmTitle);
        addRoom();
        LocalDateTime screeningDate = LocalDateTime.now().plusDays(6);
        CreateScreening command = new CreateScreening(screeningDate, filmId);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        String expectedMessage = new ScreeningDateOutOfRangeException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void screening_and_current_date_difference_is_max_21_days() {
        //given
        Long filmId = 1L;
        String filmTitle = "Sample film";
        addAdminUser();
        addRoom();
        addFilm(filmTitle);
        LocalDateTime screeningDate = LocalDateTime.now().plusDays(23);
        CreateScreening command = new CreateScreening(screeningDate, filmId);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        String expectedMessage = new ScreeningDateOutOfRangeException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void screenings_collision_cannot_exist() {
        //given
        Long filmId = 1L;
        String filmTitle = "Sample title";
        addAdminUser();
        addFilm(filmTitle);
        Screening screening = addScreening();
        CreateScreening command = new CreateScreening(
                screening.getDate().plusMinutes(10),
                filmId
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        String expectedMessage = new RoomsNoAvailableException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    private Screening addScreening() {
        var screening = createScreening(SCREENING_DATE);
        return screeningRepository.add(screening);
    }

    private void addRoom() {
        createRoomHandler.handle(createCreateRoomCommand());
    }

    private void addFilm(String title) {
        createFilmHandler.handle(createCreateFilmCommand(title));
    }

    private void addCommonUser() {
        var command = new CreateUser(
                USERNAME,
                PASSWORD
        );
        createUserHandler.handle(command);
    }

    private void addAdminUser() {
        var command = new CreateAdmin(
                USERNAME,
                PASSWORD
        );
        createAdminHandler.handle(command);
    }
}
