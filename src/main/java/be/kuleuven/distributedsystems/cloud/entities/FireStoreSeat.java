package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Utils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class FireStoreSeat implements Serializable {
    private String company;
    private String showId;
    private String seatId;
    private String time;
    private String type;
    private String name;
    private double price;
    private boolean available;

    public FireStoreSeat(Seat seat) {
        this.company = seat.getCompany();
        this.showId = seat.getShowId().toString();
        this.seatId = seat.getSeatId().toString();
        this.time = seat.getTime().format(Utils.show_time_formatter);
        this.type = seat.getType();
        this.name = seat.getName();
        this.price = seat.getPrice();
        this.available = true;
    }

    public FireStoreSeat() {
    }

    public FireStoreSeat(String company, String showId, String seatId, String time, String type, String name, double price, boolean available) {
        this.company = company;
        this.showId = showId;
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public FireStoreSeat(String company, String showId, String seatId, String time, String type, String name, double price) {
        this.company = company;
        this.showId = showId;
        this.seatId = seatId;
        this.time = time;
        this.type = type;
        this.name = name;
        this.price = price;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireStoreSeat that = (FireStoreSeat) o;
        return Double.compare(that.price, price) == 0 && Objects.equals(company, that.company) && Objects.equals(showId, that.showId) && Objects.equals(seatId, that.seatId) && Objects.equals(time, that.time) && Objects.equals(type, that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(company, showId, seatId, time, type, name, price);
    }
}
