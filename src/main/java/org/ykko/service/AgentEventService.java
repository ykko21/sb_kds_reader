package org.ykko.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ykko.config.AppConfig;
import org.ykko.repository.AgentEvent;
import org.ykko.repository.AgentEventRepository;
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
public class AgentEventService {

    private final String STREAM_NAME;
    private final Long LOOKBACK_HOURS;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KinesisClient kinesisClient;

    @Autowired
    private AgentEventRepository agentEventRepository;

    public AgentEventService(AppConfig appConfig) {
        STREAM_NAME = appConfig.getAgentEventKDSName();
        LOOKBACK_HOURS = appConfig.getAgentEventKDSLookbackHours();
        this.kinesisClient = KinesisClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void startReadingShards() {
        if(System.getProperty("source.event") == null || !System.getProperty("source.event").equals("agent")) {
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(data);
                String eventId = rootNode.get("EventId").asText();
                String eventType = rootNode.get("EventType").asText();
                if(eventType.equals("HEART_BEAT")) {
                    continue;
                }
                if(agentEventRepository.existsById(eventId) ) {
                    log.info("EventId {} already exists in the database, skipping.", eventId);
                    continue;
                }
                String eventTimestamp = rootNode.get("EventTimestamp").asText();
                Long eventUnixTimestamp = DateUtil.convertISOTimestampToUnixTimestamp(eventTimestamp);
                JsonNode currentAgentSnapshotNode = rootNode.get("CurrentAgentSnapshot");
                JsonNode agentStatusNode = currentAgentSnapshotNode.get("AgentStatus");
                String agentStatus = agentStatusNode.get("Type").asText();
                JsonNode configurationNode = currentAgentSnapshotNode.get("Configuration");
                String username = configurationNode.get("Username").asText();
                ArrayNode contactsNode = (ArrayNode)currentAgentSnapshotNode.get("Contacts");
                String contactId = null;
                String initContactId = null;
                String initMethod = null;
                String contactState = null;
                String contactQueue = null;
                String contactChannel = null;
                if(contactsNode != null && !contactsNode.isEmpty()) {
                    JsonNode contactNode = contactsNode.get(0);
                    contactId = contactNode.get("ContactId").asText();
                    initContactId = contactNode.get("InitialContactId").asText();
                    initMethod = contactNode.get("InitiationMethod").asText();
                    contactState = contactNode.get("State").asText();
                    JsonNode contactQueueNode = contactNode.get("Queue");
                    contactQueue = contactQueueNode.get("Name").asText();
                    contactChannel = contactNode.get("Channel").asText();
                }
                AgentEvent agentEvent = new AgentEvent();
                agentEvent.setEventId(eventId);
                agentEvent.setShardId(shardId);
                agentEvent.setUsername(username);
                agentEvent.setAgentStatus(agentStatus);
                agentEvent.setEventType(eventType);
                agentEvent.setContactId(contactId);
                agentEvent.setInitContactId(initContactId);
                agentEvent.setInitMethod(initMethod);
                agentEvent.setContactQueue(contactQueue);
                agentEvent.setContactState(contactState);
                agentEvent.setContactChannel(contactChannel);
                agentEvent.setEventTimestamp(eventTimestamp);
                agentEvent.setEventUnixTimestamp(eventUnixTimestamp);
                agentEvent.setFullData(data);
                agentEventRepository.save(agentEvent);
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