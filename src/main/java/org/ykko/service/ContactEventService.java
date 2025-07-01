package org.ykko.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ykko.config.AppConfig;
import org.ykko.repository.AgentEventRepository;
import org.ykko.repository.ContactEvent;
import org.ykko.repository.ContactEventRepository;
import org.ykko.util.DateUtil;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class ContactEventService {

    private final String STREAM_NAME;
    private final Long LOOKBACK_HOURS;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KinesisClient kinesisClient;

    @Autowired
    private ContactEventRepository contactEventRepository;

    public ContactEventService(AppConfig appConfig) {
        STREAM_NAME = appConfig.getContactEventKDSName();
        LOOKBACK_HOURS = appConfig.getContactEventKDSLookbackHours();
        this.kinesisClient = KinesisClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void startReadingShards() {
        if(System.getProperty("source.event") == null || !System.getProperty("source.event").equals("contact")) {
            return;
        }
        ListShardsRequest request = ListShardsRequest.builder()
                .streamName(STREAM_NAME)
                .build();

        ListShardsResponse response = kinesisClient.listShards(request);
        List<Shard> shards = response.shards();

        if (shards.isEmpty()) {
            log.warn("No shards found.");
            return;
        }

        log.info("Found {} shards. Starting reader threads...", shards.size());

        for (Shard shard : shards) {
            executor.submit(() -> {
                try {
                    readShard(shard.shardId());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void readShard(String shardId) throws Exception {
        log.info("Thread started for shard: {}", shardId);
        Instant lookbackTime = Instant.now().minus(Duration.ofHours(LOOKBACK_HOURS));
        GetShardIteratorRequest iteratorRequest = GetShardIteratorRequest.builder()
                .streamName(STREAM_NAME)
                .shardId(shardId)
                .shardIteratorType(ShardIteratorType.AT_TIMESTAMP)
                .timestamp(lookbackTime)
                .build();

        String iterator = kinesisClient.getShardIterator(iteratorRequest).shardIterator();

        while (iterator != null) {
            GetRecordsRequest recordsRequest = GetRecordsRequest.builder()
                    .shardIterator(iterator)
                    .limit(25)
                    .build();

            GetRecordsResponse recordsResponse = kinesisClient.getRecords(recordsRequest);

            for (Record record : recordsResponse.records()) {
                String data = StandardCharsets.UTF_8.decode(record.data().asByteBuffer()).toString();
                //log.info(data);
                //log.info("");
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(data);
                String id = rootNode.get("id").asText();
                if(contactEventRepository.existsById(id)) {
                    log.info("Contact event with ID {} already exists, skipping.", id);
                    continue;
                }
                log.info("1");
                JsonNode detailNode = rootNode.get("detail");
                log.info("2");
                String eventType = detailNode.get("eventType").asText();
                log.info("3");
                String contactId = detailNode.get("contactId").asText();
                log.info("4");
                String channel = detailNode.get("channel").asText();
                log.info("5");
                String initialContactId = (detailNode.get("initialContactId") == null)?"":detailNode.get("initialContactId").asText();
                log.info("6");
                String previousContactId = (detailNode.get("previousContactId") == null)?"":detailNode.get("previousContactId").asText();
                log.info("7");
                String initiationMethod = (detailNode.get("initiationMethod") == null)?"":detailNode.get("initiationMethod").asText();
                log.info("8");
                String initiationTimestamp = (detailNode.get("initiationTimestamp") == null)?"":detailNode.get("initiationTimestamp").asText();
                log.info("9");
                String connectedToSystemTimestamp = (detailNode.get("connectedToSystemTimestamp") == null)?"":detailNode.get("connectedToSystemTimestamp").asText();
                log.info("10");
                String disconnectTimestamp = (detailNode.get("disconnectTimestamp") == null)?"":detailNode.get("disconnectTimestamp").asText();
                log.info("11");
                JsonNode agentInfo = detailNode.get("agentInfo");
                String agentArn = null;
                if(agentInfo != null) {
                    agentArn = agentInfo.get("agentArn").asText();
                }
                ContactEvent event = new ContactEvent();
                event.setId(id);
                event.setShardId(shardId);
                event.setEventType(eventType);
                event.setContactId(contactId);
                event.setChannel(channel);
                event.setInitialContactId(initialContactId);
                event.setPreviousContactId(previousContactId);
                event.setInitiationMethod(initiationMethod);
                event.setInitiationTimestamp(initiationTimestamp);
                event.setConnectedToSystemTimestamp(connectedToSystemTimestamp);
                event.setDisconnectTimestamp(disconnectTimestamp);
                event.setAgentArn(agentArn);
                event.setFullData(data);
                log.info("before saving...");
                contactEventRepository.save(event);
                log.info("Saved contact event with ID: {}", id);
            }

            iterator = recordsResponse.nextShardIterator();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Shard {} thread interrupted.", shardId);
                break;
            }
        }
        log.info("Shard {} thread exiting.", shardId);
    }
}
