package com.google.codelabs.mdc.java.shrine.entities;

import com.google.codelabs.mdc.java.shrine.R;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    String baseUrl = "http://" + Common.public_ip_or_domain_of_backend + ":" + Common.open_port_of_backend + "/";
    Gson gson = new Gson();

    ApiService apiService = new Retrofit.Builder()

//            .baseUrl("http://hongdatchy.me:8080/")
            .baseUrl(baseUrl)// khong dung duoc localhost vì cái phone nó làm gì biết cái localhost là cái j
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService.class);

    @POST("api/public/common/login")
    Call<MyResponse> login(@Body LoginForm loginForm);

    @POST("api/us/user/book")
    Call<MyResponse> book(@Body BookPayload bookPayload, @Header("token") String token);

    @GET("https://maps.googleapis.com/maps/api/directions/json")
    Call<Object> getDirection(@Query("origin") String origin,
                              @Query("destination") String destination,
                              @Query("mode") String mode,
                              @Query("key") String key);
}
