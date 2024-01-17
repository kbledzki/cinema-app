package com.cinema.tickets.ui;

import com.cinema.tickets.application.TicketService;
import com.cinema.tickets.application.dto.BookTicketDto;
import com.cinema.tickets.application.dto.TicketDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Slf4j
class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @SecurityRequirement(name = "basic")
    ResponseEntity<Object> bookTicket(@RequestBody @Valid BookTicketDto dto) {
        log.info("Dto:{}", dto);
        ticketService.bookTicket(dto);
        ResponseEntity<Object> responseEntity = ResponseEntity.created(URI.create("/my/tickets")).build();
        log.info("Response entity:{}", responseEntity);
        return responseEntity;
    }

    @PatchMapping("/{ticketId}/cancel")
    @SecurityRequirement(name = "basic")
    ResponseEntity<Object> cancelTicket(@PathVariable Long ticketId) {
        log.info("Ticket id:{}", ticketId);
        ticketService.cancelTicket(ticketId);
        ResponseEntity<Object> responseEntity = ResponseEntity.ok().build();
        log.info("Response entity:{}", responseEntity);
        return responseEntity;
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "basic")
    TicketsResponse getAllTicketsByLoggedUser() {
        List<TicketDto> tickets = ticketService.getAllTicketsByLoggedUser();
        return new TicketsResponse(tickets);
    }
}
