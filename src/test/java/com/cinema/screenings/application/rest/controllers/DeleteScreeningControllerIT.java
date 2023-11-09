package com.cinema.screenings.application.rest.controllers;

import com.cinema.SpringIT;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningRepository;
import com.cinema.users.application.commands.CreateAdmin;
import com.cinema.users.application.commands.CreateUser;
import com.cinema.users.application.commands.handlers.CreateAdminHandler;
import com.cinema.users.application.commands.handlers.CreateUserHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.cinema.screenings.ScreeningFixture.SCREENING_DATE;
import static com.cinema.screenings.ScreeningFixture.createScreening;
import static org.assertj.core.api.Assertions.assertThat;

class DeleteScreeningControllerIT extends SpringIT {

    private static final String SCREENINGS_BASE_ENDPOINT = "/screenings";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "12345";

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private CreateAdminHandler createAdminHandler;

    @Autowired
    private CreateUserHandler createUserHandler;

    @Test
    void screening_is_deleted_only_by_admin() {
        //given
        addCommonUser();
        Long screeningId = 1L;

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(SCREENINGS_BASE_ENDPOINT + "/" + screeningId)
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isForbidden();
    }

    @Test
    void screening_is_deleted() {
        //given
        addAdminUser();
        Screening screening = addScreening();

        //when
        WebTestClient.ResponseSpec spec = webTestClient
                .delete()
                .uri(SCREENINGS_BASE_ENDPOINT + "/" + screening.getId())
                .headers(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .exchange();

        //then
        spec.expectStatus().isNoContent();
        assertThat(screeningRepository.readById(screening.getId())).isEmpty();
    }

    private Screening addScreening() {
        Screening screening = createScreening(SCREENING_DATE);
        return screeningRepository.add(screening);
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
