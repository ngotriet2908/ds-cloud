package be.kuleuven.distributedsystems.cloud.company;

import be.kuleuven.distributedsystems.cloud.Utils;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Show;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TheatreCompany implements ITheatreCompany {

    private WebClient.Builder webClientBuilder;

    private static final int MAX_ATTEMPTS = 10;
    private static final int DELAY_BETWEEN_ATTEMPTS = 2;
    private final String API_LOCATION;

    public TheatreCompany(String API_LOCATION, WebClient.Builder webClientBuilder){
        this.API_LOCATION = API_LOCATION;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public List<Show> getShows() {
        return webClientBuilder
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
    }

    @Override
    public Show getShow(UUID showId) {
        return webClientBuilder
                .baseUrl("https://" + API_LOCATION)
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

    @Override
    public List<LocalDateTime> getShowTimes(UUID showId) {
        var showTimesString =  webClientBuilder
                .baseUrl("https://" + API_LOCATION)
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

    @Override
    public List<Seat> getAvailableSeats(UUID showId, LocalDateTime time) {
        return webClientBuilder
                .baseUrl("https://" + API_LOCATION)
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

    @Override
    public String getCompanyName() {
        return this.API_LOCATION;
    }

    @Override
    public Seat getSeat(UUID showId, UUID seatId) {
        return webClientBuilder
                .baseUrl("https://" + API_LOCATION)
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

    @Override
    public Ticket reserveSeat(UUID showId, UUID seatId, String customer) {
        return webClientBuilder
                .baseUrl("https://" + API_LOCATION)
                .build()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString(), "ticket")
                        .queryParam("key", Utils.API_KEY)
                        .queryParam("customer", customer)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)))
                .block();
    }

    @Override
    public void removeReserveSeat(UUID showId, UUID seatId, String customer) {
        webClientBuilder
                .baseUrl("https://" + API_LOCATION)
                .build()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows", showId.toString(), "seats", seatId.toString(), "ticket")
                        .queryParam("key", Utils.API_KEY)
                        .queryParam("customer", customer)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.fixedDelay(MAX_ATTEMPTS, Duration.ofSeconds(DELAY_BETWEEN_ATTEMPTS)));
    }

}
