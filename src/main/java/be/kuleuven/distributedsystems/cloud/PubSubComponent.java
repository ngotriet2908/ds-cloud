package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bouncycastle.math.raw.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class PubSubComponent {

    Logger logger = LoggerFactory.getLogger(PubSubComponent.class);

    @Autowired
    private Application context;

    private Publisher publisher;

    private static final String SUBSCRIPTION_ID = "confirm-quote";
    private static final String PUSH_ENDPOINT = "http://localhost:8080/" + SUBSCRIPTION_ID;

    @PostConstruct
    public void init() {
        boolean isProduction = context.isProduction();
        if(isProduction){
            initProduction();
        }
        else{
            initTesting();
        }
    }

    private void initProduction(){
        try {
            TopicName topicName = TopicName.of(Utils.PROJECT_ID, Utils.TOPIC_ID);
            publisher = Publisher.newBuilder(topicName).build();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void initTesting(){
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

            try {
                topicClient.createTopic(topicName);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            logger.info("created topic " + topicClient.getTopic(topicName).getName());

            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            this.publisher =
                    Publisher.newBuilder(topicName)
                            .setChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build();

            logger.info("created publisher " + publisher.getTopicNameString());


            SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient
                    .create(
                            SubscriptionAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                                    .setCredentialsProvider(credentialsProvider)
                                    .build());

            PushConfig pushConfig = PushConfig.newBuilder()
                    .setPushEndpoint(PubSubComponent.PUSH_ENDPOINT)
                    .build();

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(Utils.PROJECT_ID, SUBSCRIPTION_ID);

            try {
                Subscription subscription =
                        subscriptionAdminClient.createSubscription(subscriptionName, publisher.getTopicName(), pushConfig, 10);
                logger.info("Created push subscription: " + subscription.getName());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public Publisher getPublisher() {
        return publisher;
    }
}