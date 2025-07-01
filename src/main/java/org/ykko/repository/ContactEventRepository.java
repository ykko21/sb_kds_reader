package org.ykko.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactEventRepository extends JpaRepository<ContactEvent, String> {

}
