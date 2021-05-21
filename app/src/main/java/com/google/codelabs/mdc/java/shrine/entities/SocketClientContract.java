package com.google.codelabs.mdc.java.shrine.entities;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.Date;
import java.util.List;

import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class SocketClientContract {

    private CompositeDisposable compositeDisposable;
    private StompClient mStompClient;
    private static final String TAG = "SocketClientContract";
    private static final String DOMAIN = "35.193.139.81";
    private static final String SERVER_PORT = "8080";

    public SocketClientContract(){
        this.mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://" + DOMAIN
                + ":" + SERVER_PORT + "/example-endpoint/websocket");
        mStompClient.connect();
    }

    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    public void subscriberStomp(MediaPlayer mediaPlayer, List<Product> listProduct, ProductListViewAdapter productListViewAdapter, List<Message> messages) {
        resetSubscriptions();
        Disposable disposable = mStompClient.topic("/topic/greetings")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    mediaPlayer.start();
                    Gson gson = new Gson();
                    Message message = gson.fromJson(topicMessage.getPayload(), Message.class);
                    String route = "From "+message.getOrigin() + " to " + message.getDestination()
                            + " " + message.getDistance() + "km";
                    Log.d(TAG, "Received " + topicMessage.getPayload());
                    listProduct.add(new Product(route, message.getPhone(), +message.getCost()));
                    productListViewAdapter.notifyDataSetChanged();
                    messages.add(message);
                }, throwable -> {
                    Log.e(TAG, "Error on subscribe topic", throwable);
                });
        compositeDisposable.add(disposable);
    }

    public void sendEchoViaStomp(String message) {
//        if (!mStompClient.isConnected()) return;
        compositeDisposable.add(mStompClient.send("/topic/hello-msg-mapping", message)
                .compose(applySchedulers())
                .subscribe(() -> {
                    Log.d(TAG, "STOMP echo send successfully");
                }, throwable -> {
                    Log.e(TAG, "Error send STOMP echo", throwable);
                }));

    }

    protected CompletableTransformer applySchedulers() {
        return upstream -> upstream
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
