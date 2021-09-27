package com.android.carromking.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.carromking.ApiService;
import com.android.carromking.CustomProgressBar;
import com.android.carromking.MyApiEndpointInterface;
import com.android.carromking.R;
import com.android.carromking.models.common.UserDataModel;
import com.android.carromking.models.common.UserWalletDataModel;
import com.android.carromking.models.local.LocalDataModel;
import com.android.carromking.models.otp.VerifyOTPBodyModel;
import com.android.carromking.models.otp.VerifyOTPResponseDataModel;
import com.android.carromking.models.otp.VerifyOTPResponseModel;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnterOTPActivity extends AppCompatActivity {

    TextView tvMobileNumber;
    EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    Button btnVerify;

    ApiService apiService = new ApiService();
    MyApiEndpointInterface apiEndpointInterface = apiService.getApiService();

    CustomProgressBar progressBar;

    String TAG;

    private EditText[] editTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_otpactivity);
        getSupportActionBar().hide();
        progressBar = new CustomProgressBar(this);

        TAG = getString(R.string.TAG);

        tvMobileNumber = findViewById(R.id.tvMobileNumber);
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);
        btnVerify = findViewById(R.id.btnVerify);

        editTexts = new EditText[]{etOtp1, etOtp2, etOtp3, etOtp4, etOtp5 ,etOtp6};

        btnVerify.setClickable(false);

        SharedPreferences sp = getSharedPreferences(TAG, MODE_PRIVATE);
        String numberWithCode = sp.getString("mobileNumber", "+91 9999999999");

        tvMobileNumber.setText(numberWithCode);

        Bundle bundle = getIntent().getExtras();
        String mobileNumber = bundle.getString("mobileNumber");
        String sessionId = bundle.getString("sessionId");

        etOtp1.addTextChangedListener(new PinTextWatcher(0));
        etOtp2.addTextChangedListener(new PinTextWatcher(1));
        etOtp3.addTextChangedListener(new PinTextWatcher(2));
        etOtp4.addTextChangedListener(new PinTextWatcher(3));
        etOtp5.addTextChangedListener(new PinTextWatcher(4));
        etOtp6.addTextChangedListener(new PinTextWatcher(5));

        etOtp1.setOnKeyListener(new PinOnKeyListener(0));
        etOtp2.setOnKeyListener(new PinOnKeyListener(1));
        etOtp3.setOnKeyListener(new PinOnKeyListener(2));
        etOtp4.setOnKeyListener(new PinOnKeyListener(3));
        etOtp5.setOnKeyListener(new PinOnKeyListener(4));
        etOtp6.setOnKeyListener(new PinOnKeyListener(5));


//        etOTP.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (etOTP.getText().toString().trim().length() == 6) {
//                    btnVerify.setClickable(true);
//                    btnVerify.setBackgroundColor(getColor(R.color.blue));
//                } else {
//                    btnVerify.setClickable(false);
//                    btnVerify.setBackgroundColor(getColor(R.color.button_grey));
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });

        btnVerify.setClickable(false);

        btnVerify.setOnClickListener(view -> {
            progressBar.show();
            String otpText =
                    etOtp1.getText().toString()
                            + etOtp2.getText().toString()
                            + etOtp3.getText().toString()
                            + etOtp4.getText().toString()
                            + etOtp5.getText().toString()
                    + etOtp6.getText().toString();

            if (otpText.length() == 6) {
                apiEndpointInterface.verifyOTP(new VerifyOTPBodyModel(mobileNumber, sessionId, otpText))
                        .enqueue(new Callback<VerifyOTPResponseModel>() {
                            @Override
                            public void onResponse(@NonNull Call<VerifyOTPResponseModel> call, @NonNull Response<VerifyOTPResponseModel> response) {
                                if (response.body() != null) {
                                    if (!response.body().isStatus()) {
                                        Toast.makeText(EnterOTPActivity.this, response.body().getError().getMessage(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        sp.edit().putString("token", response.body().getData().getUserData().getToken()).apply();
                                        sp.edit().putString("sessionId", sessionId).apply();
                                        storeDataInLocal(response.body().getData(), sp, numberWithCode);
                                        progressBar.hide();
                                        Intent i = new Intent(EnterOTPActivity.this, MainActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<VerifyOTPResponseModel> call, @NonNull Throwable t) {

                            }
                        });
            }
        });


    }

    void storeDataInLocal(VerifyOTPResponseDataModel dataModel, SharedPreferences sp, String mobileNumber) {
        UserDataModel userData = dataModel.getUserData();
        UserWalletDataModel walletData = dataModel.getUserWalletData();
        LocalDataModel localDataModel = new LocalDataModel(
                String.valueOf(userData.getId()),
                mobileNumber,
                userData.getProfilePic(),
                userData.getLevel(),
                userData.getToken(),
                String.valueOf(walletData.getWinningBalance()),
                String.valueOf(walletData.getDepositBalance()),
                String.valueOf(walletData.getBonusBalance())

        );

        Gson gson = new Gson();
        String json = gson.toJson(localDataModel);
        sp.edit().putString("local", json).apply();
    }


    public class PinTextWatcher implements TextWatcher {

        private int currentIndex;
        private boolean isFirst = false, isLast = false;
        private String newTypedString = "";

        PinTextWatcher(int currentIndex) {
            this.currentIndex = currentIndex;

            if (currentIndex == 0)
                this.isFirst = true;
            else if (currentIndex == editTexts.length - 1)
                this.isLast = true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            newTypedString = s.subSequence(start, start + count).toString().trim();

            if(isLast) {
                btnVerify.setClickable(true);
                btnVerify.setBackgroundColor(getColor(R.color.blue));
            } else {
                btnVerify.setClickable(false);
                btnVerify.setBackgroundColor(getColor(R.color.button_grey));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {


            String text = newTypedString;

            /* Detect paste event and set first char */
            if (text.length() > 1)
                text = String.valueOf(text.charAt(0)); // TODO: We can fill out other EditTexts

            editTexts[currentIndex].removeTextChangedListener(this);
            editTexts[currentIndex].setText(text);
            editTexts[currentIndex].setSelection(text.length());
            editTexts[currentIndex].addTextChangedListener(this);

            if (text.length() == 1)
                moveToNext();
            else if (text.length() == 0)
                moveToPrevious();
        }

        private void moveToNext() {
            if (!isLast)
                editTexts[currentIndex + 1].requestFocus();

            if (isAllEditTextsFilled() && isLast) { // isLast is optional
//                editTexts[currentIndex].clearFocus();
                hideKeyboard();
            }
        }

        private void moveToPrevious() {
            if (!isFirst)
                editTexts[currentIndex - 1].requestFocus();
        }

        private boolean isAllEditTextsFilled() {
            for (EditText editText : editTexts)
                if (editText.getText().toString().trim().length() == 0)
                    return false;
            return true;
        }

        private void hideKeyboard() {
            if (getCurrentFocus() != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }

    }

    public class PinOnKeyListener implements View.OnKeyListener {

        private int currentIndex;

        PinOnKeyListener(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (editTexts[currentIndex].getText().toString().isEmpty() && currentIndex != 0)
                    editTexts[currentIndex - 1].requestFocus();
            }
            return false;
        }

    }
}