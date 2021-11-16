package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class PubSubComponent {

    Logger logger = LoggerFactory.getLogger(PubSubComponent.class);

    @Autowired
    private Environment environment;

    private Publisher publisher;

    private static final String SUBSCRIPTION_ID = "confirm-quote";
    private static final String PUSH_ENDPOINT = "http://localhost:8080/" + SUBSCRIPTION_ID;

    @PostConstruct
    public void init() {
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        try {
            TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

            // Set the channel and credentials provider when creating a `TopicAdminClient`.
            // Similarly for SubscriptionAdminClient
            TopicAdminClient topicClient =
                    TopicAdminClient.create(
                            TopicAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());

            TopicName topicName = TopicName.of(Utils.PROJECT_ID, Utils.TOPIC_ID);
//            topicClient.deleteTopic(topicName);
            try {
                topicClient.createTopic(topicName);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            logger.info("created topic " + topicClient.getTopic(topicName).getName());

            SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient
                    .create(
                            SubscriptionAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());

            PushConfig pushConfig = PushConfig.newBuilder()
                    .setPushEndpoint(PubSubComponent.PUSH_ENDPOINT)
                    .build();

            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(Utils.PROJECT_ID, SUBSCRIPTION_ID);

            try {
                Subscription subscription =
                        subscriptionAdminClient.createSubscription(
                                subscriptionName,
                                topicName,
                                pushConfig,
                                60);
                logger.info("Created push subscription: " + subscription.getName());
            } catch (Exception e) {
                logger.error(e.getMessage());
//                e.printStackTrace();
            }

        } catch (Exception e) {
//            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    public Publisher getPublisher() throws IOException {
        String hostport = "localhost:8083";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
        TransportChannelProvider channelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();
//        logger.info("created publisher " + publisher.getTopicNameString());
        return Publisher.newBuilder(TopicName.of(Utils.PROJECT_ID, Utils.TOPIC_ID))
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();

    }
}
