package com.mashreq.conference.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashreq.conference.booking.config.ConferenceRoomProperties;
import com.mashreq.conference.booking.controller.ConferenceRoomBookingController;
import com.mashreq.conference.booking.domain.BookingStatus;
import com.mashreq.conference.booking.domain.ConfRoomBookingRequestDto;
import com.mashreq.conference.booking.domain.ConfRoomBookingResponseDto;
import com.mashreq.conference.booking.entities.BookingDetails;
import com.mashreq.conference.booking.entities.ConferenceRoom;
import com.mashreq.conference.booking.exception.ConfRoomBookingExceptionHandler;
import com.mashreq.conference.booking.repo.BookingDetailsRepository;
import com.mashreq.conference.booking.repo.ConferenceRoomRepository;
import com.mashreq.conference.booking.service.ConferenceBookingService;
import com.mashreq.conference.booking.validator.RequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@WebMvcTest(ConferenceRoomBookingController.class)
@ContextConfiguration(classes = {
        ConferenceRoomBookingController.class,
        ConferenceRoomProperties.class,
        ConfRoomBookingExceptionHandler.class,
        RequestValidator.class,
        ConferenceRoomRepository.class,
        ConferenceBookingService.class})
public class ConferenceRoomBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestValidator requestValidator;

    @MockBean
    ConferenceRoomRepository conferenceRoomRepository;

    @MockBean
    BookingDetailsRepository bookingDetailsRepository;

    @Autowired
    ConferenceBookingService conferenceBookingService;

    @Autowired
    ConferenceRoomProperties conferenceRoomProperties;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRoomBookingInvalidRequest1() throws Exception {

        var request = ConfRoomBookingRequestDto.builder()
                .build();

        String requestBodyJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/conference/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson)
                );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Invalid Request", confRoomBookingRes.getResponse());

        var errorList = confRoomBookingRes.getErrors();

        assertTrue(errorList.contains("user Id is required"));
        assertTrue(errorList.contains("start time is required"));
        assertTrue(errorList.contains("end time is required"));
        assertTrue(errorList.contains("number of people should be minimum 2"));
    }

    @Test
    public void testRoomBookingValidRequestWithInvalidData() throws Exception {

        var request = ConfRoomBookingRequestDto.builder()
                .userId("12345")
                .startTime("01:00")
                .endTime("00:00")
                .numberOfPeople(2)
                .build();

        String requestBodyJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post("/conference/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
        );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Invalid Request", confRoomBookingRes.getResponse());

        var errorList = confRoomBookingRes.getErrors();

        assertTrue(errorList.contains("Start Time should always be lesser than End Time."));
    }

    @Test
    public void testRoomBookingValidRequestWithInvalidTimeInterval() throws Exception {

        var startTime = nextAvailableStartTime();
        String formattedStartTime = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());

        var endTime = startTime.plusMinutes(13L);

        while(verifyBookingTimeWithinMaintenance(endTime))
            endTime = endTime.plusMinutes(13L);

        String formattedEndTime = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());

        var request = ConfRoomBookingRequestDto.builder()
                .userId("12345")
                .startTime(formattedStartTime)
                .endTime(formattedEndTime)
                .numberOfPeople(2)
                .build();

        String requestBodyJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post("/conference/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
        );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Invalid Request", confRoomBookingRes.getResponse());

        var errorList = confRoomBookingRes.getErrors();

        assertTrue(errorList.contains("Invalid Booking time. It should be intervals of 15 mins for example " +
                "2:00 - 2:15 or 2:00 - 2:30 or 2:00 to 3:00"));
    }

    @Test
    public void testRoomBookingValidRequestWithInvalidTimeDuringMaintenance() throws Exception {

        var request = ConfRoomBookingRequestDto.builder()
                .userId("12345")
                .startTime("13:00")
                .endTime("13:15")
                .numberOfPeople(2)
                .build();

        String requestBodyJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post("/conference/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
        );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Invalid Request", confRoomBookingRes.getResponse());

        var errorList = confRoomBookingRes.getErrors();

        assertTrue(errorList.contains("Cannot book room due to maintenance time"));
    }

    @Test
    public void testRoomBookingValidRequestWithMaximumCapcityReached() throws Exception {

        var startTime = nextAvailableStartTime();
        String formattedStartTime = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());

        var endTime = startTime.plusMinutes(15L);
        while(verifyBookingTimeWithinMaintenance(endTime))
            endTime = endTime.plusMinutes(15L);

        String formattedEndTime = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());

        var request = ConfRoomBookingRequestDto.builder()
                .userId("12345")
                .startTime(formattedStartTime)
                .endTime(formattedEndTime)
                .numberOfPeople(21)
                .build();

        var requestBodyJson = objectMapper.writeValueAsString(request);

        var conferenceRoomsList = new ArrayList<ConferenceRoom>();
        conferenceRoomsList.add(new ConferenceRoom(1L, "Amaze", 3, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(2L, "Beauty", 7, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(3L, "Inspire", 12, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(4L, "Strive", 20, BookingStatus.AVAILABLE.name()));

        when(conferenceRoomRepository.findByStatus(BookingStatus.AVAILABLE.name())).thenReturn(conferenceRoomsList);

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post("/conference/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
        );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Invalid Request", confRoomBookingRes.getResponse());

        var errorList = confRoomBookingRes.getErrors();

        assertTrue(errorList.contains("Requested number of people is greater than maximum capacity of the rooms available"));
    }

    @Test
    public void testRoomBookingValidRequest() throws Exception {

        var startTime = nextAvailableStartTime();
        String formattedStartTime = String.format("%02d:%02d", startTime.getHour(), startTime.getMinute());

        var endTime = startTime.plusMinutes(15L);
        while(verifyBookingTimeWithinMaintenance(endTime))
            endTime = endTime.plusMinutes(15L);

        String formattedEndTime = String.format("%02d:%02d", endTime.getHour(), endTime.getMinute());

        var request = ConfRoomBookingRequestDto.builder()
                .userId("12345")
                .startTime(formattedStartTime)
                .endTime(formattedEndTime)
                .numberOfPeople(20)
                .build();

        var requestBodyJson = objectMapper.writeValueAsString(request);

        var conferenceRoomsList = new ArrayList<ConferenceRoom>();
        conferenceRoomsList.add(new ConferenceRoom(1L, "Amaze", 3, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(2L, "Beauty", 7, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(3L, "Inspire", 12, BookingStatus.AVAILABLE.name()));
        conferenceRoomsList.add(new ConferenceRoom(4L, "Strive", 20, BookingStatus.AVAILABLE.name()));

        when(conferenceRoomRepository.findByStatus(BookingStatus.AVAILABLE.name())).thenReturn(conferenceRoomsList);

        var response = mockMvc.perform(
                MockMvcRequestBuilders.post("/conference/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
        );

        var result = response.andReturn();
        var confRoomBookingResStr = result.getResponse().getContentAsString();
        var confRoomBookingRes = objectMapper.readValue(confRoomBookingResStr,ConfRoomBookingResponseDto.class);

        assertEquals("Conference Room Booked Successfully", confRoomBookingRes.getStatus());

        var bookingDetails = objectMapper.convertValue(confRoomBookingRes.getResponse(), BookingDetails.class);

        assertEquals("Strive", bookingDetails.getConferenceRoom().getName());
        assertEquals("BOOKED",bookingDetails.getConferenceRoom().getStatus());

    }


    private LocalTime nextAvailableStartTime(){
        LocalTime currentTime = LocalTime.now();

        // Round up the minutes to the nearest multiple of 15
        int roundedMinutes = (currentTime.getMinute() / 15 + 1) * 15;
        if (roundedMinutes == 60) {
            roundedMinutes = 0;
            currentTime = currentTime.plusHours(1);
        }

        // Adjust the time if the rounded minutes exceed 59
        LocalTime nextTime = currentTime.withMinute(roundedMinutes).withSecond(0).withNano(0);

        while(verifyBookingTimeWithinMaintenance(nextTime))
            nextTime = nextTime.plusMinutes(15);

        return nextTime;
    }

    private boolean verifyBookingTimeWithinMaintenance(LocalTime localTime){

        String formattedEndTime = String.format("%02d:%02d", localTime.getHour(), localTime.getMinute());
        var maintenanceTimings = conferenceRoomProperties.getMaintenanceTimings();

        boolean val = maintenanceTimings.stream()
                .anyMatch(x->x.getStartTime().equals(formattedEndTime));
        return val;
    }
}
