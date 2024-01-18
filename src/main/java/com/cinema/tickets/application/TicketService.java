package com.cinema.tickets.application;

import com.cinema.screenings.domain.Screening;
import com.cinema.screenings.domain.ScreeningRepository;
import com.cinema.screenings.domain.ScreeningSeat;
import com.cinema.screenings.domain.exceptions.ScreeningNotFoundException;
import com.cinema.tickets.application.dto.BookTicketDto;
import com.cinema.tickets.application.dto.TicketDto;
import com.cinema.tickets.domain.Ticket;
import com.cinema.tickets.domain.TicketBookingPolicy;
import com.cinema.tickets.domain.TicketCancellingPolicy;
import com.cinema.tickets.domain.TicketRepository;
import com.cinema.tickets.domain.exceptions.TicketNotFoundException;
import com.cinema.tickets.infrastructure.TicketMapper;
import com.cinema.users.application.UserService;
import com.cinema.users.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketBookingPolicy ticketBookingPolicy;
    private final TicketCancellingPolicy ticketCancellingPolicy;
    private final TicketMapper ticketMapper;
    private final ScreeningRepository screeningRepository;
    private final UserService userService;
    private final Clock clock;

    @Transactional
    public void bookTicket(BookTicketDto dto) {
        log.info("Dto:{}", dto);
        Screening screening = screeningRepository
                .getById(dto.screeningId())
                .orElseThrow(ScreeningNotFoundException::new);
        log.info("Found screening:{}", screening);
        ticketBookingPolicy.checkScreeningDate(screening.timeToScreeningInHours(clock));
        User loggedUserId = userService.getLoggedUser();
        dto
                .seatsIds()
                .stream()
                .map(seatId -> {
                    ScreeningSeat foundSeat = screening.findSeat(seatId);
                    log.info("Found seat: {}", foundSeat);
                    foundSeat.markAsNotFree();
                    return new Ticket(Ticket.Status.BOOKED, foundSeat, loggedUserId);
                })
                .toList()
                .forEach(ticket -> {
                    Ticket addedTicket = ticketRepository.add(ticket);
                    log.info("Added ticket:{}", addedTicket);
                });
    }

    @Transactional
    public void cancelTicket(Long ticketId) {
        log.info("Ticket id:{}", ticketId);
        User loggedUser = userService.getLoggedUser();
        Ticket ticket = ticketRepository
                .getByIdAndUserId(ticketId, loggedUser.getId())
                .orElseThrow(TicketNotFoundException::new);
        log.info("Found ticket:{}", ticket);
        ticket.cancel(ticketCancellingPolicy, clock);
        log.info("Ticket cancelled:{}", ticket);
    }

    public List<TicketDto> getAllTicketsByLoggedUser() {
        User loggedUser = userService.getLoggedUser();
        return ticketRepository
                .getAllByUserId(loggedUser.getId())
                .stream()
                .map(ticketMapper::mapToDto)
                .toList();
    }
}