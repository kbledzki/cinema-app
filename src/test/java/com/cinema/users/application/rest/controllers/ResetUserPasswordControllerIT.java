package com.cinema.users.application.rest.controllers;

import com.cinema.SpringIT;
import com.cinema.users.domain.User;
import com.cinema.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static com.cinema.users.UserFixture.createUser;
import static org.assertj.core.api.Assertions.assertThat;

class ResetUserPasswordControllerIT extends SpringIT {

    private static final String USERS_BASE_ENDPOINT = "/users";

    @Autowired
    private UserRepository userRepository;

    @Test
    void user_password_is_reset() {
        //given
        User user = userRepository.add(createUser());

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .patch()
                .uri(uriBuilder -> uriBuilder
                        .path(USERS_BASE_ENDPOINT + "/password/reset")
                        .queryParam("mail", user.getMail())
                        .build()
                )
                .attribute("mail", user.getMail())
                .exchange();

        //then
        spec.expectStatus().isOk();
        UUID userPasswordResetToken = userRepository
                .readyByMail(user.getUsername())
                .orElseThrow()
                .getPasswordResetToken();
        assertThat(userPasswordResetToken).isNotNull();
    }
}
