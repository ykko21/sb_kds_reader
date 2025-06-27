package org.ykko.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.*;
import software.amazon.awssdk.services.kinesis.model.Record;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class KinesisService {

    private static final String STREAM_NAME = "aws-connect-agent-events";
    private static final Region REGION = Region.US_EAST_1;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final KinesisClient kinesisClient;

    public KinesisService() {
        this.kinesisClient = KinesisClient.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @PostConstruct
    public void startReadingShards() {
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
            executor.submit(() -> readShard(shard.shardId()));
        }
    }

    private void readShard(String shardId) {
        log.info("Thread started for shard: {}", shardId);

        GetShardIteratorRequest iteratorRequest = GetShardIteratorRequest.builder()
                .streamName(STREAM_NAME)
                .shardId(shardId)
                .shardIteratorType(ShardIteratorType.LATEST)
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
                log.info("[Shard {}] Record: {}", shardId, data);
            }

            iterator = recordsResponse.nextShardIterator();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.info("Shard {} thread interrupted.", shardId);
                break;
            }
        }

        log.info("Shard {} thread exiting.", shardId);
    }
}