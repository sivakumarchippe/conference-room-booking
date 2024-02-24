package com.mashreq.conference.booking.exception;

import com.mashreq.conference.booking.domain.ConfRoomBookingResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class ConfRoomBookingExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> invalidRequestHandler(MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> errors = new ArrayList<>();

    ex.getAllErrors().forEach(err -> errors.add(err.getDefaultMessage()));

    var response = ConfRoomBookingResponseDto.builder()
            .response("Invalid Request")
            .errors(errors)
            .build();

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConferenceRoomBookingException.class)
  public ResponseEntity<?> invalidRequestHandler(ConferenceRoomBookingException ex, HttpServletRequest request) {
    List<String> errors = new ArrayList<>();
    errors.add(ex.getMessage());
    var response = ConfRoomBookingResponseDto.builder()
            .response("Invalid Request")
            .errors(errors)
            .build();

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }


}