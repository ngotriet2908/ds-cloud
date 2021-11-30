package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Utils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Booking {
    private UUID id;
    private LocalDateTime time;
    private List<Ticket> tickets;
    private String customer;

    public Booking(UUID id, LocalDateTime time, List<Ticket> tickets, String customer) {
        this.id = id;
        this.time = time;
        this.tickets = tickets;
        this.customer = customer;
    }

    public Booking(FireStoreBooking booking) {
        this.id = UUID.fromString(booking.getId());
        this.time = LocalDateTime.parse(booking.getTime(), Utils.show_time_formatter);
        this.tickets = booking.getTickets()
                .stream()
                .map(Ticket::new)
                .collect(Collectors.toList());
        this.customer = booking.getCustomer();
    }

    public Booking() {
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public UUID getId() {
        return this.id;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public List<Ticket> getTickets() {
        return this.tickets;
    }

    public String getCustomer() {
        return this.customer;
    }
}
