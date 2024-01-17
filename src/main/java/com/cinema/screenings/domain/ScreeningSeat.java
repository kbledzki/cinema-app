package com.cinema.screenings.domain;

import com.cinema.halls.domain.HallSeat;
import com.cinema.tickets.domain.exceptions.TicketAlreadyExistsException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

@Entity
@Getter
@ToString(exclude = {"hallSeat", "screening"})
public class ScreeningSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isFree;

    @OneToOne(fetch = FetchType.LAZY)
    private HallSeat hallSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Screening screening;

    @Version
    private int version;

    protected ScreeningSeat() {
    }

    public ScreeningSeat(boolean isFree, HallSeat hallSeat, Screening screening) {
        this.isFree = isFree;
        this.hallSeat = hallSeat;
        this.screening = screening;
    }

    public void markAsNotFree() {
        if (!this.isFree) {
            throw new TicketAlreadyExistsException();
        }
        this.isFree = false;
    }

    public void markAsFree() {
        this.isFree = true;
    }
}
