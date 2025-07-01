package org.ykko.repository;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="contact_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactEvent {

    @Id
    @Column(name="id")
    private String id;

    @Column(name="shard_id")
    private String shardId;

    @Column(name="event_type")
    String eventType;

    @Column(name="contact_id")
    String contactId;

    @Column(name="channel")
    String channel;

    @Column(name="init_contact_id")
    String initialContactId;

    @Column(name="prev_contact_id")
    String previousContactId;

    @Column(name="init_method")
    String initiationMethod;

    @Column(name="init_timestamp")
    String initiationTimestamp;

    @Column(name="conn_to_sys_timestamp")
    String connectedToSystemTimestamp;

    @Column(name="disconn_timestamp")
    String disconnectTimestamp;

    @Column(name="agent_arn")
    String agentArn;

    @Lob
    @Column(name="full_data", columnDefinition = "CLOB")
    private String fullData;
}
