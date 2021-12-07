package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class Model {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private PubSubComponent pubSubComponent;

    @Autowired
    private InternalCompanyComponent internalCompany;

    @Autowired
    private Firestore firestoreDB;

    private static final int MAX_ATTEMPTS = 10;
    private static final int DELAY_BETWEEN_ATTEMPTS = 2;

    private static final String BOOKINGS = "Bookings";

    Logger logger = LoggerFactory.getLogger(Model.class);

    public List<Show> getShows() {
        List<Show> allShows = new ArrayList<>();

        for (String API_LOCATION :
             Utils.API_LOCATIONS) {
            try {
                List<Show> showsForAPI = webClientBuilder
                        .baseUrl("https://" + API_LOCATION + "/")
                        .build()
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows")
                                .queryParam("key", Utils.API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                        .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                        .block()
                        .getContent()
                        .stream().collect(Collectors.toList());

                allShows.addAll(showsForAPI);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        allShows.addAll(internalCompany.getShows());
        return allShows;
    }

    public Show getShow(String company, UUID showId) {
        if (company.equals(Utils.INTERNAL_COMPANY_NAME)) {
            return internalCompany.getShow(showId);
        }

        return webClientBuilder
                .baseUrl("https://" + company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString())
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block();
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        if (company.equals(Utils.INTERNAL_COMPANY_NAME)) {
            return internalCompany.getShowTimes(showId);
        }

        var showTimesString =  webClientBuilder
                .baseUrl("https://" + company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "times")
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block()
                .getContent();

        return showTimesString.stream()
                .map(string -> LocalDateTime.parse(string, Utils.show_time_formatter))
                .collect(Collectors.toList());
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        if (company.equals(Utils.INTERNAL_COMPANY_NAME)) {
            return internalCompany.getAvailableSeats(showId, time);
        }

        return webClientBuilder
                .baseUrl("https://" + company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats")
                        .queryParam("key", Utils.API_KEY)
                        .queryParam("time", time.toString())
                        .queryParam("available", "true")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block()
                .getContent().stream().collect(Collectors.toList());
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        if (company.equals(Utils.INTERNAL_COMPANY_NAME)) {
            return internalCompany.getSeat(seatId);
        }

        return webClientBuilder
                .baseUrl("https://" + company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString())
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block();
    }

//    public Ticket getTicket(String company, UUID showId, UUID seatId) {
//        return webClientBuilder
//                .baseUrl("https://" + company)
//                .build()
//                .get()
//                .uri(uriBuilder -> uriBuilder
//                        .pathSegment("shows", showId.toString(), "seats", seatId.toString(), "ticket")
//                        .queryParam("key", Utils.API_KEY)
//                        .build())
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
//                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
//                .block();
//    }


    public List<Booking> getBookings(String customer) {
        try {
            List<Booking> bookings = new ArrayList<>();
            Query query = firestoreDB.collection(BOOKINGS).whereEqualTo("customer", customer);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                bookings.add(new Booking(document.toObject(FireStoreBooking.class)));
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Booking> getAllBookings() {
        try {
            List<Booking> bookings = new ArrayList<>();
            Query query = firestoreDB.collection(BOOKINGS);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                bookings.add(new Booking(document.toObject(FireStoreBooking.class)));
            }
            return bookings;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Set<String> getBestCustomers() {
        Map<String, Integer> ticketCounter = new HashMap<>();
        for(var booking: getAllBookings()) {
            if (!ticketCounter.containsKey(booking.getCustomer())) {
                ticketCounter.put(booking.getCustomer(), booking.getTickets().size());
            } else {
                ticketCounter.put(booking.getCustomer(), ticketCounter.get(booking.getCustomer()) + booking.getTickets().size());
            }
        }
        int maxTicketCount = 0;
        for (var ticketCount :ticketCounter.entrySet()) {
            if (ticketCount.getValue() > maxTicketCount) {
                maxTicketCount = ticketCount.getValue();
            }
        }

        int finalMaxTicketCount = maxTicketCount;
        return ticketCounter
                .keySet()
                .stream()
                .filter(customer -> ticketCounter.get(customer) == finalMaxTicketCount)
                .collect(Collectors.toSet());
    }

    public void confirmQuotes(List<Quote> quotes, String customer) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
        out.writeObject(quotes);
        out.close();
        byteArrayOutputStream.close();

        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(byteArrayOutputStream.toByteArray()))
                .putAttributes("customer", customer)
                .build();
        logger.info("Publishing via " + this.pubSubComponent.getPublisher().getTopicNameString());
        ApiFuture<String> future = this.pubSubComponent.getPublisher().publish(pubsubMessage);
    }

    public void confirmQuotesHelper(List<Quote> quotes, String customer) {
        List<Ticket> tmpTickets = new ArrayList<>();
        boolean confirmedAllQuotes = true;
        try {
            for(var quote: quotes) {
                if (quote.getCompany().equals(Utils.INTERNAL_COMPANY_NAME)) {
                    Ticket ticket = internalCompany.reserveSeat(quote.getSeatId(), customer);
                    if (ticket != null) {
                        tmpTickets.add(ticket);
                        continue;
                    } else {
                        throw new Exception("Seat is not available");
                    }
                }

                var ticket = webClientBuilder
                        .baseUrl("https://" + quote.getCompany())
                        .build()
                        .put()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows", quote.getShowId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                                .queryParam("key", Utils.API_KEY)
                                .queryParam("customer", customer)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                        .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                        .block();

                tmpTickets.add(ticket);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            confirmedAllQuotes = false;
        }

        if (!confirmedAllQuotes) {
            for (var ticket : tmpTickets) {
                if (ticket.getCompany().equals(Utils.INTERNAL_COMPANY_NAME)) {
                    internalCompany.removeReserveSeat(ticket.getSeatId());
                    continue;
                }

                webClientBuilder
                        .baseUrl("https://" + ticket.getCompany())
                        .build()
                        .put()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows", ticket.getShowId().toString(), "seats", ticket.getSeatId().toString(), "ticket")
                                .queryParam("key", Utils.API_KEY)
                                .queryParam("customer", customer)
                                .build())
                        .retrieve()
                        .bodyToMono(Void.class)
                        .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)));
            }
        } else {
            Booking booking = new Booking(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    tmpTickets,
                    customer
            );
            FireStoreBooking fireStoreBooking = new FireStoreBooking(booking);

            CollectionReference bookingCollection = firestoreDB.collection(BOOKINGS);
            DocumentReference bookingDoc = bookingCollection.document(fireStoreBooking.getId());
            ApiFuture<WriteResult> future = bookingDoc.set(fireStoreBooking);
            try {
                System.out.println("Update time : " + future.get().getUpdateTime());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
