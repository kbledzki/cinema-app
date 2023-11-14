package com.cinema.tickets.application.rest.controllers;

import com.cinema.SpringIT;
import com.cinema.films.application.commands.CreateFilm;
import com.cinema.films.application.commands.handlers.CreateFilmHandler;
import com.cinema.rooms.application.commands.CreateRoom;
import com.cinema.rooms.application.commands.handlers.CreateRoomHandler;
import com.cinema.screenings.application.commands.CreateScreening;
import com.cinema.screenings.application.commands.handlers.CreateScreeningHandler;
import com.cinema.tickets.application.queries.dto.TicketDto;
import com.cinema.tickets.domain.Ticket;
import com.cinema.tickets.domain.TicketRepository;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.cinema.tickets.TicketFixture.createCreateFilmCommand;
import static com.cinema.tickets.TicketFixture.createCreateRoomCommand;
import static com.cinema.tickets.TicketFixture.createCreateScreeningCommand;
import static com.cinema.tickets.TicketFixture.createTicket;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;

class ReadTicketControllerIT extends SpringIT {

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
    void tickets_are_read_by_user_id() {
        //given
        CreateFilm createFilmCommand = createCreateFilmCommand();
        createFilmHandler.handle(createFilmCommand);

        CreateRoom createRoomCommand = createCreateRoomCommand();
        createRoomHandler.handle(createRoomCommand);

        CreateScreening createScreeningCommand = createCreateScreeningCommand();
        createScreeningHandler.handle(createScreeningCommand);

        Ticket ticket = ticketRepository.add(createTicket());

        int rowNumber = 1;
        int seatNumber = 1;

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .get()
                .uri(TICKETS_BASE_ENDPOINT + "/my")
                .headers(headers -> headers.setBasicAuth(username, password))
                .exchange();

        //then
        List<TicketDto> expected = List.of(
                new TicketDto(
                        1L,
                        ticket.getStatus(),
                        createFilmCommand.title(),
                        createScreeningCommand.date(),
                        createRoomCommand.id(),
                        rowNumber,
                        seatNumber
                )
        );
        spec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[*]").value(everyItem(notNullValue()))
                .jsonPath("$[0].id").isEqualTo(expected.get(0).id())
                .jsonPath("$[0].status").isEqualTo(expected.get(0).status().name())
                .jsonPath("$[0].filmTitle").isEqualTo(expected.get(0).filmTitle())
                .jsonPath("$[0].screeningDate").isEqualTo(expected.get(0).screeningDate().toString())
                .jsonPath("$[0].roomId").isEqualTo(expected.get(0).roomId())
                .jsonPath("$[0].rowNumber").isEqualTo(expected.get(0).rowNumber())
                .jsonPath("$[0].seatNumber").isEqualTo(expected.get(0).seatNumber());
    }
}
