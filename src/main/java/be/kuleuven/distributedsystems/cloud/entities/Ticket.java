package be.kuleuven.distributedsystems.cloud.entities;

import java.util.UUID;

public class Ticket {
    private String company;
    private UUID showId;
    private UUID seatId;
    private UUID ticketId;
    private String customer;

    public Ticket() {
    }

    public Ticket(FireStoreTicket ticket) {
        this.company = ticket.getCompany();
        this.showId = UUID.fromString(ticket.getShowId());
        this.seatId = UUID.fromString(ticket.getSeatId());
        this.ticketId = UUID.fromString(ticket.getTicketId());
        this.customer = ticket.getCustomer();
    }

    public Ticket(String company, UUID showId, UUID seatId, UUID ticketId, String customer) {
        this.company = company;
        this.showId = showId;
        this.seatId = seatId;
        this.ticketId = ticketId;
        this.customer = customer;
    }

    public String getCompany() {
        return company;
    }

    public UUID getShowId() {
        return showId;
    }

    public UUID getSeatId() {
        return this.seatId;
    }

    public UUID getTicketId() {
        return this.ticketId;
    }

    public String getCustomer() {
        return this.customer;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Ticket)) {
            return false;
        }
        var other = (Ticket) o;
        return this.ticketId.equals(other.ticketId)
                && this.seatId.equals(other.seatId)
                && this.showId.equals(other.showId)
                && this.company.equals(other.company);
    }

    @Override
    public int hashCode() {
        return this.company.hashCode() * this.showId.hashCode() * this.seatId.hashCode() * this.ticketId.hashCode();
    }

    @Override
    public String toString() {
        return "Ticket{" + "\n" +
                "company='" + company + '\'' + "\n" +
                ", showId=" + showId + "\n" +
                ", seatId=" + seatId + "\n" +
                ", ticketId=" + ticketId + "\n" +
                ", customer='" + customer + '\'' + "\n" +
                '}' + "\n";
    }
}
