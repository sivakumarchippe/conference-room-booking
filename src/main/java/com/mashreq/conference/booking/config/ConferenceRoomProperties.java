package com.mashreq.conference.booking.config;

import com.mashreq.conference.booking.domain.Booking;
import com.mashreq.conference.booking.domain.MaintenanceTiming;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "conference-room")
public class ConferenceRoomProperties {
    private List<MaintenanceTiming> maintenanceTimings;
    private Booking booking;
}
