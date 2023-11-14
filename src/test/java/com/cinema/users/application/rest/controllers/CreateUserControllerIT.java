package com.cinema.users.application.rest.controllers;

import com.cinema.SpringIT;
import com.cinema.users.UserFixture;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.domain.User;
import com.cinema.users.domain.UserRepository;
import com.cinema.users.domain.UserRole;
import com.cinema.users.domain.exceptions.UserMailNotUniqueException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.users.UserFixture.createCrateUserCommand;
import static com.cinema.users.UserFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserControllerIT extends SpringIT {

    private static final String USERS_BASE_ENDPOINT = "/users";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    void user_is_created() {
        //given
        CreateUser crateUserCommand = UserFixture.createCrateUserCommand();

        //then
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(USERS_BASE_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(crateUserCommand)
                .exchange();

        spec.expectStatus().isOk();
        assertThat(userRepository.readyByMail(crateUserCommand.mail()))
                .isNotEmpty()
                .hasValueSatisfying(user -> {
                    assertEquals(crateUserCommand.mail(), user.getUsername());
                    assertTrue(passwordEncoder.matches(crateUserCommand.password(), user.getPassword()));
                    assertEquals(UserRole.COMMON, user.getRole());
                });
    }

    @Test
    void user_name_cannot_be_duplicated() {
        //given
        User user = userRepository.add(createUser("user1@mail.com"));
        CreateUser crateUserCommand = createCrateUserCommand(user.getUsername());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .post()
                .uri(USERS_BASE_ENDPOINT)
                .bodyValue(crateUserCommand)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        //then
        String expectedMessage = new UserMailNotUniqueException().getMessage();
        spec
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody()
                .jsonPath("$.message", equalTo(expectedMessage));
    }
}