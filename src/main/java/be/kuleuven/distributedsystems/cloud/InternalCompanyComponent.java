package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class InternalCompanyComponent {
    Logger logger = LoggerFactory.getLogger(InternalCompanyComponent.class);

    @Autowired
    private Firestore firestoreDB;

    public List<Show> getShows() {
        List<Show> shows = new ArrayList<>();
        try {
            Query query = firestoreDB
                    .collection(Utils.THEATER_DATA)
                    .document(Utils.INTERNAL_COMPANY_NAME)
                    .collection("Shows");

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                shows.add(new Show(document.toObject(FireStoreShow.class)));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return shows;
    }

    public Show getShow(UUID showId) {
        try {
            Query query = firestoreDB
                    .collection(Utils.THEATER_DATA)
                    .document(Utils.INTERNAL_COMPANY_NAME)
                    .collection("Shows")
                    .whereEqualTo("showId", showId.toString())
                    ;

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                return new Show(document.toObject(FireStoreShow.class));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public List<LocalDateTime> getShowTimes(UUID showId) {
        try {
            Query query = firestoreDB
                    .collection(Utils.THEATER_DATA)
                    .document(Utils.INTERNAL_COMPANY_NAME)
                    .collection("Seats")
                    .whereEqualTo("showId", showId.toString())
                    ;
            List<String> times = new ArrayList<>();
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                FireStoreSeat seat = document.toObject(FireStoreSeat.class);
                if (!times.contains(seat.getTime())) {
                    times.add(seat.getTime());
                }
            }
            return times.stream()
                    .map(time -> LocalDateTime.parse(time, Utils.show_time_formatter))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time) {
        try {
            Query query = firestoreDB
                    .collection(Utils.THEATER_DATA)
                    .document(Utils.INTERNAL_COMPANY_NAME)
                    .collection("Seats")
                    .whereEqualTo("showId", showId.toString())
                    .whereEqualTo("time", time.format(Utils.show_time_formatter))
                    .whereEqualTo("available", true)
                    ;
            List<Seat> seats = new ArrayList<>();
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                seats.add(new Seat(document.toObject(FireStoreSeat.class)));
            }
            return seats;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    public Seat getSeat(UUID seatId) {
        try {
            Query query = firestoreDB
                    .collection(Utils.THEATER_DATA)
                    .document(Utils.INTERNAL_COMPANY_NAME)
                    .collection("Seats")
                    .whereEqualTo("seatId", seatId.toString())
                    ;
            List<Seat> seats = new ArrayList<>();
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                return new Seat(document.toObject(FireStoreSeat.class));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        Pair pair = fromDataToShows();
        List<Show> shows = (List<Show>) pair.first;
        Map<UUID, List<Seat>> seatsMap = (Map<UUID, List<Seat>>) pair.second;
        var showsCollections = firestoreDB
                .collection(Utils.THEATER_DATA)
                .document(Utils.INTERNAL_COMPANY_NAME)
                .collection("Shows");
        var seatsCollections = firestoreDB
                .collection(Utils.THEATER_DATA)
                .document(Utils.INTERNAL_COMPANY_NAME)
                .collection("Seats");
        boolean isInit = true;
        for(var show: shows) {
            Query query = showsCollections.whereEqualTo("name", show.getName());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            if (querySnapshot.get().getDocuments().size() == 0) {
                isInit = false;
            }
        }
        logger.info("is firestore init: " + isInit);

        if (!isInit) {
            for(Show show: shows) {
                FireStoreShow fireStoreShow = new FireStoreShow(show);
                DocumentReference showDoc = showsCollections.document(fireStoreShow.getShowId());
                ApiFuture<WriteResult> future = showDoc.set(fireStoreShow);
                logger.info("Create show " + fireStoreShow.getName() + ": " + future.get().getUpdateTime());
                List<Seat> seats = seatsMap.get(show.getShowId());
                for(Seat seat: seats) {
                    FireStoreSeat fireStoreSeat = new FireStoreSeat(seat);
                    DocumentReference seatDoc = seatsCollections.document(fireStoreSeat.getSeatId());
                    ApiFuture<WriteResult> future1 = seatDoc.set(fireStoreSeat);
                    logger.info("Create seat " + fireStoreSeat.getName() + ": " + future1.get().getUpdateTime());
                }
            }
        }
    }

    public Ticket reserveSeat(UUID seatId, String customer) throws ExecutionException, InterruptedException {
        final DocumentReference docRef = firestoreDB
                .collection(Utils.THEATER_DATA)
                .document(Utils.INTERNAL_COMPANY_NAME)
                .collection("Seats")
                .document(seatId.toString());
        ApiFuture<Seat> transactionFuture = firestoreDB.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef).get();
            Boolean isAvailable = snapshot.getBoolean("available");
            Seat seat = new Seat(snapshot.toObject(FireStoreSeat.class));

            if (isAvailable == null) {
                logger.error("Seat " + seat.getSeatId() + " not exist");
                return null;
            }
            if (!isAvailable) {
                logger.error("Seat " + seat.getSeatId() + " is already reserved");
                return null;
            }

            transaction.update(docRef, "available", false);
            return seat;
        });

        Seat seat = transactionFuture.get();
        if (seat == null) return null;
        return new Ticket(
                seat.getCompany(),
                seat.getShowId(),
                seat.getSeatId(),
                UUID.randomUUID(),
                customer
        );
    }

    public void removeReserveSeat(UUID seatId) {
        final DocumentReference docRef = firestoreDB
                .collection(Utils.THEATER_DATA)
                .document(Utils.INTERNAL_COMPANY_NAME)
                .collection("Seats")
                .document(seatId.toString());
        try {
            firestoreDB.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef).get();
                transaction.update(docRef, "available", true);
                return null;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(e.getMessage());
        }
    }

    private Pair fromDataToShows() {
        try {
            List<Show> shows = new ArrayList<>();
            String data = new String( new ClassPathResource("data.json").getInputStream().readAllBytes());
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode showsNode = (ArrayNode) mapper.readTree(data).get("shows");
            Map<UUID, List<Seat>> seatsMap = new HashMap<>();
            for(var showNode: showsNode) {
                UUID showID = UUID.randomUUID();
                List<Seat> seats = new ArrayList<>();
                ArrayNode seatsNode = (ArrayNode) showNode.get("seats");
                for(var seatNode: seatsNode) {
                    seats.add(new Seat(
                            Utils.INTERNAL_COMPANY_NAME,
                            showID,
                            UUID.randomUUID(),
                            LocalDateTime.parse(
                                    seatNode.get("time").asText(),
                                    Utils.show_time_formatter),
                            seatNode.get("type").asText(),
                            seatNode.get("name").asText(),
                            seatNode.get("price").asDouble()
                    ));
                }
                seatsMap.put(showID, seats);
                shows.add(new Show(
                        Utils.INTERNAL_COMPANY_NAME,
                        showID,
                        showNode.get("name").asText(),
                        showNode.get("location").asText(),
                        showNode.get("image").asText()
                ));
            }

            logger.info("Internal company: " + shows.size());
            return new Pair(shows, seatsMap);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return new Pair(new ArrayList<>(), new HashMap<>());
    }
}
