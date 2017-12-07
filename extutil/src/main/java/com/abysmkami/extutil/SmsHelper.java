package com.abysmkami.extutil;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.utils.SMSLog;

import static android.content.ContentValues.TAG;

/**
 * Created by "小灰灰"
 * on 3/3/2017 17:07
 * 邮箱：www.adonis_lsh.com
 */

public class SmsHelper {

    public static final int SUCCESS_CODE = 1;
    private static String mPhoneNum;
    private static Send_State mSend_state;
    private static String Tag = "SmsHelper";
    private static Context mContext;
    private static String mCountryCode;
    private static String mVerificationCode;
    private static Verification_State mVerification_state;
    // 默认使用中国区号
    private static final String DEFAULT_COUNTRY_ID = "86";


    /**
     * 初始化SDK,ShareSDK是可以多次初始化的,如何在应用中多次调用
     *
     * @param context 这里面最好传入app的Context,这样不容易造成内存泄漏
     * @param appKey 传入ShareSDK申请的appKey
     * @param appSecret 传入ShareSDK申请的appSecret
     * @return SmsAPI
     */
    public static void initSDK(Context context, String appKey, String appSecret) {
        mContext = context;
        SMSSDK.initSDK(context, appKey, appSecret);
        SMSSDK.registerEventHandler(eh);
        mCountryCode = getCurrentCountryCode(mContext);
    }

    /**
     * 如果不调用这个方法,将使用默认的国家码
     * @param countryCode 设置国家码,不设置将使用当前网路所在的国际码
     * @return SmsAPI
     */
    public static void setCountryCode(String countryCode) {
        mCountryCode = countryCode;
    }

    /**
     * @param phoneNum   要发送验证码的手机号
     * @param send_state 发送验的状态
     */
    public static void sendVerifyPhoneNum(String phoneNum, Send_State send_state) {
        mPhoneNum = phoneNum;
        mSend_state = send_state;
        Log.e(Tag, mCountryCode);
        SMSSDK.getVerificationCode(mCountryCode, mPhoneNum);
//        return this;
    }

    /**
     *
     * @param verificationCode 验证码
     * @param verification_state 发送验证码的状态(结果)
     * @return SmsAPI
     */
    public static void sendVerificationCode(String verificationCode, Verification_State verification_state) {
        mVerificationCode = verificationCode;
        mVerification_state = verification_state;
        if (TextUtils.isEmpty(mCountryCode)) {
            Log.e(Tag, "请调用initSDK()初始化手机号");
        } else if (TextUtils.isEmpty(mPhoneNum)) {
            Log.e(Tag, "请先调用sendVerifyPhoneNum(String phoneNum,Send_State send_state)方法,初始化手机号");
        } else {
            SMSSDK.submitVerificationCode(mCountryCode, mPhoneNum, verificationCode);
        }
//        return this;
    }

    static EventHandler eh = new EventHandler() {
        @Override
        public void afterEvent(int event, int result, Object data) {
            Log.e(TAG,"event = "+event+" result = "+result+" data = " +data.toString());
            Message msg = new Message();
            msg.arg1 = event;
            msg.arg2 = result;
            msg.obj = data;
            SMSHandler.sendMessage(msg);
        }

    };

     static Handler SMSHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int event = msg.arg1;
            int result = msg.arg2;
            Object data = msg.obj;
            if (result == SMSSDK.RESULT_COMPLETE) {
                //回调完成
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    //提交验证码成功
                    Log.e(TAG, "提交验证码成功" + data.toString());
                    if (mVerification_state != null) {
                        mVerification_state.sendVerificationCodeResult(SUCCESS_CODE,"成功");
                    }
                } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    //获取验证码成功
                    Log.e(TAG, "获取验证码成功" + data.toString());
                    if (mSend_state != null) {
                        mSend_state.sendPhoneResult(SUCCESS_CODE,"成功");
                    }
                } else if (event == SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES) {
                    //返回支持发送验证码的国家列表
                    Log.e(TAG, "返回支持发送验证码的国家列表" + data.toString());
                }
            } else {
                    int status;
                    try {
                        ((Throwable) data).printStackTrace();
                        Throwable throwable = (Throwable) data;
                        JSONObject object = new JSONObject(throwable.getMessage());
                        String des = object.optString("detail");
                        status = object.optInt("status");
                        Log.e(TAG, status + "");
                        if (!TextUtils.isEmpty(des)) {
                            if (mSend_state != null) {
                                mSend_state.sendPhoneResult(status,des);
                            }
                            if (mVerification_state != null) {
                                mVerification_state.sendVerificationCodeResult(status,des);
                            }
                            return;
                        }
                    } catch (Exception e) {
                        SMSLog.getInstance().w(e);
                    }
            }
            super.handleMessage(msg);
        }
    };

    public static void cancelCall() {
        SMSSDK.unregisterAllEventHandler();
//        return this;
    }

    /**
     * 服务器发送验证码的状态
     */
    public interface Send_State {
        void sendPhoneResult(int stateCode, String des);
    }

    public interface Verification_State {
        void sendVerificationCodeResult(int stateCode, String des);
    }
    public static String getCurrentCountryCode(Context context) {
        String mcc = getMCC(context);
        String[] countryArr = null;
        if (!TextUtils.isEmpty(mcc)) {
            countryArr = SMSSDK.getCountryByMCC(mcc);
        }
        if (countryArr == null) {
            Log.w("SMSSDK", "no country found by MCC: " + mcc);
            countryArr = SMSSDK.getCountry(DEFAULT_COUNTRY_ID);
        }
        return countryArr[1];
    }

    private static String getMCC(Context context) {
        TelephonyManager tm = (TelephonyManager)context
                .getSystemService(Context.TELEPHONY_SERVICE);
        // 返回当前手机注册的网络运营商所在国家的MCC+MNC. 如果没注册到网络就为空.
        String networkOperator = tm.getNetworkOperator();
        if (!TextUtils.isEmpty(networkOperator)) {
            return networkOperator;
        }
        // 返回SIM卡运营商所在国家的MCC+MNC. 5位或6位. 如果没有SIM卡返回空
        return tm.getSimOperator();
    }

}
