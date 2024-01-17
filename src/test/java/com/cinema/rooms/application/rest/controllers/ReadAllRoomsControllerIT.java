package com.cinema.rooms.application.rest.controllers;

import com.cinema.BaseIT;
import com.cinema.rooms.domain.Room;
import com.cinema.rooms.domain.RoomRepository;
import com.cinema.users.application.commands.CreateAdmin;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateAdminHandler;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.rooms.RoomFixture.createRoom;

class ReadAllRoomsControllerIT extends BaseIT {

    private static final String ROOMS_ENDPOINT = "/rooms";

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CreateUserHandler createUserHandler;

    @Autowired
    private CreateAdminHandler createAdminHandler;

    @Test
    void rooms_are_read() {
        //given
        Room room = roomRepository.add(createRoom());
        String adminMail = "admin@mail.com";
        String adminPassword = "12345";
        CreateAdmin command = new CreateAdmin(adminMail, adminPassword);
        createAdminHandler.handle(command);

        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(ROOMS_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(command.adminMail(), command.adminPassword()))
                .exchange();

        //then
        responseSpec
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(room.getId())
                .jsonPath("$[0].rowsNumber").isEqualTo(room.getRowsNumber())
                .jsonPath("$[0].rowSeatsNumber").isEqualTo(room.getRowSeatsNumber());
    }

    @Test
    void rooms_are_read_only_by_authorized_user() {
        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(ROOMS_ENDPOINT)
                .exchange();

        //then
        responseSpec.expectStatus().isUnauthorized();
    }

    @Test
    void rooms_are_read_only_by_admin() {
        //given
        String userMail = "user1@mail.com";
        String userPassword = "12345";
        CreateUser command = new CreateUser(userMail, userPassword);
        createUserHandler.handle(command);

        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .get()
                .uri(ROOMS_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(command.mail(), command.password()))
                .exchange();

        //then
        responseSpec.expectStatus().isForbidden();
    }
}
