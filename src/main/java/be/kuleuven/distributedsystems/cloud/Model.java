package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import be.kuleuven.distributedsystems.cloud.company.ITheatreCompany;
import be.kuleuven.distributedsystems.cloud.company.InternalTheatreCompany;
import be.kuleuven.distributedsystems.cloud.company.TheatreCompany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import com.sendgrid.*;

import javax.annotation.PostConstruct;
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
    private InternalTheatreCompany internalCompany;

    @Autowired
    private Firestore firestoreDB;

    private List<ITheatreCompany> theatreCompanies;

    private static final int MAX_ATTEMPTS = 10;
    private static final int DELAY_BETWEEN_ATTEMPTS = 2;

    private static final String BOOKINGS = "Bookings";

    Logger logger = LoggerFactory.getLogger(Model.class);

    @PostConstruct
    public void init(){
        this.theatreCompanies = new LinkedList<>();
        for(String API_LOCATION : Utils.API_LOCATIONS){
            this.theatreCompanies.add(new TheatreCompany(API_LOCATION, webClientBuilder));
        }
        theatreCompanies.add(internalCompany);
    }

    public List<Show> getShows() {
        List<Show> allShows = new ArrayList<>();

        try {
            theatreCompanies.forEach((theatreCompany -> allShows.addAll(theatreCompany.getShows()) ));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return allShows;
    }

    public Show getShow(String company, UUID showId) {
        for (ITheatreCompany theatreCompany : theatreCompanies) {
            if(theatreCompany.getCompanyName().equals(company)){
                return  theatreCompany.getShow(showId);
            }
        }
        return null;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        for (ITheatreCompany theatreCompany : theatreCompanies) {
            if(theatreCompany.getCompanyName().equals(company)){
                return  theatreCompany.getShowTimes(showId);
            }
        }
        return null;
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        for (ITheatreCompany theatreCompany : theatreCompanies) {
            if(theatreCompany.getCompanyName().equals(company)){
                return  theatreCompany.getAvailableSeats(showId, time);
            }
        }
        return null;
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        for (ITheatreCompany theatreCompany : theatreCompanies) {
            if(theatreCompany.getCompanyName().equals(company)){
                return  theatreCompany.getSeat(showId,seatId);
            }
        }
        return null;
    }

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
                .putAttributes("email","stijn.martens@student.kuleuven.be")
                .build();

        logger.info("Publishing via " + this.pubSubComponent.getPublisher().getTopicNameString());
        ApiFuture<String> future = this.pubSubComponent.getPublisher().publish(pubsubMessage);
    }

    public void confirmQuotesHelper(List<Quote> quotes, String customer) {
        List<Ticket> tmpTickets = new ArrayList<>();
        Ticket ticket = null;
        boolean confirmedAllQuotes = true;
        try {
            for(var quote: quotes) {
                for (ITheatreCompany theatreCompany : theatreCompanies) {
                    if(theatreCompany.getCompanyName().equals(quote.getCompany())){
                        ticket = theatreCompany.reserveSeat(quote.getShowId(), quote.getSeatId(), customer);
                    }
                }
                if (ticket == null)
                    throw new Exception("Seat is not available");
                tmpTickets.add(ticket);
                ticket = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            confirmedAllQuotes = false;
        }

        if (!confirmedAllQuotes) {
            for (Ticket reservedTicket : tmpTickets) {
                for (ITheatreCompany theatreCompany : theatreCompanies) {
                    if(theatreCompany.getCompanyName().equals(reservedTicket.getCompany())){
                        theatreCompany.removeReserveSeat(reservedTicket.getShowId(), reservedTicket.getSeatId(), customer);
                    }
                }
            }
        } else {
            Booking booking = new Booking(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    tmpTickets,
                    customer
            );

            FireStoreBooking fireStoreBooking = new FireStoreBooking(booking);

            sendFeedback(customer,"Test");

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

    void sendFeedback(String email , String message){

        Email from = new Email("stijn.martens@student.kuleuven.be");
        String subject = "Sending with Twilio SendGrid is Fun";
        Email to = new Email(email);
        Content content = new Content("text/plain", "and easy to do anywhere, even with Java");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(Utils.SENDGRID_API);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            logger.error(ex.toString());
        }
    }

}
