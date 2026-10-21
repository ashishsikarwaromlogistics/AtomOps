package com.example.atomops.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.example.atomops.R;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.CancellationException;

import javax.net.ssl.SSLException;

import retrofit2.HttpException;

public class Utils {
    public static String getDeviceIMEI(Context context) {
        String deviceUniqueIdentifier=  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        try{
            deviceUniqueIdentifier=  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        }
        catch (Exception e){
            deviceUniqueIdentifier="errorfind";
        }
        return deviceUniqueIdentifier;
    }
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean haveInternet(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            return true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            return true;
        }

         Toast.makeText(ctx,"No,Internet Connection",0).show();
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected() && cm.isActiveNetworkMetered();

    }
    public static void showDialog(Context con,String title, String msg, int icon) {
        try {
            new android.app.AlertDialog.Builder(con)
                    .setTitle(title)
                    .setIcon(icon)
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                    .show();
            Log.d("ashishsikarwar",msg);
        }catch (Exception e){}

    }
    public static void showDialog2(Context con, String title, String msg, int icon, Runnable onOkClick,
                                   Runnable onOkClick1) {
        try {
            new android.app.AlertDialog.Builder(con)
                    .setTitle(title)
                    .setIcon(icon)
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, id) -> {
                        dialog.dismiss();
                        if (onOkClick != null) onOkClick.run(); // Call the callback
                    })

                    .setNegativeButton("Cancel", (dialog, id) -> {
                        dialog.dismiss();
                        if (onOkClick1 != null) onOkClick1.run(); // Call the callback
                    })

                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDialog(Context con, String title, String msg,String positivebtn,String negativebutton, int icon, Runnable onOkClick,
                                   Runnable onOkClick1) {
        try {
            new android.app.AlertDialog.Builder(con)
                    .setTitle(title)
                    .setIcon(icon)
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton(positivebtn, (dialog, id) -> {
                        dialog.dismiss();
                        if (onOkClick != null) onOkClick.run(); // Call the callback
                    })

                    .setNegativeButton(negativebutton, (dialog, id) -> {
                        dialog.dismiss();
                        if (onOkClick1 != null) onOkClick1.run(); // Call the callback
                    })

                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String getCurrentTimestamp()  {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public static String getCurrentTimestampunique() {
        return new SimpleDateFormat(
                "yyyyMMddHHmmssSSS",
                Locale.getDefault()
        ).format(new Date());
    }

    public static String formatNumber(int number) {
        return String.format("%04d", number);
    }


    public static void geterrormessagewithcode(Context con,int code,String msg){
        showDialog(con,"error",getHttpErrorMessage(code,msg), R.drawable.ic_baseline_error_24);
    }

    /** Converts an HTTP status code into a human-readable message. */
    public static String getHttpErrorMessage(int code,String msg) {
        switch (code) {
            case 400:
                return "The request was invalid. Please try again.\n" +
                        "";
            case 401:
                return "Your session has expired. Please log in again.";
            case 403:
                return "You don't have permission to access this.";
            case 404:
                return "The service could not be found. Please try again later.";
            case 408:
                return "The request timed out. Please try again.";
            case 429:
                return "Too many attempts. Please wait a moment and try again.";
            default:
                if (code >= 500 && code <= 599) {
                    return "Our server is having trouble right now. Please try again later.";
                }
                return "Something went wrong (code " + code + "). Please try again.";
        }
    }


    public static void gettrycatcherror(Context con,Throwable e){
        showDialog(con,"error",getErrorMessage(e), R.drawable.ic_baseline_error_24);
    }


    /** Converts any exception into a human-readable message. */
    public static String getErrorMessage(Throwable e) {
        // Never swallow coroutine cancellation (kotlinx CancellationException
        // is the same class as java.util.concurrent.CancellationException)
        if (e instanceof CancellationException) {
            throw (CancellationException) e;
        }

        Log.e("AppError", e.getMessage() != null ? e.getMessage() : "Unknown error", e);

        // Order matters: subclasses of IOException must come before IOException
        if (e instanceof UnknownHostException) {
            return "No internet connection. Please check your network and try again.";
        } else if (e instanceof SocketTimeoutException) {
            return "The connection timed out. Please check your internet speed and try again.";
        } else if (e instanceof ConnectException) {
            return "Unable to connect to the server. Please try again later.";
        } else if (e instanceof SSLException) {
            return "A secure connection could not be established. Please check your date/time settings or network.";
        } else if (e instanceof HttpException) {
            return getHttpErrorMessage(((HttpException) e).code(),"");
        } else if (e instanceof JsonSyntaxException) {
            return "We received an unexpected response from the server. Please try again.";
        } else if (e instanceof IOException) {
            return "A network error occurred. Please check your connection and try again.";
        } else {
            return "Something unexpected happened. Please try again.";
        }
    }



    public static String getSerial(Context context) {
        String serial= Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
        return serial;
    }
}
