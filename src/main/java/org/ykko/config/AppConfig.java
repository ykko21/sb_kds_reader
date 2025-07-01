package org.ykko.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.kds.agent-event.name}")
    private String agentEventKDSName;

    @Value("${aws.kds.agent-event.lookback-hours}")
    private Long agentEventKDSLookbackHours;
}
