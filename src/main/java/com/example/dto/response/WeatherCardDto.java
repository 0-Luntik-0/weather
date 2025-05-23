package com.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCardDto {
    private Integer locationId;
    private String locationName;
    private LocationResponseDto weather;


}