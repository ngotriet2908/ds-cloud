package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Model {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private List<Booking> bookings = new ArrayList<>();

    public List<Show> getShows() {
//        TODO: browse to all of the companies and group the results
        return webClientBuilder
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
    }

    public Show getShow(String company, UUID showId) {
        // TODO: check company to use appropriate URL
        return webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString())
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        // TODO: return a list with all possible times for the given show
        var showTimesString =  webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "times")
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<String>>() {})
                .block()
                .getContent();

        return showTimesString.stream()
                .map(string -> LocalDateTime.parse(string, Utils.show_time_formatter))
                .collect(Collectors.toList());

    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {

        return webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
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
                .block()
                .getContent().stream().collect(Collectors.toList());
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        // TODO: return the given seat
        System.out.println(seatId);
        return webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString())
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .block();
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        return webClientBuilder
                .baseUrl("https://reliabletheatrecompany.com/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString(), "ticket")
                        .queryParam("key", Utils.API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
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
        List<Ticket> tmpTickets = new ArrayList<>();
        boolean success = true;
        try {
            for(var quote: quotes) {
                var ticket = webClientBuilder
                        .baseUrl("https://reliabletheatrecompany.com/")
                        .build()
                        .put()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows", quote.getShowId().toString(), "seats", quote.getSeatId().toString(), "ticket")
                                .queryParam("key", Utils.API_KEY)
                                .queryParam("customer", customer)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
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
                        .baseUrl("https://reliabletheatrecompany.com/")
                        .build()
                        .put()
                        .uri(uriBuilder -> uriBuilder
                                .pathSegment("shows", ticket.getShowId().toString(), "seats", ticket.getSeatId().toString(), "ticket")
                                .queryParam("key", Utils.API_KEY)
                                .queryParam("customer", customer)
                                .build())
                        .retrieve().bodyToMono(Void.class);
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
