package com.mashreq.conference.booking.service;

import com.mashreq.conference.booking.domain.BookingStatus;
import com.mashreq.conference.booking.domain.ConfRoomBookingRequestDto;
import com.mashreq.conference.booking.domain.ConfRoomBookingResponseDto;
import com.mashreq.conference.booking.entities.BookingDetails;
import com.mashreq.conference.booking.entities.ConferenceRoom;
import com.mashreq.conference.booking.exception.ConferenceRoomBookingException;
import com.mashreq.conference.booking.repo.BookingDetailsRepository;
import com.mashreq.conference.booking.repo.ConferenceRoomRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ConferenceBookingService {

    private final ConferenceRoomRepository conferenceRoomRepository;
    private final BookingDetailsRepository bookingDetailsRepository;

    public ConfRoomBookingResponseDto bookRoom(ConfRoomBookingRequestDto confRoomBookingRequestDto){

        List<ConferenceRoom> availableConferenceRoomList = conferenceRoomRepository.findByStatus(BookingStatus.AVAILABLE.name());

        Map<Integer, List<ConferenceRoom>> capacityDifference= availableConferenceRoomList.stream()
                .filter(x->(confRoomBookingRequestDto.getNumberOfPeople()<=x.getCapacity()))
                .collect(Collectors.groupingBy(x->x.getCapacity()));
        Optional<List<ConferenceRoom>> conferenceRoomOptionalList = capacityDifference.entrySet().stream().min(Map.Entry.comparingByKey()).map(x->x.getValue());
        var  conferenceRoom = conferenceRoomOptionalList.get()!=null ? conferenceRoomOptionalList.get().get(0): null;

        if(Objects.isNull(conferenceRoom))
            throw new ConferenceRoomBookingException("There are no avaialable rooms at the moment");


        conferenceRoom.setStatus(BookingStatus.BOOKED.name());

        var bookingDetails = BookingDetails.builder()
                            .fromTime(confRoomBookingRequestDto.getStartTime())
                            .toTime(confRoomBookingRequestDto.getEndTime())
                            .bookedBy(confRoomBookingRequestDto.getUserId())
                            .numberOfPeople(confRoomBookingRequestDto.getNumberOfPeople())
                            .conferenceRoom(conferenceRoom)
                            .build();
        bookingDetailsRepository.save(bookingDetails);

        return ConfRoomBookingResponseDto.builder()
                .response(bookingDetails)
                .status("Conference Room Booked Successfully")
                .build();
    }

    public ConfRoomBookingResponseDto fetchListOfAvailableRooms(String startTime, String endTime){

        var bookingDetailsList = bookingDetailsRepository.getBookingBetweenStartTimeEndTime(startTime,endTime);

        var conferenceRoomList = conferenceRoomRepository.findAll();

        var finalConferenceList = new ArrayList<ConferenceRoom>();



        if(Objects.nonNull(bookingDetailsList) && !bookingDetailsList.isEmpty()){
            var bookingDetailsConfList =  bookingDetailsList.stream().map(x->x.getConferenceRoom().getName()).collect(Collectors.toList());
            conferenceRoomList.stream().filter(x->Objects.nonNull(bookingDetailsConfList)
                            && !bookingDetailsConfList.isEmpty()
                            && !bookingDetailsConfList.contains(x.getStatus()))
                    .forEach(x->{
                        x.setStatus(BookingStatus.AVAILABLE.name());
                        finalConferenceList.add(x);
                    });
        }else {
            conferenceRoomList.stream().forEach(x->{
                        x.setStatus(BookingStatus.AVAILABLE.name());
                        finalConferenceList.add(x);
                    });
        }





        return ConfRoomBookingResponseDto.builder()
                .response(finalConferenceList)
                .build();
    }

    private boolean isConferenceRoomBooked(List<BookingDetails> bookingDetailsList){


        return true;

    }



}
