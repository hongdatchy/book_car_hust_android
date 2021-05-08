package com.google.codelabs.mdc.java.shrine.entities;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    Gson gson = new Gson();

    ApiService apiService = new Retrofit.Builder()
            .baseUrl("http://hongdatchy.me:8080/")
//            .baseUrl("http://192.168.1.77:8080/")// khong dung duoc localhost vì cái phone nó làm gì biết cái localhost là cái j
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService.class);

    @POST("api/public/common/login")
    Call<MyResponse> login(@Body LoginForm loginForm);

    @POST("api/us/user/book")
    Call<MyResponse> book(@Body BookPayload bookPayload, @Header("token") String token);
}
