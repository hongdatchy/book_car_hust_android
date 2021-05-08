package com.google.codelabs.mdc.java.shrine.entities;

import com.google.gson.annotations.SerializedName;

import lombok.Data;


@Data
public class MyResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Object data;

}
