package be.kuleuven.distributedsystems.cloud.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FireStoreTicket implements Serializable {
    private String company;
    private String showId;
    private String seatId;
    private String ticketId;
    private String customer;

    public FireStoreTicket(Ticket ticket) {
        this.company = ticket.getCompany();
        this.showId = ticket.getShowId().toString();
        this.seatId = ticket.getSeatId().toString();
        this.ticketId = ticket.getTicketId().toString();
        this.customer = ticket.getCustomer();
    }

    public FireStoreTicket() {
    }

    public FireStoreTicket(String company, String showId, String seatId, String ticketId, String customer) {
        this.company = company;
        this.showId = showId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getShowId() {
        return showId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
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
        FireStoreTicket that = (FireStoreTicket) o;
        return Objects.equals(company, that.company) && Objects.equals(showId, that.showId) && Objects.equals(seatId, that.seatId) && Objects.equals(ticketId, that.ticketId) && Objects.equals(customer, that.customer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(company, showId, seatId, ticketId, customer);
    }
}
