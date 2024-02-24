package com.mashreq.conference.booking.exception;

public class ConferenceRoomBookingException extends RuntimeException{

    public ConferenceRoomBookingException(){
        super();
    }

    public ConferenceRoomBookingException(String message){
        super(message);
    }

    public ConferenceRoomBookingException(String message, Throwable cause){
        super(message, cause);
    }
}
