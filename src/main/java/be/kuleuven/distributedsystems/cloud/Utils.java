package be.kuleuven.distributedsystems.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;

public class Utils {
    public static String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    public static String[] API_LOCATIONS = {"reliabletheatrecompany.com", "unreliabletheatrecompany.com"};
    public static ObjectMapper json_mapper = new ObjectMapper();
    public static DateTimeFormatter show_time_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static String PROJECT_ID = "demo-distributed-systems-kul";
    public static String TOPIC_ID = "confirm-quote";
}
