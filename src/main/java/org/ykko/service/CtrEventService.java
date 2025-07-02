package org.ykko.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ykko.config.AppConfig;
import org.ykko.repository.ContactEvent;
import org.ykko.repository.ContactEventRepository;
import org.ykko.repository.CtrEvent;
import org.ykko.repository.CtrEventRepository;
import org.ykko.util.JsonUtil;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class CtrEventService {

    private final String STREAM_NAME;
    private final Long LOOKBACK_HOURS;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KinesisClient kinesisClient;

    @Autowired
    private CtrEventRepository ctrEventRepository;

    public CtrEventService(AppConfig appConfig) {
        STREAM_NAME = appConfig.getCtrEventKDSName();
        LOOKBACK_HOURS = appConfig.getCtrEventKDSLookbackHours();
        this.kinesisClient = KinesisClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void startReadingShards() {
        if(System.getProperty("source.event") == null || !System.getProperty("source.event").equals("ctr")) {
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
                JsonNode agentNode = rootNode.get("Agent");
                String id = UUID.randomUUID().toString();
                String agentUsername = null;
                String agentArn = null;
                if(agentNode != null) {
                    agentUsername = JsonUtil.getValue(agentNode, "Username");
                    agentArn = JsonUtil.getValue(agentNode, "ARN");
                }

                String channel = JsonUtil.getValue(rootNode, "Channel");
                String connectedToSystemTimestamp = JsonUtil.getValue(rootNode, "ConnectedToSystemTimestamp");
                String contactId = JsonUtil.getValue(rootNode, "ContactId");
                String disconnectReason = JsonUtil.getValue(rootNode, "DisconnectReason");
                String disconnectTimestamp = JsonUtil.getValue(rootNode, "DisconnectTimestamp");
                String initialContactId = JsonUtil.getValue(rootNode, "InitialContactId");
                String initiationMethod = JsonUtil.getValue(rootNode, "InitiationMethod");
                String initiationTimestamp = JsonUtil.getValue(rootNode, "InitiationTimestamp");
                String previousContactId = JsonUtil.getValue(rootNode, "PreviousContactId");
                String transferCompletedTimestamp = JsonUtil.getValue(rootNode, "TransferCompletedTimestamp");
                Optional<CtrEvent> optional = ctrEventRepository.findByAgentUsernameAndContactIdAndInitialContactIdAndPreviousContactId(agentUsername, contactId, initialContactId, previousContactId);

                if(optional.isPresent()) {
                    log.warn("Duplicate event found for agentUsername:{},contactId:{},initialContactId:{},previousContactId:{}. Skipping.", agentUsername, contactId, initialContactId, previousContactId);
                    continue;
                } else {
                    CtrEvent ctrEvent = new CtrEvent();
                    ctrEvent.setId(id);
                    ctrEvent.setShardId(shardId);
                    ctrEvent.setAgentUsername(agentUsername);
                    ctrEvent.setAgentArn(agentArn);
                    ctrEvent.setChannel(channel);
                    ctrEvent.setConnectedToSystemTimestamp(connectedToSystemTimestamp);
                    ctrEvent.setContactId(contactId);
                    ctrEvent.setDisconnectReason(disconnectReason);
                    ctrEvent.setDisconnectTimestamp(disconnectTimestamp);
                    ctrEvent.setInitialContactId(initialContactId);
                    ctrEvent.setInitiationMethod(initiationMethod);
                    ctrEvent.setInitiationTimestamp(initiationTimestamp);
                    ctrEvent.setPreviousContactId(previousContactId);
                    ctrEvent.setTransferCompletedTimestamp(transferCompletedTimestamp);
                    ctrEvent.setFullData(data);
                    // Save to repository
                    ctrEventRepository.save(ctrEvent);
                    log.info("Saved event for contactId: {}", contactId);
                }
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
