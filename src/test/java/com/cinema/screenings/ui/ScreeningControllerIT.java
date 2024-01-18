package com.cinema.screenings.ui;

import com.cinema.BaseIT;
import com.cinema.films.FilmFixture;
import com.cinema.films.domain.Film;
import com.cinema.films.domain.FilmRepository;
import com.cinema.halls.HallFixture;
import com.cinema.halls.domain.Hall;
import com.cinema.halls.domain.HallRepository;
import com.cinema.screenings.application.dto.CreateScreeningDto;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningRepository;
import com.cinema.screenings.domain.exceptions.ScreeningDateOutOfRangeException;
import com.cinema.screenings.domain.exceptions.ScreeningsCollisionsException;
import com.cinema.users.UserFixture;
import com.cinema.users.domain.User;
import com.cinema.users.domain.UserRepository;
import com.cinema.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.cinema.screenings.ScreeningFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class ScreeningControllerIT extends BaseIT {

    private static final String SCREENINGS_ADMIN_ENDPOINT = "/admin/screenings";
    private static final String SCREENINGS_PUBLIC_ENDPOINT = "/public/screenings";

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void screening_is_created() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        User user = addUser();
        CreateScreeningDto createScreeningDto = new CreateScreeningDto(SCREENING_DATE, film.getId(), hall.getId());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createScreeningDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(screeningRepository.getById(1L))
                .isNotEmpty()
                .hasValueSatisfying(screening -> {
                    assertThat(screening.getDate()).isEqualTo(createScreeningDto.date());
                    assertThat(screening.getFilm().getId()).isEqualTo(createScreeningDto.filmId());
                    assertThat(screening.getHall().getId()).isEqualTo(1L);
                });
    }

    @Test
    void screening_and_current_date_difference_is_min_7_days() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        User user = addUser();
        LocalDateTime screeningDate = LocalDateTime.now().plusDays(6);
        CreateScreeningDto createScreeningDto = new CreateScreeningDto(screeningDate, film.getId(), hall.getId());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createScreeningDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
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
        Hall hall = addHall();
        Film film = addFilm();
        User user = addUser();
        LocalDateTime screeningDate = LocalDateTime.now().plusDays(23);
        CreateScreeningDto createScreeningDto = new CreateScreeningDto(screeningDate, film.getId(), hall.getId());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createScreeningDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
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
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreening(film, hall);
        User user = addUser();
        CreateScreeningDto createScreeningDto = new CreateScreeningDto(
                screening.getDate().plusMinutes(10),
                film.getId(),
                hall.getId()
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(SCREENINGS_ADMIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createScreeningDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new ScreeningsCollisionsException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void screening_is_deleted() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreening(film, hall);
        User user = addUser();

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(SCREENINGS_ADMIN_ENDPOINT + "/" + screening.getId())
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isNoContent();
        assertThat(screeningRepository.getById(screening.getId())).isEmpty();
    }

    @Test
    void screenings_are_gotten() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreening(film, hall);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(SCREENINGS_PUBLIC_ENDPOINT)
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*]").value(hasSize(1))
                .jsonPath("$[*].*").value(everyItem(notNullValue()))
                .jsonPath("$.screenings[0].date").isEqualTo(screening.getDate().toString())
                .jsonPath("$.screenings[0].filmTitle").isEqualTo(film.getTitle());
    }

    @Test
    void screenings_are_gotten_by_date() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        LocalDate requiredDate = LocalDate.of(2023, 12, 13);
        Screening screeningWithRequiredDate = addScreening(requiredDate, film, hall);
        addScreening(requiredDate.minusDays(1), film, hall);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(SCREENINGS_PUBLIC_ENDPOINT)
                        .queryParam("date", requiredDate.toString())
                        .build()
                )
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.screenings[*].date").isEqualTo(screeningWithRequiredDate.getDate().toString());
    }

    @Test
    void seats_are_gotten_by_screening_id() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(SCREENINGS_PUBLIC_ENDPOINT + "/" + screening.getId() + "/seats")
                        .queryParam("screeningId", screening.getId())
                        .build()
                )
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$").isNotEmpty()
                .jsonPath("$.*.rowNumber").exists()
                .jsonPath("$.*.number").exists()
                .jsonPath("$.*.isFree").exists()
                .jsonPath("$.*.*").value(everyItem(notNullValue()));
    }

    private Screening addScreening(Film film, Hall hall) {
        return screeningRepository.add(createScreening(film, hall));
    }

    private Screening addScreening(LocalDate date, Film film, Hall hall) {
        LocalDateTime dateTime = date.atStartOfDay().plusHours(16);
        Screening screening = createScreening(dateTime, film, hall);
        return screeningRepository.add(screening);
    }

    private Screening addScreeningWithSeats(Hall hall, Film film) {
        return screeningRepository.add(createScreeningWithSeats(hall, film));
    }

    private Hall addHall() {
        return hallRepository.add(HallFixture.createHall());
    }

    private Film addFilm() {
        return filmRepository.add(FilmFixture.createFilm());
    }

    private User addUser() {
        return userRepository.add(UserFixture.createUser(UserRole.ADMIN));
    }
}
