package com.cinema.tickets.application.rest.controllers;

import com.cinema.BaseIT;
import com.cinema.films.application.commands.handlers.CreateFilmHandler;
import com.cinema.rooms.application.commands.handlers.CreateRoomHandler;
import com.cinema.screenings.application.commands.handlers.CreateScreeningHandler;
import com.cinema.tickets.application.commands.BookTicket;
import com.cinema.tickets.domain.Ticket;
import com.cinema.tickets.domain.TicketRepository;
import com.cinema.tickets.domain.TicketStatus;
import com.cinema.tickets.domain.exceptions.TicketAlreadyExistsException;
import com.cinema.tickets.domain.exceptions.TicketBookTooLateException;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.cinema.tickets.TicketFixture.SCREENING_DATE;
import static com.cinema.tickets.TicketFixture.createCreateFilmCommand;
import static com.cinema.tickets.TicketFixture.createCreateRoomCommand;
import static com.cinema.tickets.TicketFixture.createCreateScreeningCommand;
import static com.cinema.tickets.TicketFixture.createTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BookTicketControllerIT extends BaseIT {

    private static final String TICKETS_BASE_ENDPOINT = "/tickets";
    private static final String username = "user1@mail.com";
    private static final String password = "12345";

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CreateUserHandler createUserHandler;

    @Autowired
    private CreateFilmHandler createFilmHandler;

    @Autowired
    private CreateScreeningHandler createScreeningHandler;

    @Autowired
    private CreateRoomHandler createRoomHandler;

    @SpyBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        createUserHandler.handle(
                new CreateUser(
                        username,
                        password
                )
        );
    }

    @Test
    void ticket_is_made_for_existing_screening() {
        //given
        Long nonExistingScreeningId = 0L;
        Long seatId = 1L;
        BookTicket command = new BookTicket(
                nonExistingScreeningId,
                seatId
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(username, password))
                .exchange();

        //then
        spec.expectStatus().isNotFound();
    }

    @Test
    void ticket_is_booked_for_existing_seat() {
        //given
        addScreening();
        Long screeningId = 1L;
        Long nonExistingSeatId = 0L;
        BookTicket command = new BookTicket(
                screeningId,
                nonExistingSeatId
        );


        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(username, password))
                .exchange();

        //then
        spec.expectStatus().isNotFound();
    }

    @Test
    void ticket_is_booked() {
        //given
        String filmTitle = "Title 1";
        String roomId = "1";
        addScreening(filmTitle, roomId);
        Long screeningId = 1L;
        Long seatId = 1L;
        BookTicket command = new BookTicket(
                screeningId,
                seatId
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(username, password))
                .exchange();

        //then
        spec.expectStatus().isCreated();
        assertThat(ticketRepository.readById(1L))
                .isNotEmpty()
                .hasValueSatisfying(ticket -> {
                    assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
                    assertEquals(screeningId, ticket.getScreeningId());
                    assertEquals(seatId, ticket.getSeatId());
                    assertEquals(1L, ticket.getUserId());
                });
    }

    @Test
    void ticket_is_unique() {
        //given
        addScreening();
        Ticket ticket = ticketRepository.add(createTicket());
        BookTicket command = new BookTicket(
                ticket.getScreeningId(),
                ticket.getSeatId()
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(username, password))
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
        LocalDateTime screeningDate = SCREENING_DATE;
        addScreening(screeningDate);
        Mockito
                .when(clock.instant())
                .thenReturn(screeningDate.minusMinutes(59).toInstant(ZoneOffset.UTC));
        Long screeningId = 1L;
        Long seatId = 1L;
        BookTicket command = new BookTicket(
                screeningId,
                seatId
        );

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(TICKETS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .headers(headers -> headers.setBasicAuth(username, password))
                .exchange();

        //then
        String expectedMessage = new TicketBookTooLateException().getMessage();
        spec
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }

    private void addScreening() {
        createFilmHandler.handle(createCreateFilmCommand());
        createRoomHandler.handle(createCreateRoomCommand());
        createScreeningHandler.handle(createCreateScreeningCommand(SCREENING_DATE));
    }

    private void addScreening(LocalDateTime screeningDate) {
        createFilmHandler.handle(createCreateFilmCommand());
        createRoomHandler.handle(createCreateRoomCommand());
        createScreeningHandler.handle(createCreateScreeningCommand(screeningDate));
    }

    private void addScreening(String filmTitle, String roomId) {
        createFilmHandler.handle(createCreateFilmCommand(filmTitle));
        createRoomHandler.handle(createCreateRoomCommand(roomId));
        createScreeningHandler.handle(createCreateScreeningCommand(SCREENING_DATE));
    }
}
