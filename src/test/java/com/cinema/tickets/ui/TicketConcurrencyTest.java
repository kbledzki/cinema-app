package com.cinema.tickets.ui;

import com.cinema.films.domain.Film;
import com.cinema.halls.domain.Hall;
import com.cinema.halls.domain.exceptions.HallNotFoundException;
import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningSeat;
import com.cinema.tickets.application.dto.BookTicketDto;
import com.cinema.users.UserFixture;
import com.cinema.users.domain.User;
import com.cinema.users.domain.exceptions.UserNotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TicketConcurrencyTest extends TicketBaseIT {

//    @Test
//    void ticket_is_booked_only_by_one_user() {
//        //given
//        Film film = addFilm();
//        Hall hall = addHall();
//        Screening screening = addScreeningWithSeats(hall, film);
//        ScreeningSeat seat = screening.getSeats().stream().findFirst().orElseThrow(HallNotFoundException::new);
//        List<User> users = addUsers();
//        BookTicketDto bookTicketDto = new BookTicketDto(screening.getId(), List.of(seat.getId()));
//
//        //when
//        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
//            executorService.submit(() -> bookTicket(users.stream().findFirst().orElseThrow(UserNotFoundException::new).getMail(), bookTicketDto));
//            executorService.submit(() -> bookTicket(users.get(1).getMail(), bookTicketDto));
//            executorService.submit(() -> bookTicket(users.get(2).getMail(), bookTicketDto));
//            executorService.shutdown();
//        }
//
//        //then
//        Assertions.assertThat(ticketRepository.getAll()).hasSize(1);
//    }

    private void bookTicket(String userMail, BookTicketDto bookTicketDto) {
        webTestClient
                .post()
                .uri("/tickets")
                .headers(headers -> headers.setBasicAuth(userMail, UserFixture.PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bookTicketDto)
                .exchange();
    }
}
