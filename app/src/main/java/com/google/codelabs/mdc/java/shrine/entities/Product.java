package com.google.codelabs.mdc.java.shrine.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {

    private String route;
    private String phone;
    private int price;


}
