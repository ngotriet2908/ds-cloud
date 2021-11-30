package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Utils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class FireStoreBooking implements Serializable {
    private String id;
    private String time;
    private List<FireStoreTicket> tickets;
    private String customer;

    public FireStoreBooking(String id, String time, List<FireStoreTicket> tickets, String customer) {
        this.id = id;
        this.time = time;
        this.tickets = tickets;
        this.customer = customer;
    }

    public FireStoreBooking(Booking booking) {
        this.id = booking.getId().toString();
        this.time = booking.getTime().format(Utils.show_time_formatter);
        this.tickets = booking.getTickets()
                .stream()
                .map(FireStoreTicket::new)
                .collect(Collectors.toList());
        this.customer = booking.getCustomer();
    }

    public FireStoreBooking() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<FireStoreTicket> getTickets() {
        return tickets;
    }

    public void setTickets(List<FireStoreTicket> tickets) {
        this.tickets = tickets;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireStoreBooking that = (FireStoreBooking) o;
        return Objects.equals(id, that.id) && Objects.equals(time, that.time) && Objects.equals(tickets, that.tickets) && Objects.equals(customer, that.customer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time, tickets, customer);
    }
}
