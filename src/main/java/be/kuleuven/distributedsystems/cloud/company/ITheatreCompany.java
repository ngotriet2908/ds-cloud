package be.kuleuven.distributedsystems.cloud.company;

import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Show;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ITheatreCompany {
    List<Show> getShows();
    Show getShow(UUID showId);
    List<LocalDateTime> getShowTimes(UUID showId);
    List<Seat> getAvailableSeats(UUID showId, LocalDateTime time);
    String getCompanyName();
    Seat getSeat(UUID showId, UUID seatId);
    Ticket reserveSeat(UUID showId, UUID seatId, String customer) throws Exception;
    void removeReserveSeat(UUID showId, UUID seatId, String customer);
}
