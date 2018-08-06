
package cn.sj.cordova.bluetoothprint;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.io.OutputStream;


import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
// import android.content.ServiceConnection;
// import android.content.ComponentName;
// import android.content.Context;
// import android.content.Intent;
// import android.os.Bundle;
// import android.os.IBinder;
import android.content.*;
// import android.content.ContextWrapper;
import android.os.*;
// import android.os.RemoteException;
import android.app.Activity;
import android.view.*;
// import android.view.ContextThemeWrapper;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Xml.Encoding;
import android.util.Base64;
import java.util.ArrayList;
import java.util.List;


public class BluetoothPrintPlugin extends CordovaPlugin {

    private BluetoothAdapter mBluetoothAdapter;

    private Activity activity;
    private String boothAddress = "" ;
    private String oneModel,drawingRev,oneClass,oneCode,chipId,dateTime,specification = "";

    private boolean isConnection = false;//蓝牙是否连接
    private BluetoothDevice device = null;
    private static BluetoothSocket bluetoothSocket = null;
    private static OutputStream outputStream = null;
    private static final UUID uuid = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");



    private static final String LOG_TAG = "BluetoothPrinter";
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    Bitmap bitmap;




    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();

    }



    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //获取已配对的蓝牙设备
        if (action.equals("getPairedDevices")) {
            getPairedDevices(args, callbackContext);
            return true;
        }
        //连接选中的蓝牙设备(打印机)
        if (action.equals("connectDevice")){
            connectDevice(args, callbackContext);
            return true;
        }
        //打印
        if (action.equals("printText")){
            printText(args, callbackContext);
            return true;
        }
        //断开连接
        if (action.equals("closeConnect")){
            closeConnect(args, callbackContext);
            return true;
        }

        if (action.equals("printImage")){
            printImage(args, callbackContext);
            return true;
        }

        return false;
    }

    private void closeConnect(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            bluetoothSocket.close();
            outputStream.close();
            isConnection = false;
            callbackContext.success("断开连接成功！");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            isConnection = true;
            callbackContext.error("断开连接失败！");

        }
    }

    private void connectDevice(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String address = args.getString(0);
        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (!isConnection) {//没有连接
            try {
                bluetoothSocket = device
                        .createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnection = true;
                callbackContext.success("连接成功");
            } catch (Exception e) {
                isConnection = false;
                callbackContext.error("连接失败");
            }
        }else{//连接了
            callbackContext.success("连接成功");
        }

    }

    private void getPairedDevices(final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            JSONArray json = new JSONArray();
            for (BluetoothDevice device : pairedDevices) {
                JSONObject jo = new JSONObject();
                jo.put("name", device.getName());
                jo.put("address", device.getAddress());
                json.put(jo);
            }
            callbackContext.success(json);
        }
        else{
            callbackContext.error("未有配对蓝牙");

        }
    }

    private void printText(final JSONArray args, final CallbackContext callbackContext) throws JSONException{

        String sendData = args.getString(0);

        if (isConnection) {
            try {
                byte[] data = sendData.getBytes("gbk");
                outputStream.write(data, 0, data.length);
                outputStream.flush();
                callbackContext.success("打印成功！");
            } catch (IOException e) {
                callbackContext.error("打印失败！");
            }
        } else {
            callbackContext.error("设备未连接，请重新连接！");
        }
    }



    private void printImage(final JSONArray args, final CallbackContext callbackContext) throws JSONException{

        try {
            String sendData = args.getString(0);
            final String encodedString = sendData;
            final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);
            final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);

            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            bitmap = decodedBitmap;
            int mWidth = bitmap.getWidth();
            int mHeight = bitmap.getHeight();

            bitmap = resizeImage(bitmap, 48 * 8, mHeight);

            byte[] bt = decodeBitmap(bitmap);

            outputStream.write(bt);
            outputStream.flush();

            // tell the user data were sent
            //Log.d(LOG_TAG, "Data Sent");
            callbackContext.success("Data Sent");


        }  catch (IOException e) {
            callbackContext.error(e);
        }

    }

    //New implementation, change old
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    //New implementation
    private static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();

        if (width > w) {
            float scaleWidth = ((float) w) / width;
            float scaleHeight = ((float) h) / height + 24;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleWidth);
            Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                    height, matrix, true);
            return resizedBitmap;
        } else {
            Bitmap resizedBitmap = Bitmap.createBitmap(w, height + 24, Config.RGB_565);
            Canvas canvas = new Canvas(resizedBitmap);
            Paint paint = new Paint();
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, (w - width) / 2, 0, paint);
            return resizedBitmap;
        }
    }

    private static String hexStr = "0123456789ABCDEF";

    private static String[] binaryArray = {"0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111"};

    public static byte[] decodeBitmap(Bitmap bmp) {
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        List<String> list = new ArrayList<String>(); //binaryString list
        StringBuffer sb;
        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;
        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160) {
                    sb.append("0");
                } else {
                    sb.append("1");
                }
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer.toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8 : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
//            Log.d(LOG_TAG, "DECODEBITMAP ERROR : width is too large");
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
//            Log.d(LOG_TAG, "DECODEBITMAP ERROR : height is too large");
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString + widthHexString + heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    public static List<String> binaryListToHexStringList(List<String> list) {
        List<String> hexList = new ArrayList<String>();
        for (String binaryStr : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    public static String myBinaryStrToHexString(String binaryStr) {
        String hex = "";
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }

        return hex;
    }

    public static byte[] hexList2Byte(List<String> list) {
        List<byte[]> commandList = new ArrayList<byte[]>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        byte[] bytes = sysCopy(commandList);
        return bytes;
    }

    public static byte[] sysCopy(List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }




    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}