package com.cinema.tickets.application.rest;

import com.cinema.tickets.application.dto.TicketBookDto;
import com.cinema.tickets.application.dto.TicketDto;
import com.cinema.tickets.application.services.TicketService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    @SecurityRequirement(name = "basic")
    void bookTicket(@RequestBody @Valid TicketBookDto dto) {
        ticketService.bookTicket(dto);
    }

    @PatchMapping("/{ticketId}/cancel")
    @SecurityRequirement(name = "basic")
    void cancelTicket(@PathVariable Long ticketId) {
        ticketService.cancelTicket(ticketId);
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "basic")
    List<TicketDto> readAllTicketsByCurrentUser() {
        return ticketService.readByCurrentUser();
    }
}

