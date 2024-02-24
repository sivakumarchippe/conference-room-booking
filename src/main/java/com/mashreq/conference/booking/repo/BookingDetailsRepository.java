package com.mashreq.conference.booking.repo;

import com.mashreq.conference.booking.entities.BookingDetails;
import com.mashreq.conference.booking.entities.ConferenceRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailsRepository extends JpaRepository<BookingDetails, Long> {

    @Query("SELECT e FROM BookingDetails e WHERE e.fromTime >= :startTime AND e.toTime <= :endTime")
    List<BookingDetails> getBookingBetweenStartTimeEndTime(String startTime,String endTime);
}
