package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Show;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebClientConfigurer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
public class Application {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.setProperty("server.port", System.getenv().getOrDefault("PORT", "8080"));

        // Apache JSP scans by default all JARs, which is not necessary, so disable it
        System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*.jar");
        System.setProperty(org.apache.tomcat.util.scan.Constants.SCAN_JARS_PROPERTY, "taglibs-standard-spec-*.jar,taglibs-standard-impl-*.jar");

        // Start Spring Boot application
        ApplicationContext context = SpringApplication.run(Application.class, args);
    }

    @Bean
    public boolean isProduction() {
        return Objects.equals(System.getenv("GAE_ENV"), "standard");
    }

    @Bean
    public String projectId() {
        return "ds-2-cloud";
    }

    /*
     * You can use this builder to create a Spring WebClient instance which can be used to make REST-calls.
     */
    @Bean
    WebClient.Builder webClientBuilder(HypermediaWebClientConfigurer configurer) {
        return configurer.registerHypermediaTypes(WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()))
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)));
    }

    @Bean
    HttpFirewall httpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public Firestore firestoreDB() {
        if (isProduction()){
            try {
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                return FirestoreOptions
                        .getDefaultInstance()
                        .toBuilder()
                        .setCredentials(credentials)
                        .setProjectId(Utils.PROJECT_ID)
                        .build()
                        .getService();
            }
            catch (Exception e) {
                return null;
            }

        }
        else {
            String hostport = "localhost:8084";
            return FirestoreOptions
                    .getDefaultInstance()
                    .toBuilder()
                    .setProjectId(this.projectId())
                    .setHost(hostport)
                    .setCredentials(new FirestoreOptions.EmulatorCredentials())
                    .setCredentialsProvider(FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
                    .build()
                    .getService();
        }



     }
}
