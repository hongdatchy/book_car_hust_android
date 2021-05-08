package com.google.codelabs.mdc.java.shrine.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private double distance;
    private double lat1;
    private double lat2;
    private double long1 ;
    private double long2 ;
    private int cost;
    private String origin;
    private String destination;
    private String phone;
}
