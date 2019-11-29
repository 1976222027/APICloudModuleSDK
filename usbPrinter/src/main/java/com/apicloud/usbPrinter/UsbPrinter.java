package com.apicloud.usbPrinter;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.apicloud.usbPrinter.xinye.LabelUtils;
import com.apicloud.usbPrinter.xinye.UsbPrinters;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.tools.command.EscCommand;
import com.tools.command.LabelCommand;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.apicloud.usbPrinter.Constant.ACTION_USB_PERMISSION;
import static com.apicloud.usbPrinter.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.apicloud.usbPrinter.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.apicloud.usbPrinter.DeviceConnFactoryManager.CONN_STATE_FAILED;


/**
 * Created by Administrator on 2018/12/10.
 */

public class UsbPrinter extends UZModule {
    private static final String DEBUG_TAG = "UsbPrinterss";
    List<String> mUsbDevice;
    private static Context mContext;
    private int id = 0;
    private PendingIntent mPermissionIntent;
    private ThreadPool threadPool;

    public UsbPrinter(UZWebView webView) {
        super(webView);
        mContext = getContext();
    }
    void sendReceiptWithResponse() {
        final JSONArray dataArr = sendContext.optJSONArray("data");
        if ((dataArr == null) || (dataArr.length() == 0)) {
            try {
                JSONObject ret = new JSONObject();
                ret.put("status", false);
                ret.put("msg", "打印小票数据不能为空");
                sendContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject itemDataObj = dataArr.optJSONObject(i);
            String rowtype = itemDataObj.optString("rowtype");
            if (!TextUtils.isEmpty(rowtype)) {
                if ("printQRCode".equals(rowtype)) {
                    String datas = itemDataObj.optString("data");
                    String alignment = itemDataObj.optString("alignment", "left");
                    // 取消倍高倍宽
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    if (2 == isInstruction) {
                        try {
                            Bitmap bmp = encodeQRCode(datas, ErrorCorrectionLevel.L, 8);
                            esc.addRastBitImage(bmp, 140, 0);
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // 设置纠错等级
                        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                        // 设置qrcode模块大小
                        esc.addSelectSizeOfModuleForQRCode((byte) 6);
                        // 设置qrcode内容
                        esc.addStoreQRCodeData(datas);
                        esc.addPrintQRCode();// 打印QRCode
                    }
                } else if ("printTitle".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                } else if ("printColumnsText".equals(rowtype)) {
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    JSONArray colsTextArr = itemDataObj.optJSONArray("colsTextArr");
                    JSONArray colsWidthArr = itemDataObj.optJSONArray("colsWidthArr");
                    JSONArray colsAlign = itemDataObj.optJSONArray("colsAlign");
                    String[] text = new String[colsTextArr.length()];
                    for (int j = 0; j < colsTextArr.length(); j++) {
                        text[j] = colsTextArr.optString(j);
                    }
                    int[] width = new int[colsWidthArr.length()];
                    for (int j = 0; j < colsWidthArr.length(); j++) {
                        width[j] = colsWidthArr.optInt(j);
                    }
                    int[] align = new int[colsAlign.length()];
                    for (int j = 0; j < colsAlign.length(); j++) {
                        align[j] = colsAlign.optInt(j);
                    }
                    String str = "";
                    for (int k = 0; k < text.length; k++) {
                        String s = text[k];
                        int x = getlength(text[k]);
                        int y = width[k];
                        int b = y - x;
                        if (align[k] == 0) {
                            for (int a = 0; a < b; a++) {
                                s = s + " ";
                            }
                        } else if (align[k] == 1) {
                            if (b % 2 == 0) {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                            } else {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                                s = " " + s;
                            }
                        } else if (align[k] == 2) {
                            for (int a = 0; a < b; a++) {
                                s = " " + s;
                            }
                        }
                        str += s;
                    }
                    str += "\n";
                    esc.addText(str);
                } else if ("printText".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                }

            }
        }
        if (isbox) {
            // 开钱箱
            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//            esc.addPrintAndFeedLines((byte) 8);
        }
        esc.addCutPaper();
//        // 加入查询打印机状态，用于连续打印
//        byte[] bytes = {29, 114, 1};
//        esc.addUserCommand(bytes);
        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }

    public static Context getContexts() {
        return mContext;
    }

    boolean isXpriner;//是否芯烨打印机
    UsbPrinters p;
//请求权限
    public void jsmethod_requestPermission(UZModuleContext moduleContext) {
//        isJiabo = moduleContext.optBoolean("isJiaBo");
//        if (isJiabo) {
//            JSONObject ret = new JSONObject();
//            try {
//                ret.put("status", true);
//                moduleContext.success(ret, true);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else {
        UsbPrinters.requestUsbPrinter(getContext(), moduleContext);
//        }
    }

    /**
     * 打印小票
     *
     * @param moduleContext
     */
    UZModuleContext sendContext;

    public void jsmethod_printData(UZModuleContext moduleContext) {
        final JSONArray dataArr = moduleContext.optJSONArray("data");
        if ((dataArr == null) || (dataArr.length() == 0)) {
            try {
                JSONObject ret = new JSONObject();
                ret.put("status", false);
                ret.put("msg", "打印小票数据不能为空");
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        if (isXpriner) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            Log.e("wang", "开始打印");
                            sendReceiptWithResponse(dataArr);
                        }
                    }
            ).start();
        } else {
            sendContext = moduleContext;
            threadPool = ThreadPool.getInstantiation();
            threadPool.addTask(new Runnable() {
                @Override
                public void run() {
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                            !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                        mHandler.obtainMessage(CONN_PRINTER).sendToTarget();
                        return;
                    }
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                        sendReceiptWithResponse();
                    } else {
                        mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget();
                    }
                }
            });
        }
        if (receiver != null) {
            getContext().unregisterReceiver(receiver);
        }

    }

    /**
     * 开启钱箱
     *
     * @param moduleContext
     */
    public void jsmethod_openCashBox(UZModuleContext moduleContext) {
        sendContext = moduleContext;
        if (isXpriner) {
//            EscCommand esc = new EscCommand();
//            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//            Vector<Byte> datasss = esc.getCommand(); // 发送数据
//            byte[] bytes = LabelUtils.ByteTo_byte(datasss);
//            String str = Base64.encodeToString(bytes, Base64.DEFAULT);
//            final byte[] decode_datas = Base64.decode(str, Base64.DEFAULT);
//            if(p!=null){
//                p.close();
//                p=null;
//            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        p = UsbPrinters.open(getContext());
//                        if(p.ready()){
//                            p.write(decode_datas);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();

            EscCommand esc = new EscCommand();
            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
            Vector<Byte> datasss = esc.getCommand(); // 发送数据
            byte[] bytes = LabelUtils.ByteTo_byte(datasss);
            String str = Base64.encodeToString(bytes, Base64.DEFAULT);
            byte[] decode_datas = Base64.decode(str, Base64.DEFAULT);
            try {
                p.write(decode_datas);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            byte[] cmd = {0x10, 0x14, 1, 0, 5};
//            try {
//                p.write(cmd);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else {
            threadPool = ThreadPool.getInstantiation();
            threadPool.addTask(new Runnable() {
                @Override
                public void run() {
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                            !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                        mHandler.obtainMessage(CONN_PRINTER).sendToTarget();
                        return;
                    }
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                        EscCommand esc = new EscCommand();
                        // 开钱箱
                        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
                        Vector<Byte> datas = esc.getCommand();
                        // 发送数据
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
                    } else {
//                    mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget();
                    }
                }
            });
        }
        if (receiver != null) {
            getContext().unregisterReceiver(receiver);
        }
    }

    /**
     * 初始化小票打印机
     *
     * @param moduleContext
     */
    UZModuleContext backContext;
    int isInstruction;
    boolean isbox;
    int port;
    String mPrinterIp;

    public void jsmethod_initPrint(UZModuleContext moduleContext) {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_QUERY_PRINTER_STATE);
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        getContext().registerReceiver(receiver, filter);
        mUsbDevice = new ArrayList<>();
        backContext = moduleContext;
        String type = moduleContext.optString("type");
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(mContext, "请输入打印机类型", Toast.LENGTH_SHORT).show();
            return;
        }
        isInstruction = moduleContext.optInt("isinstruction", 1);
        isbox = moduleContext.optBoolean("isbox", true);
        mPrinterIp = moduleContext.optString("mPrinterIp");
        port = moduleContext.optInt("port", 9100);
        isXpriner = false;
        if (type.equals("usb-Xprinter")) {
            if (p != null) {
                p.close();
                p = null;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        p = UsbPrinters.open(getContext());
                        if (p.ready()) {
                            Log.e("wang", "初始化成功");
                            JSONObject ret = new JSONObject();
                            try {
                                ret.put("status", true);
                                isXpriner = true;
                                backContext.success(ret, true);
                                return;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            JSONObject ret = new JSONObject();
                            try {
                                ret.put("status", false);
                                backContext.success(ret, true);
                                return;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        } else if (type.equals("usb")) {
            getUsbDeviceList();
//             usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
//            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null) {
//                JSONObject ret = new JSONObject();
//                try {
//                    ret.put("status", true);
//                    backContext.success(ret, true);
//                    return;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
            closeport();
            if (mUsbDevice.size() <= 0) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "usb未连接打印机");
                    backContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return;
            }
            //获取USB设备名
            String usbName = mUsbDevice.get(0);
            //通过USB设备名找到USB设备
            UsbDevice usbDevice = Utils.getUsbDeviceFromName(getContext(), usbName);
            //判断USB设备是否有权限
            if (usbManager.hasPermission(usbDevice)) {
                usbConn(usbDevice);
            } else {//请求权限
                mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(usbDevice, mPermissionIntent);
            }
        } else {
            int IP1;  //IP的第一个数
            int IP2;  //IP的第二个数
            int IP3;  //IP的第三个数
            int IP4;  //IP的第四个数
            String[] split = mPrinterIp.split("\\.");
            //检查输入的IP地址是否有效
            if (split.length != 4) {
                Toast.makeText(getContext(), "请输入正确的IP地址", Toast.LENGTH_SHORT).show();
                return;
            }
            IP1 = Integer.parseInt(split[0]);
            IP2 = Integer.parseInt(split[1]);
            IP3 = Integer.parseInt(split[2]);
            IP4 = Integer.parseInt(split[3]);
            if (IP1 > 255 || IP2 > 255 || IP3 > 255 || IP4 > 255) {
                Toast.makeText(getContext(), "IP地址无效", Toast.LENGTH_SHORT).show();
                return;
            }
            //初始化端口信息
            new DeviceConnFactoryManager.Build()
                    //设置端口连接方式
                    .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                    //设置端口IP地址
                    .setIp(mPrinterIp)
                    //设置端口ID（主要用于连接多设备）
                    .setId(id)
                    //设置连接的热点端口号
                    .setPort(port)
                    .build();
            threadPool = ThreadPool.getInstantiation();
            threadPool.addTask(new Runnable() {
                @Override
                public void run() {
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                }
            });
        }
    }
    UsbManager usbManager;
    public void getUsbDeviceList() {
         usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        // Get the list of attached devices
        HashMap<String, UsbDevice> devices = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        Log.d(DEBUG_TAG, "count " + count);
        if (count > 0) {
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                String devicename = device.getDeviceName();
                Log.e("wang", devicename + "---------");
                if (checkUsbDevicePidVid(device)) {
                    mUsbDevice.add(devicename);
                }
            }
        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "usb未连接打印机");
                backContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
    }

    boolean checkUsbDevicePidVid(UsbDevice dev) {
        int pid = dev.getProductId();
        int vid = dev.getVendorId();
        return ((vid == 34918 && pid == 256) || (vid == 1137 && pid == 85)
                || (vid == 6790 && pid == 30084)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 512)
                || (vid == 26728 && pid == 256) || (vid == 26728 && pid == 768)
                || (vid == 26728 && pid == 1024) || (vid == 26728 && pid == 1280)
                || (vid == 26728 && pid == 1536));
    }

    /**
     * usb连接
     *
     * @param usbDevice
     */
    private void usbConn(UsbDevice usbDevice) {
        new DeviceConnFactoryManager.Build()
                .setId(id)
                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.USB)
                .setUsbDevice(usbDevice)
                .setContext(getContext())
                .build();
        Log.e("wang", "id=====" + id);
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                System.out.println("permission ok for device " + device);
                                usbConn(device);
                            }
                        } else {
                            System.out.println("permission denied for device " + device);
                        }
                    }
                    break;
                //Usb连接断开、蓝牙连接断开广播
                case ACTION_USB_DEVICE_DETACHED:
                    mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
                    break;
                case DeviceConnFactoryManager.ACTION_CONN_STATE:
                    int state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1);
                    int deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1);
                    switch (state) {
                        case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
                            if (id == deviceId) {
//                                tvConnState.setText(getString(R.string.str_conn_state_disconnect));
//                                Toast.makeText(context, R.string.str_conn_state_disconnect, Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
//                            tvConnState.setText(getString(R.string.str_conn_state_connecting));
                            Toast.makeText(context, "打印机连接中", Toast.LENGTH_SHORT).show();
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                            JSONObject ret = new JSONObject();
                            try {
                                ret.put("status", true);
                                ret.put("msg", getConnDeviceInfo());
                                backContext.success(ret, false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            Toast.makeText(context, R.string.str_conn_state_connected + "\n" + getConnDeviceInfo(), Toast.LENGTH_SHORT).show();
                            break;
                        case CONN_STATE_FAILED:
                            JSONObject ret1 = new JSONObject();
                            try {
                                ret1.put("status", false);
                                ret1.put("msg", "连接失败！");
                                backContext.success(ret1, false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            Utils.toast(getContext(), getContext().getString(R.string.str_conn_fail));
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_QUERY_PRINTER_STATE:
//                    if (counts >=0) {
//                        if(continuityprint) {
//                            printcount++;
//                            Utils.toast(MainActivity.this, getString(R.string.str_continuityprinter) + " " + printcount);
//                        }
//                        if(counts!=0) {
//                            sendContinuityPrint();
//                        }else {
//                            continuityprint=false;
//                        }
//                    }
                    break;
                default:
                    break;
            }
        }
    };

    private static final int CONN_PRINTER = 0x12;
    /**
     * 连接状态断开
     */
    private static final int CONN_STATE_DISCONN = 0x007;
    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONN_STATE_DISCONN:
//                    if (isXpriner) {
////                        Utils.toast(getContext(), getContext().getString(R.string.str_disconnect_success));
//                    } else {
//                        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
//                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
////                            Utils.toast(getContext(), getContext().getString(R.string.str_disconnect_success));
//                        }
//                    }
                    break;
                case PRINTER_COMMAND_ERROR:
//                    Utils.toast(getContext(), getContext().getString(R.string.str_choice_printer_command));
                    break;
                case CONN_PRINTER:
//                    Utils.toast(getContext(), getContext().getString(R.string.str_cann_printer));
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
//                        ret.put("msg", "打印机未初始化成功");
                        sendContext.success(ret, true);
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MESSAGE_UPDATE_PARAMETER:
                    String strIp = msg.getData().getString("Ip");
                    String strPort = msg.getData().getString("Port");
                    //初始化端口信息
                    new DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp(strIp)
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(Integer.parseInt(strPort))
                            .build();
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
                default:
                    new DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp("192.168.2.227")
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(9100)
                            .build();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
            }
        }
    };

    private String getConnDeviceInfo() {
        String str = "";
        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if (deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState()) {
            if ("USB".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "USB\n";
                str += "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ("WIFI".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "WIFI\n";
                str += "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str += "Port: " + deviceConnFactoryManager.getPort();
            } else if ("BLUETOOTH".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "BLUETOOTH\n";
                str += "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ("SERIAL_PORT".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "SERIAL_PORT\n";
                str += "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str += "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return str;
    }

    /**
     * 重新连接回收上次连接的对象，避免内存泄漏
     */
    private void closeport() {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
        }
    }


    /**
     * 判断字符串长度
     *
     * @param s
     * @return
     */
    public static int getlength(String s) {
        if (s == null) {
            return 0;
        }
        char[] c = s.toCharArray();
        int len = 0;
        for (int i = 0; i < c.length; i++) {
            len++;
            if (!isLetter(c[i])) {
                len++;
            }
        }
        return len;
    }

    public static boolean isLetter(char c) {
        int k = 0x80;
        return c / k == 0 ? true : false;
    }


    private Bitmap encodeQRCode(String text, ErrorCorrectionLevel errorCorrectionLevel,
                                int scale) throws WriterException {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
//        QRCode code = new QRCode();
//        Encoder.encode(text, errorCorrectionLevel, hints, code);
        QRCode code = Encoder.encode(text, errorCorrectionLevel, hints);
        final ByteMatrix m = code.getMatrix();
        final int mw = m.getWidth();
        final int mh = m.getHeight();
        final int IMG_WIDTH = mw * scale;
        final int IMG_HEIGHT = mh * scale;
        Bitmap bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        c.drawColor(Color.WHITE);
        p.setColor(Color.BLACK);
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < mw; x++) {
                if (m.get(x, y) == 1) {
                    c.drawRect(x * scale, y * scale,
                            (x + 1) * scale, (y + 1) * scale, p);
                }
            }
        }
        return bmp;
    }


    void sendReceiptWithResponse(JSONArray dataArr) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        for (int i = 0; i < dataArr.length(); i++) {
            JSONObject itemDataObj = dataArr.optJSONObject(i);
            String rowtype = itemDataObj.optString("rowtype");
            if (!TextUtils.isEmpty(rowtype)) {
                if ("printQRCode".equals(rowtype)) {
                    String datas = itemDataObj.optString("data");
                    String alignment = itemDataObj.optString("alignment", "left");
                    // 取消倍高倍宽
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    if (2 == isInstruction) {
                        try {
                            Bitmap bmp = encodeQRCode(datas, ErrorCorrectionLevel.L, 8);
                            esc.addRastBitImage(bmp, 140, 0);
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // 设置纠错等级
                        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                        // 设置qrcode模块大小
                        esc.addSelectSizeOfModuleForQRCode((byte) 5);
                        // 设置qrcode内容
                        esc.addStoreQRCodeData(datas);
                        esc.addPrintQRCode();// 打印QRCode
                    }
                } else if ("printTitle".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                } else if ("printColumnsText".equals(rowtype)) {
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    JSONArray colsTextArr = itemDataObj.optJSONArray("colsTextArr");
                    JSONArray colsWidthArr = itemDataObj.optJSONArray("colsWidthArr");
                    JSONArray colsAlign = itemDataObj.optJSONArray("colsAlign");
                    String[] text = new String[colsTextArr.length()];
                    for (int j = 0; j < colsTextArr.length(); j++) {
                        text[j] = colsTextArr.optString(j);
                    }
                    int[] width = new int[colsWidthArr.length()];
                    for (int j = 0; j < colsWidthArr.length(); j++) {
                        width[j] = colsWidthArr.optInt(j);
                    }
                    int[] align = new int[colsAlign.length()];
                    for (int j = 0; j < colsAlign.length(); j++) {
                        align[j] = colsAlign.optInt(j);
                    }
                    String str = "";
                    for (int k = 0; k < text.length; k++) {
                        String s = text[k];
                        int x = getlength(text[k]);
                        int y = width[k];
                        int b = y - x;
                        if (align[k] == 0) {
                            for (int a = 0; a < b; a++) {
                                s = s + " ";
                            }
                        } else if (align[k] == 1) {
                            if (b % 2 == 0) {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                            } else {
                                for (int a = 0; a < b / 2; a++) {
                                    s = " " + s + " ";
                                }
                                s = " " + s;
                            }
                        } else if (align[k] == 2) {
                            for (int a = 0; a < b; a++) {
                                s = " " + s;
                            }
                        }
                        str += s;
                    }
                    str += "\n";
                    esc.addText(str);
                } else if ("printText".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                }

            }
        }
        if (isbox) {
            // 开钱箱
            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//            esc.addPrintAndFeedLines((byte) 8);
        }
        esc.addCutPaper();
//        // 加入查询打印机状态，用于连续打印
//        byte[] bytes = {29, 114, 1};
//        esc.addUserCommand(bytes);
        Vector<Byte> datass = esc.getCommand(); // 发送数据
        byte[] bytes = LabelUtils.ByteTo_byte(datass);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        byte[] decode_datas = Base64.decode(str, Base64.DEFAULT);
        try {
            p.write(decode_datas);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
