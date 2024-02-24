package com.mashreq.conference.booking.validator;

import com.mashreq.conference.booking.config.ConferenceRoomProperties;
import com.mashreq.conference.booking.domain.BookingStatus;
import com.mashreq.conference.booking.domain.ConfRoomBookingRequestDto;
import com.mashreq.conference.booking.entities.BookingDetails;
import com.mashreq.conference.booking.entities.ConferenceRoom;
import com.mashreq.conference.booking.exception.ConferenceRoomBookingException;
import com.mashreq.conference.booking.repo.BookingDetailsRepository;
import com.mashreq.conference.booking.repo.ConferenceRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final ConferenceRoomProperties conferenceRoomProperties;
    private final ConferenceRoomRepository conferenceRoomRepository;
    private final BookingDetailsRepository bookingDetailsRepository;


    public boolean validateBookingRequest(ConfRoomBookingRequestDto confRoomBookingRequestDto){

        if(Objects.isNull(confRoomBookingRequestDto)
            ||  Objects.isNull(confRoomBookingRequestDto.getStartTime())
            || Objects.isNull(confRoomBookingRequestDto.getEndTime()))
            throw new ConferenceRoomBookingException("Invalid Request");

        LocalTime startTime = LocalTime.parse(confRoomBookingRequestDto.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(confRoomBookingRequestDto.getEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

        if(endTime.isBefore(startTime))
            throw new ConferenceRoomBookingException("Start Time should always be lesser than End Time.");

        if(isPastTime(startTime,endTime))
            throw new ConferenceRoomBookingException("Start Time or End Time should be greater than current time");

        if(isValidBookingInterval(startTime , endTime)!=0)
            throw new ConferenceRoomBookingException("Invalid Booking time. It should be intervals of 15 mins for example " +
                    "2:00 - 2:15 or 2:00 - 2:30 or 2:00 to 3:00");
        if(verifyBookingTimeWithinMaintenance(confRoomBookingRequestDto.getStartTime() , confRoomBookingRequestDto.getEndTime()))
            throw new ConferenceRoomBookingException("Cannot book room due to maintenance time");

        updateStatusOfAvaialableRooms();
        var conferenceRoomList = conferenceRoomRepository.findByStatus(BookingStatus.AVAILABLE.name());

        if(conferenceRoomList.isEmpty())
            throw new ConferenceRoomBookingException("There are no conference rooms available at the moment");

        if(verifyCapacityRequested(confRoomBookingRequestDto.getNumberOfPeople(), conferenceRoomList))
            throw new ConferenceRoomBookingException("Requested number of people is greater than maximum capacity " +
                    "of the rooms available");

        return true;

    }

    private long isValidBookingInterval(LocalTime startTime, LocalTime endTime){
        // Calculate the difference
        long differenceInMinutes = startTime.until(endTime, ChronoUnit.MINUTES);

        if(Objects.isNull(conferenceRoomProperties.getBooking()))
            throw new ConferenceRoomBookingException("Booking Interval cannot be null");

        int interval = conferenceRoomProperties.getBooking().getInterval();
        // Calculate the difference in 15-minute intervals
        long differenceIn15MinIntervals = differenceInMinutes % interval;


        return differenceIn15MinIntervals;
    }

    private boolean verifyBookingTimeWithinMaintenance(String startTime, String endTime){
        var maintenanceTimings = conferenceRoomProperties.getMaintenanceTimings();

        boolean val = maintenanceTimings.stream()
                .anyMatch(x->x.getStartTime().equals(startTime) || x.getEndTime().equals(endTime));
        return val;
    }

    private boolean verifyCapacityRequested(int capacity, List<ConferenceRoom> conferenceRoomList){

        Optional<ConferenceRoom> maxCapacityConference =  conferenceRoomList.stream()
                .max((p1,p2)->p1.getCapacity() - p2.getCapacity());

        if(maxCapacityConference.isEmpty())
            return true;

        ConferenceRoom conferenceRoom = maxCapacityConference.get();
        return conferenceRoom.getCapacity() < capacity;
    }

    private boolean isPastTime(LocalTime startTime, LocalTime endTime){
        // Get the current time
        LocalTime presentTime = LocalTime.now();

        // Define a DateTimeFormatter for the desired format "HH:mm"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Format the LocalTime object using the formatter
        String formattedTime = presentTime.format(formatter);

        LocalTime currentTime = LocalTime.parse(formattedTime, DateTimeFormatter.ofPattern("HH:mm"));

        if(startTime.isBefore(currentTime) || endTime.isBefore(currentTime))
            return true;

        return false;
    }

    private void updateStatusOfAvaialableRooms() {

        var conferenceRoomList = bookingDetailsRepository.findAll();
        var expiryList = conferenceRoomList.stream()
                .filter(x->isBookingExpired(x.getToTime())
                        && x.getConferenceRoom().getStatus().equals(BookingStatus.BOOKED))
                .collect(Collectors.toList());

        expiryList.stream().forEach(x->updateExpiryBooking(x));

    }

    private boolean isBookingExpired(String endTime) {

        LocalTime toTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"));

        // Get the current time
        LocalTime presentTime = LocalTime.now();

        // Define a DateTimeFormatter for the desired format "HH:mm"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Format the LocalTime object using the formatter
        String formattedTime = presentTime.format(formatter);

        LocalTime currentTime = LocalTime.parse(formattedTime, DateTimeFormatter.ofPattern("HH:mm"));

        return toTime.isBefore(currentTime);
    }

    private void updateExpiryBooking(BookingDetails bookingDetails) {
        ConferenceRoom conferenceRoom = bookingDetails.getConferenceRoom();
        conferenceRoom.setStatus(BookingStatus.AVAILABLE.name());
        conferenceRoomRepository.save(conferenceRoom);

    }

    public boolean validateTimeRange(String startTimeStr, String endTimeStr){

        if(Objects.isNull(startTimeStr) ||  Objects.isNull(endTimeStr))
            throw new ConferenceRoomBookingException("Invalid Time Range Given");

        LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

        if(endTime.isBefore(startTime))
            throw new ConferenceRoomBookingException("Start Time should always be lesser than End Time.");

        if(isPastTime(startTime,endTime))
            throw new ConferenceRoomBookingException("Start Time or End Time should be greater than current time");

        updateStatusOfAvaialableRooms();
        return true;
    }

}
