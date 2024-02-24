package com.mashreq.conference.booking.controller;

import com.mashreq.conference.booking.config.ConferenceRoomProperties;
import com.mashreq.conference.booking.domain.ConfRoomBookingRequestDto;
import com.mashreq.conference.booking.domain.ConfRoomBookingResponseDto;
import com.mashreq.conference.booking.domain.MaintenanceTiming;
import com.mashreq.conference.booking.entities.ConferenceRoom;
import com.mashreq.conference.booking.service.ConferenceBookingService;
import com.mashreq.conference.booking.validator.RequestValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conference")
@AllArgsConstructor
public class ConferenceRoomBookingController {

    private final RequestValidator requestValidator;
    private final ConferenceRoomProperties conferenceRoomProperties;
    private final ConferenceBookingService conferenceBookingService;

    @GetMapping("/maintenance-timings")
    public List<MaintenanceTiming> getMaintenanceTimings() {
        return conferenceRoomProperties.getMaintenanceTimings();
    }

    @PostMapping("/book")
    public ConfRoomBookingResponseDto book(@Valid @RequestBody ConfRoomBookingRequestDto confRoomBookingRequestDto) {

        var isValid = requestValidator.validateBookingRequest(confRoomBookingRequestDto);

        if(isValid){
           return conferenceBookingService.bookRoom(confRoomBookingRequestDto);
        }
        return ConfRoomBookingResponseDto.builder()
                .status("Invalid Booking Request")
                .build();
    }

    @GetMapping
    public ConfRoomBookingResponseDto getAvailableMeetingRooms(@RequestParam @NotBlank(message = "Startime is required") @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Please use the 24-hour format (HH:mm).")
                                                                   String startTime,
                                                                @RequestParam @NotBlank(message = "EndTime is required") @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Please use the 24-hour format (HH:mm).") String endTime){
        var isValid = requestValidator.validateTimeRange(startTime, endTime);

        if(isValid){
            return conferenceBookingService.fetchListOfAvailableRooms(startTime, endTime);
        }
        return ConfRoomBookingResponseDto.builder()
                .status("Invalid Time Range")
                .build();
    }
}
