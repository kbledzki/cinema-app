package com.cinema.tickets.ui;

import com.cinema.films.domain.Film;
import com.cinema.halls.domain.Hall;
import com.cinema.halls.domain.exceptions.HallNotFoundException;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningSeat;
import com.cinema.screenings.domain.exceptions.ScreeningNotFoundException;
import com.cinema.screenings.domain.exceptions.ScreeningSeatNotFoundException;
import com.cinema.tickets.application.dto.BookTicketDto;
import com.cinema.tickets.domain.Ticket;
import com.cinema.tickets.domain.exceptions.TicketAlreadyCancelledException;
import com.cinema.tickets.domain.exceptions.TicketAlreadyExistsException;
import com.cinema.tickets.domain.exceptions.TicketBookTooLateException;
import com.cinema.tickets.domain.exceptions.TicketCancelTooLateException;
import com.cinema.users.UserFixture;
import com.cinema.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketControllerIT extends TicketBaseIT {

    private User user;

    @BeforeEach
    void setUpUser() {
        user = userRepository.add(UserFixture.createUser());
    }

    @Test
    void ticket_is_booked_for_existing_screening() {
        //given
        long nonExistingScreeningId = 0L;
        long seatId = 1L;
        BookTicketDto bookTicketDto = new BookTicketDto(
                nonExistingScreeningId,
                List.of(seatId)
        );


        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new ScreeningNotFoundException().getMessage();
        spec
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void ticket_is_booked_for_existing_seat() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreening(hall, film);
        long nonExistingSeatId = 0L;
        BookTicketDto bookTicketDto = new BookTicketDto(
                screening.getId(),
                List.of(nonExistingSeatId)
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new ScreeningSeatNotFoundException().getMessage();
        spec
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void ticket_is_booked() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);
        BookTicketDto bookTicketDto = new BookTicketDto(
                screening.getId(),
                List.of(screening.getSeats().stream().findFirst().get().getId())
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(ticketRepository.getByIdAndUserId(1L, 1L))
                .isNotEmpty()
                .hasValueSatisfying(ticket -> {
                    assertEquals(1L, ticket.getUser().getId());
                    assertEquals(Ticket.Status.BOOKED, ticket.getStatus());
                    assertEquals(bookTicketDto.screeningId(), ticket.getSeat().getScreening().getId());
                    assertEquals(bookTicketDto.seatsIds().stream().findFirst().get(), ticket.getSeat().getId());
                });
    }

    @Test
    void tickets_are_booked() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);
        ScreeningSeat seat1 = screening.getSeats().get(0);
        ScreeningSeat seat2 = screening.getSeats().get(1);
        BookTicketDto bookTicketDto = new BookTicketDto(
                screening.getId(),
                List.of(seat1.getId(), seat2.getId())
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(ticketRepository.getAllByUserId(user.getId()))
                .isNotEmpty()
                .allSatisfy(ticket -> assertEquals(Ticket.Status.BOOKED, ticket.getStatus()));
    }

    @Test
    void ticket_is_booked_for_free_seat() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithNotFreeSeat(hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        BookTicketDto bookTicketDto = new BookTicketDto(
                screening.getId(),
                List.of(seat.getId())
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new TicketAlreadyExistsException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void ticket_is_booked_at_least_1h_before_screening() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(LocalDateTime.now(clock).minusMinutes(59), hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        BookTicketDto bookTicketDto = new BookTicketDto(
                screening.getId(),
                List.of(seat.getId())
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new TicketBookTooLateException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void ticket_is_cancelled() {
        //give
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        Ticket ticket = addTicket(seat, user);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .patch()
                .uri(TICKETS_BASE_ENDPOINT + "/" + ticket.getId() + "/cancel")
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec.expectStatus().isOk();
        assertThat(ticketRepository.getByIdAndUserId(ticket.getId(), ticket.getUser().getId()))
                .isNotEmpty()
                .hasValueSatisfying(cancelledTicket ->
                        assertEquals(Ticket.Status.CANCELLED, cancelledTicket.getStatus())
                );
    }

    @Test
    void ticket_already_cancelled_cannot_be_cancelled() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        Ticket ticket = addCancelledTicket(seat, user);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .patch()
                .uri(TICKETS_BASE_ENDPOINT + "/" + ticket.getId() + "/cancel")
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new TicketAlreadyCancelledException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void ticket_is_cancelled_at_least_24h_before_screening() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(LocalDateTime.now(clock).minusHours(23), hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        Ticket ticket = addTicket(seat, user);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .patch()
                .uri(TICKETS_BASE_ENDPOINT + "/" + ticket.getId() + "/cancel")
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        String expectedMessage = new TicketCancelTooLateException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    @Test
    void tickets_are_gotten_by_user_id() {
        //given
        Hall hall = addHall();
        Film film = addFilm();
        Screening screening = addScreeningWithSeats(hall, film);
        ScreeningSeat seat = screening.getSeats().stream().findFirst().get();
        Ticket ticket = addTicket(seat, user);

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(TICKETS_BASE_ENDPOINT + "/my")
                .headers(headers -> headers.setBasicAuth(user.getMail(), user.getPassword()))
                .exchange();

        //then
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*]").value(everyItem(notNullValue()))
                .jsonPath("$.tickets[0].id").isEqualTo(ticket.getId())
                .jsonPath("$.tickets[0].status").isEqualTo(ticket.getStatus().name())
                .jsonPath("$.tickets[0].filmTitle").isEqualTo(film.getTitle())
                .jsonPath("$.tickets[0].screeningDate").isEqualTo(screening.getDate().toString())
                .jsonPath("$.tickets[0].hallId").isEqualTo(hall.getId())
                .jsonPath("$.tickets[0].rowNumber").isEqualTo(seat.getHallSeat().getRowNumber())
                .jsonPath("$.tickets[0].seatNumber").isEqualTo(seat.getHallSeat().getNumber());
    }
}
