package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import javax.annotation.Resource;
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

    private List<Booking> bookings = new ArrayList<>();

    private static final int MAX_ATTEMPTS = 10;
    private static final int DELAY_BETWEEN_ATTEMPTS = 2;

    Logger logger = LoggerFactory.getLogger(Model.class);

//    @Resource(name = "getPublisher")
//    private Publisher publisher;

    public List<Show> getShows() {
//        TODO: browse to all of the companies and group the results
        List<Show> allShows = new ArrayList<>();
        try {
            List<Show> reliable = webClientBuilder
                    .baseUrl("https://reliabletheatrecompany.com/")
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows")
                            .queryParam("key", Utils.API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                    .block()
                    .getContent()
                    .stream().collect(Collectors.toList());

            List<Show> unreliable = webClientBuilder
                    .baseUrl("https://unreliabletheatrecompany.com/")
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

            allShows.addAll(reliable);
            allShows.addAll(unreliable);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allShows;
    }

    public Show getShow(String company, UUID showId) {
        // TODO: check company to use appropriate URL
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
        // TODO: return a list with all possible times for the given show
        System.out.println(company);
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
        // TODO: return the given seat
        System.out.println(seatId);
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

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        return webClientBuilder
                .baseUrl("https://" + company)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString(), "ticket")
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block();
    }

    public List<Booking> getBookings(String customer) {

        return bookings
                .stream()
                .filter(booking -> booking.getCustomer().equals(customer))
                .collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Set<String> getBestCustomers() {
        Map<String, Integer> ticketCounter = new HashMap<>();
        for(var booking: bookings) {
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

    public void confirmQuotes(List<Quote> quotes, String customer) {
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
//                    .setData(ByteString.copyFromUtf8(quotes.toString()))
                .setData(ByteString.copyFromUtf8("hahaha"))
                .putAttributes("customer", customer)
                .build();
        logger.info("Published via " + this.pubSubComponent.getPublisher().getTopicNameString() + ": " + pubsubMessage);
        ApiFuture<String> future = this.pubSubComponent.getPublisher().publish(pubsubMessage);
        ApiFutures.addCallback(
                future,
                new ApiFutureCallback<String>() {

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException) {
                            ApiException apiException = ((ApiException) throwable);
                            // details on the API exception
                            logger.error(String.valueOf(apiException.getStatusCode().getCode()));
                            logger.error(String.valueOf(apiException.isRetryable()));
                        }
                        throwable.printStackTrace();
                        logger.error("Error publishing message : " + throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(String messageId) {
                        // Once published, returns server-assigned message ids (unique within the topic)
                        logger.info("Published message ID: " + messageId);
                    }
                },
                MoreExecutors.directExecutor());
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void confirmQuotesHelper(List<Quote> quotes, String customer) {
        List<Ticket> tmpTickets = new ArrayList<>();
        boolean success = true;
        try {
            for(var quote: quotes) {
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
            success = false;
        }


//        TODO: ensure all or nothing by removing the tickets
        if (!success) {
            for (var ticket : tmpTickets) {
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
            bookings.add(new Booking(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    tmpTickets,
                    customer
            ));
        }
    }
}
