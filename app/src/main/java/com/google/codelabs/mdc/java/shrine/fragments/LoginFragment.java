package com.google.codelabs.mdc.java.shrine.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.codelabs.mdc.java.shrine.entities.ApiService;
import com.google.codelabs.mdc.java.shrine.entities.DataLogin;
import com.google.codelabs.mdc.java.shrine.entities.LoginForm;
import com.google.codelabs.mdc.java.shrine.entities.MyResponse;
import com.google.codelabs.mdc.java.shrine.R;
import com.google.codelabs.mdc.java.shrine.activities.MainActivity;
import com.google.gson.Gson;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Fragment representing the login screen for Shrine.
 */
public class LoginFragment extends Fragment {

    private TextInputEditText passwordEditText;
    private TextInputLayout passwordTextInput;
    private TextInputEditText usernameEditText;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.login_fragment, container, false);
        passwordTextInput = view.findViewById(R.id.password_text_input);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        usernameEditText = view.findViewById(R.id.username_edit_text);
        MaterialButton nextButton = view.findViewById(R.id.next_button);

        nextButton.setOnClickListener(view1 -> {
            String username = String.valueOf(usernameEditText.getText());
            String pass = String.valueOf(passwordEditText.getText());
//            set username from local to say hi : ...
            SharedPreferences settings = getActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("username", username).apply();

//            call api login
            callApi(new LoginForm(username, pass));
        });

        return view;
    }

    private void callApi(LoginForm loginForm) {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ApiService.apiService.login(loginForm).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                MyResponse loginResponse = response.body();
                passwordTextInput.setError(null);
                if(loginResponse.getMessage().equals("user")){
//                    set token from local
                    SharedPreferences settings = getActivity().getSharedPreferences("token", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(loginResponse.getData());
                    DataLogin dataLogin = gson.fromJson(json, DataLogin.class);
                    editor.putString("user", dataLogin.getToken()).apply();
                    settings = getActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
                    editor = settings.edit();
                    editor.putString("phone", dataLogin.getPhone()).apply();
//                    start map activity
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.gotoGoogleMap();
                }else if(loginResponse.getMessage().equals("driver")){
//                    set token from local
                    SharedPreferences settings = getActivity().getSharedPreferences("token", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("driver", (String) loginResponse.getData()).apply();
//                    start driver activity
                    MainActivity  mainActivity = (MainActivity) getActivity();
                    mainActivity.gotoDriverActivity();
                }else{
                    passwordTextInput.setError((CharSequence) loginResponse.getData());
                }

                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {
                Toast.makeText(getActivity(),"Error internet or server is not running",Toast.LENGTH_SHORT).show();
                System.out.println("ssssssscc" );
                progressDialog.dismiss();
            }

        });
    }

}
