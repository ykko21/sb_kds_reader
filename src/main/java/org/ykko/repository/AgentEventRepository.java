package org.ykko.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentEventRepository extends JpaRepository<AgentEvent, String> {

}
