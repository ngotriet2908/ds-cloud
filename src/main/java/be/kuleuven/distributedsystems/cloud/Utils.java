package be.kuleuven.distributedsystems.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;

public class Utils {
    public static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    public static String SENDGRID_API = "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";
    public static String[] API_LOCATIONS = {"reliabletheatrecompany.com", "unreliabletheatrecompany.com"};
    public static ObjectMapper json_mapper = new ObjectMapper();
    public static DateTimeFormatter show_time_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static String TOPIC_ID = "confirm-quote";
    public static String INTERNAL_COMPANY_NAME = "Internal Company";
    public static String THEATER_DATA = "Theater data";
}
