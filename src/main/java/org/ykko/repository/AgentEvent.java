package org.ykko.repository;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="agent_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {

    @Id
    @Column(name="event_id")
    private String eventId;

    @Column(name="shard_id")
    private String shardId;

    @Column(name="username")
    private String username;

    @Column(name="agent_arn")
    private String agentArn;

    @Column(name="agent_status")
    private String agentStatus;

    @Column(name="event_type")
    private String eventType;

    @Column(name="contact_id")
    private String contactId;

    @Column(name="init_contact_id")
    private String initContactId;

    @Column(name="init_method")
    private String initMethod;

    @Column(name="contact_queue")
    private String contactQueue;

    @Column(name="contact_state")
    private String contactState;

    @Column(name="contact_channel")
    private String contactChannel;

    @Column(name="event_timestamp")
    private String eventTimestamp;

    @Column(name="event_unix_timestamp")
    private Long eventUnixTimestamp;

    @Lob
    @Column(name="full_data", columnDefinition = "CLOB")
    private String fullData;
}
