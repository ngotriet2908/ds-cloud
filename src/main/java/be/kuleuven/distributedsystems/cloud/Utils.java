package be.kuleuven.distributedsystems.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;

public class Utils {
    public static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    public static String SENDGRID_API = "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";
    public static String[] API_LOCATIONS = {"reliabletheatrecompany.com", "unreliabletheatrecompany.com"};
    public static ObjectMapper json_mapper = new ObjectMapper();
    public static DateTimeFormatter show_time_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static String PROJECT_ID = "ds-2-cloud";
    public static String TOPIC_ID = "confirm-quote";
    public static String INTERNAL_COMPANY_NAME = "Internal Company";
    public static String THEATER_DATA = "Theater data";
}
