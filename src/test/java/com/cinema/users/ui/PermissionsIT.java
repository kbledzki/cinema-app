package com.cinema.users.ui;

import com.cinema.BaseIT;
import com.cinema.users.UserFixture;
import com.cinema.users.application.UserService;
import com.cinema.users.application.dto.CreateUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class PermissionsIT extends BaseIT {

    @Autowired
    private UserService userService;

    @Test
    void user_with_admin_role_has_access_to_endpoints_with_admin_prefix() {
        //given
        CreateUserDto crateUserDto = UserFixture.createCrateUserDto();
        userService.createAdmin(crateUserDto);
        String sampleAdminEndpoint = "/admin/halls";

        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .options()
                .uri(sampleAdminEndpoint)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(crateUserDto.mail(), crateUserDto.password()))
                .exchange();

        //then
        responseSpec.expectStatus().isOk();
    }

    @Test
    void user_with_common_role_has_no_access_to_endpoints_with_admin_prefix() {
        //given
        CreateUserDto crateUserDto = UserFixture.createCrateUserDto();
        userService.createUser(crateUserDto);
        String sampleAdminEndpoint = "/admin/halls";

        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .options()
                .uri(sampleAdminEndpoint)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(crateUserDto.mail(), crateUserDto.password()))
                .exchange();

        //then
        responseSpec.expectStatus().isForbidden();
    }

    @Test
    void user_dont_have_to_be_authenticated_to_have_access_to_endpoint_with_public_prefix() {
        //given
        String samplePublicEndpoint = "/public/films";

        //when
        WebTestClient.ResponseSpec responseSpec = webTestClient
                .options()
                .uri(samplePublicEndpoint)
                .exchange();

        //then
        responseSpec.expectStatus().isOk();
    }
}
