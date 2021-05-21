package com.google.codelabs.mdc.java.shrine.activities;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.codelabs.mdc.java.shrine.entities.Message;
import com.google.codelabs.mdc.java.shrine.entities.Product;
import com.google.codelabs.mdc.java.shrine.entities.ProductListViewAdapter;
import com.google.codelabs.mdc.java.shrine.R;
import com.google.codelabs.mdc.java.shrine.entities.SocketClientContract;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class DriverActivity extends AppCompatActivity {

    ArrayList<Product> listProduct;
    ProductListViewAdapter productListViewAdapter;
    ListView listViewProduct;
    TextView textViewHiDriver;
    List<Message> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);
        listProduct = new ArrayList<>();
        productListViewAdapter = new ProductListViewAdapter(listProduct);
        messages = new ArrayList<>();
        listViewProduct = findViewById(R.id.list_product);
        listViewProduct.setAdapter(productListViewAdapter);
        textViewHiDriver = findViewById(R.id.hi_driver);

        String username = getSharedPreferences("user_info", Context.MODE_PRIVATE).getString("username", "Driver");
        textViewHiDriver.setText("Hi " + username);

        listViewProduct.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Gson gson = new Gson();
                Intent myIntent = new Intent(DriverActivity.this, MapsActivity.class);
                myIntent.putExtra("driver apply contract", gson.toJson(messages.get(messages.size()-1)));
                DriverActivity.this.startActivity(myIntent);
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listProduct.clear();
                productListViewAdapter.notifyDataSetChanged();
            }
        });

        SocketClientContract socketClientContract = new SocketClientContract();
        socketClientContract.subscriberStomp(MediaPlayer.create(this, R.raw.nofication), listProduct, productListViewAdapter, messages);

    }

}
