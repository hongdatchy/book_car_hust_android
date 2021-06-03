package com.google.codelabs.mdc.java.shrine.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.codelabs.mdc.java.shrine.entities.ApiService;
import com.google.codelabs.mdc.java.shrine.entities.BookPayload;
import com.google.codelabs.mdc.java.shrine.entities.Message;
import com.google.codelabs.mdc.java.shrine.entities.MyResponse;
import com.google.codelabs.mdc.java.shrine.R;
import com.google.codelabs.mdc.java.shrine.entities.Product;
import com.google.codelabs.mdc.java.shrine.entities.ProductListViewAdapter;
import com.google.codelabs.mdc.java.shrine.entities.SocketClientContract;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DistanceFragment extends Fragment {

    private SocketClientContract socketClientContract;
    private double distance, lat1, lat2, long1, long2;
    private int cost;
    String TAG ="DistanceFragment";
    String origin, destination, phone;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        socketClientContract = new SocketClientContract();
        ProductListViewAdapter productListViewAdapter = new ProductListViewAdapter(new ArrayList<>());
        socketClientContract.subscriberStomp(new MediaPlayer(), new ArrayList<>(), productListViewAdapter, new ArrayList<>());
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_distance, container, false);
        TextView textView = view.findViewById(R.id.text_view);
        Button button = view.findViewById(R.id.book_btn);

        distance = Double.parseDouble(getArguments().getString("distance"));
        lat1 = Double.parseDouble(getArguments().getString("lat1"));
        lat2 = Double.parseDouble(getArguments().getString("lat2"));
        long1 = Double.parseDouble(getArguments().getString("long1"));
        long2 = Double.parseDouble(getArguments().getString("long2"));
        origin = getArguments().getString("origin");
        destination = getArguments().getString("destination");
        phone = getArguments().getString("phone");
        if(phone != null){
            button.setText("Phone " + phone);
            button.setTextSize(10);
            button.setOnClickListener(v -> {
                call(phone);
            });
        }else{
            button.setOnClickListener(v -> {
                book();
            });
        }
        cost = getCost(distance);
        String costStr = String.valueOf(cost).replaceAll("(\\d)(?=(\\d{3})+$)", "$1,");
        textView.setText("Distance: " + Math.round(distance*100.0)/100.0 + " km" + '\n' + "Cost: " + costStr + " Vn Dong");
        return view;
    }

    private void call(String phone) {
        String phoneNumber = String.format("tel: %s", phone);
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        // Set the data for the intent as the phone number.
        callIntent.setData(Uri.parse(phoneNumber));
        // If package resolves to an app, send intent.
        if (callIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(callIntent);
        } else {
            Log.e(TAG, "Can't resolve app for ACTION_DIAL Intent.");
        }
    }

    private int getCost(double distance) {
        return (int)distance*5000;
    }

    private void book(){

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        SharedPreferences settings = getActivity().getSharedPreferences("token", Context.MODE_PRIVATE);
        String token = settings.getString("user", "user token null");
        settings = getActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        String phone = settings.getString("phone", "user phone null");
        socketClientContract.sendEchoViaStomp(
                new Gson().toJson(Message.builder()
                        .cost(cost)
                        .distance(distance)
                        .lat1(lat1)
                        .lat2(lat2)
                        .long1(long1)
                        .long2(long2)
                        .origin(origin)
                        .destination(destination)
                        .phone(phone)
                        .build()
                )
        );

        ApiService.apiService.book(BookPayload.builder()
                .cost(cost)
                .distance(String.valueOf(distance))
                .build(), token).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                Log.e(TAG, String.valueOf(response));
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {
                Log.e(TAG, String.valueOf(t));
                progressDialog.dismiss();
            }
        });
    }
}
