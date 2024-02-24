package com.mashreq.conference.booking.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfRoomBookingRequestDto {

    @NotBlank(message = "user Id is required")
    private String userId;

    @NotBlank(message = "start time is required")
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Please use the 24-hour format (HH:mm).")
    private String startTime;

    @NotBlank(message = "end time is required")
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]$", message = "Invalid time format. Please use the 24-hour format (HH:mm).")
    private String endTime;

    @NotNull(message = "number of people should not be zero or empty or null")
    @Min(value = 2, message = "number of people should be minimum 2")
    private int numberOfPeople;
}
