package org.ykko.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CtrEventRepository extends JpaRepository<CtrEvent, String> {
    Optional<CtrEvent> findByAgentUsernameAndContactIdAndInitialContactIdAndPreviousContactIdAndLastUpdateTimestamp(String agentUsername, String contactId, String initialContactId, String previousContactId, String lastUpdateTimestamp);
}
