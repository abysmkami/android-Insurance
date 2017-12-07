package com.abysmkami.android_insurance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.abysmkami.extutil.SmsHelper;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.phone)
    EditText phoneNum;
    @Bind(R.id.sendVerifyCode)
    Button sendVerifyCode;
    @Bind(R.id.verifyCode)
    EditText verifyCode;

    //    @Bind(R.id.verification)
//    Button mVerification;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.sendVerifyCode, R.id.register})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sendVerifyCode:
                String appkey = "22bb824367eb8";
                String appSecret = "2358b54d08d8fc3c76a2f2e118612eeb";
                SmsHelper.initSDK(getApplicationContext(), appkey, appSecret);
                SmsHelper.setCountryCode("86");
                String mPhoneNumber = phoneNum.getText().toString().trim();
                SmsHelper.sendVerifyPhoneNum(mPhoneNumber, new SmsHelper.Send_State() {
                    @Override
                    public void sendPhoneResult(int stateCode, String des) {
                        if (stateCode == SmsHelper.SUCCESS_CODE) {
                            Log.e(stateCode + "", des);
                        }
                    }
                });
                break;
            case R.id.register:
                String verifytext = verifyCode.getText().toString().trim();
                SmsHelper.sendVerificationCode(verifytext, new SmsHelper.Verification_State() {
                    @Override
                    public void sendVerificationCodeResult(int stateCode, String des) {
                        if (stateCode == SmsHelper.SUCCESS_CODE) {
                            Log.e(stateCode + "", des);
                            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
        }
    }
}