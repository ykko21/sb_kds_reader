package org.ykko.repository;

import jakarta.persistence.*;
import lombok.*;
import org.ykko.util.JsonUtil;

import java.util.UUID;

@Entity
@Table(name="ctr_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtrEvent {

    @Id
    @Column(name="id")
    private String id;

    @Column(name="shard_id")
    private String shardId;

    @Column(name="agent_username")
    private String agentUsername;

    @Column(name="agent_arn")
    private String agentArn;

    @Column(name="channel")
    private String channel;

    @Column(name="conn_to_sys_timestamp")
    private String connectedToSystemTimestamp;

    @Column(name="contact_id")
    private String contactId;

    @Column(name="disconn_reason")
    private String disconnectReason;

    @Column(name="disconn_timestamp")
    private String disconnectTimestamp;

    @Column(name="init_contact_id")
    private String initialContactId;

    @Column(name="init_method")
    private String initiationMethod;

    @Column(name="init_timestamp")
    private String initiationTimestamp;

    @Column(name="prev_contact_id")
    private String previousContactId;

    @Column(name="last_update_timestamp")
    private String lastUpdateTimestamp;

    @Column(name="transf_compl_timestamp")
    private String transferCompletedTimestamp;

    @Lob
    @Column(name="full_data", columnDefinition = "CLOB")
    private String fullData;
}
