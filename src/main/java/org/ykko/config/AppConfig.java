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

    @Value("${aws.kds.contact-event.name}")
    private String contactEventKDSName;

    @Value("${aws.kds.contact-event.lookback-hours}")
    private Long contactEventKDSLookbackHours;

    @Value("${aws.kds.ctr-event.name}")
    private String ctrEventKDSName;

    @Value("${aws.kds.ctr-event.lookback-hours}")
    private Long ctrEventKDSLookbackHours;


}
