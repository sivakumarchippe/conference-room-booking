package com.mashreq.conference.booking.repo;

import com.mashreq.conference.booking.entities.ConferenceRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConferenceRoomRepository extends JpaRepository<ConferenceRoom, Long> {

    List<ConferenceRoom> findByStatus(String status);
}
