package com.apicloud.myusbprint;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2018/12/10.
 */

public class UsbPrinter extends UZModule implements PrinterUtils.PrinterCallBack {
    private static final String DEBUG_TAG = "UsbPrinterss";
    List<String> mUsbDevice;//设备列表
    private static Context mContext;
    private int id = 0;
    private PendingIntent mPermissionIntent;
    private PrinterUtils printerUtils;

    public static Context getContexts() {
        return mContext;
    }

    public UsbPrinter(final UZWebView webView) {
        super(webView);
//        webView.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if (event.getAction() == KeyEvent.ACTION_DOWN) {
//                    if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {
//                        webView.goBack();
//                        return true;
//                    } else {
//
//                        return false;
//                    }
//                }else {
//                    return false;
//                }
//            }
//        });
        mContext = getContext();
        initViews();
    }

    private void initViews() {
        printerUtils = new PrinterUtils(this.getContext());
        printerUtils.registerPrinterReceiver();
        printerUtils.onPrinterCallBack(this);
        printerUtils.onUSBprinterCallBack(new PrinterUtils.USBprinterCallBack() {
            @Override
            public void cannotFind() {
                Log.e("MainActivity", "找不到打印设备");
                printerUtils.showToast("找不到打印设备");
            }

            @Override
            public void beSucceed() {
                Log.e("MainActivity", "链接成功");
                printerUtils.showToast("链接成功");
            }

            @Override
            public void beDefeated(String details) {
                Log.e("MainActivity", "链接失败"+details);
                printerUtils.showToast(details);
            }
        });

    }

    void sendReceiptWithResponse() {
        printerUtils.sendReceipt(sendContext, isbox, isInstruction);//数据，钱箱 ,初始化

//        final JSONArray dataArr = sendContext.optJSONArray("data");
//        if ((dataArr == null) || (dataArr.length() == 0)) {
//            try {
//                JSONObject ret = new JSONObject();
//                ret.put("status", false);
//                ret.put("msg", "打印小票数据不能为空");
//                sendContext.success(ret, true);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
//        EscCommand esc = new EscCommand();
//        esc.addInitializePrinter();
//        esc.addPrintAndFeedLines((byte) 3);
//        for (int i = 0; i < dataArr.length(); i++) {
//            JSONObject itemDataObj = dataArr.optJSONObject(i);
//            String rowtype = itemDataObj.optString("rowtype");
//            if (!TextUtils.isEmpty(rowtype)) {
//                if ("printQRCode".equals(rowtype)) {
//                    String datas = itemDataObj.optString("data");
//                    String alignment = itemDataObj.optString("alignment", "left");
//                    // 取消倍高倍宽
//                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    if (alignment.equals("right")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
//                    } else if (alignment.equals("left")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//                    } else {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
//                    }
//                    if (2 == isInstruction) {
//                        try {
//                            Bitmap bmp = encodeQRCode(datas, ErrorCorrectionLevel.L, 8);
//                            esc.addRastBitImage(bmp, 100, 0);
//                        } catch (WriterException e) {
//                            e.printStackTrace();
//                        }
//                    } else {
//                        // 设置纠错等级
//                        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
//                        // 设置qrcode模块大小
//                        esc.addSelectSizeOfModuleForQRCode((byte) 5);
//                        // 设置qrcode内容
//                        esc.addStoreQRCodeData(datas);
//                        esc.addPrintQRCode();// 打印QRCode
//                    }
//                } else if ("printTitle".equals(rowtype)) {
//                    String text = itemDataObj.optString("text");
//                    String alignment = itemDataObj.optString("alignment", "left");
//                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
//                    esc.addSetKanjiFontMode(EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
//                    if (alignment.equals("right")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
//                    } else if (alignment.equals("left")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//                    } else {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
//                    }
//                    esc.addText(text);
//                } else if ("printColumnsText".equals(rowtype)) {
//                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//                    JSONArray colsTextArr = itemDataObj.optJSONArray("colsTextArr");
//                    JSONArray colsWidthArr = itemDataObj.optJSONArray("colsWidthArr");
//                    JSONArray colsAlign = itemDataObj.optJSONArray("colsAlign");
//                    String[] text = new String[colsTextArr.length()];
//                    for (int j = 0; j < colsTextArr.length(); j++) {
//                        text[j] = colsTextArr.optString(j);
//                    }
//                    int[] width = new int[colsWidthArr.length()];
//                    for (int j = 0; j < colsWidthArr.length(); j++) {
//                        width[j] = colsWidthArr.optInt(j);
//                    }
//                    int[] align = new int[colsAlign.length()];
//                    for (int j = 0; j < colsAlign.length(); j++) {
//                        align[j] = colsAlign.optInt(j);
//                    }
//                    String str = "";
//                    for (int k = 0; k < text.length; k++) {
//                        String s = text[k];
//                        int x = getlength(text[k]);
//                        int y = width[k];
//                        int b = y - x;
//                        if (align[k] == 0) {
//                            for (int a = 0; a < b; a++) {
//                                s = s + " ";
//                            }
//                        } else if (align[k] == 1) {
//                            if (b % 2 == 0) {
//                                for (int a = 0; a < b / 2; a++) {
//                                    s = " " + s + " ";
//                                }
//                            } else {
//                                for (int a = 0; a < b / 2; a++) {
//                                    s = " " + s + " ";
//                                }
//                                s = " " + s;
//                            }
//                        } else if (align[k] == 2) {
//                            for (int a = 0; a < b; a++) {
//                                s = " " + s;
//                            }
//                        }
//                        str += s;
//                    }
//                    str += "\n";
//                    esc.addText(str);
//                } else if ("printText".equals(rowtype)) {
//                    String text = itemDataObj.optString("text");
//                    String alignment = itemDataObj.optString("alignment", "left");
//                    esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    esc.addSetKanjiFontMode(EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
//                    if (alignment.equals("right")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
//                    } else if (alignment.equals("left")) {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//                    } else {
//                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
//                    }
//                    esc.addText(text);
//                }
//
//            }
//        }
//        if (isbox) {
//            // 开钱箱
//            esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
////            esc.addPrintAndFeedLines((byte) 8);
//        }
//        esc.addCutPaper();
////        // 加入查询打印机状态，用于连续打印
////        byte[] bytes = {29, 114, 1};
////        esc.addUserCommand(bytes);
//        Vector<Byte> datas = esc.getCommand();
//        Log.e("mhy",datas.toString());
//        // 发送数据
//        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }

    //    同步映射在该类下定义public类型，jsmethod前缀sync后缀的return类型函数，接收UZModuleContext类型参数，并同步返回经ModuleResult包装过的数据。
//    如何一个异步接口给Javascript
//    在该类下定义public类型，jsmethod前缀的void类型函数，并接收UZModuleContext类型参数， 将操作结果通过success 或者error回调。
    public void jsmethod_requestPermission(UZModuleContext moduleContext) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            moduleContext.success(ret, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * 打印小票
     *
     * @param moduleContext
     */
    UZModuleContext sendContext;

    public void jsmethod_printData(UZModuleContext moduleContext) {
        sendContext = moduleContext;
        sendReceiptWithResponse();
    }

    /**
     * 开启钱箱
     *
     * @param moduleContext
     */
    public void jsmethod_openCashBox(UZModuleContext moduleContext) {
        sendContext = moduleContext;
        printerUtils.openBox();
//        getContext().unregisterReceiver(receiver);
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
        //检查状态
        try {
            printerUtils.getPrinterStatus();//通过回调 成功就走成功方法
        } catch (RemoteException e) {
            e.printStackTrace();
        }
// *    发送广播 初始 usb**/
        mUsbDevice = new ArrayList<>();
        backContext = moduleContext;
        String type = moduleContext.optString("type");
        isInstruction = moduleContext.optInt("isinstruction", 1);
        isbox = moduleContext.optBoolean("isbox", true);
        mPrinterIp = moduleContext.optString("mPrinterIp");
        port = moduleContext.optInt("port", 9100);
        if (type.equals("usb")) {
            getUsbDeviceList();
            UsbManager usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
            //判断初始化过的
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
            Log.e("wang", mUsbDevice.get(0) + "获取设备名");
            //通过USB设备名找到USB设备
            UsbDevice usbDevice = PrinterUtils.getUsbDeviceFromName(getContext(), usbName);
            //判断USB设备是否有权限
            if (usbManager.hasPermission(usbDevice)) {
                Log.e("wang", usbDevice + "已有权限");
                usbConn(usbDevice);
            } else {//请求权限
                Log.e("wang", usbDevice + "去申请权限");
//                mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
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

        }
    }

    public void getUsbDeviceList() {
        UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        // Get the list of attached devices
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
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
                || (vid == 26728 && pid == 1536) || (vid == 1155 && pid == 1803));
    }

    /**
     * usb连接
     *
     * @param usbDevice
     */
    private void usbConn(UsbDevice usbDevice) {
        printerUtils.initUsb();
//        new DeviceConnFactoryManager.Build()
//                .setId(id)
//                .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.USB)
//                .setUsbDevice(usbDevice)
//                .setContext(getContext())
//                .build();
////        Log.e("wang", "id=====" + id);
//        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
    }

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
                case PRINTER_COMMAND_ERROR:
//                    Utils.toast(getContext(), getContext().getString(R.string.str_choice_printer_command));
                    break;
                case CONN_PRINTER:
//                    Utils.toast(getContext(), getContext().getString(R.string.str_cann_printer));
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("msg", "打印机未初始化成功");
                        sendContext.success(ret, true);
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    private String getConnDeviceInfo() {
        return "USB";
    }

    /**
     * 重新连接回收上次连接的对象，避免内存泄漏
     */
    private void closeport() {

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


    @Override
    public void beSucceed() {
        Log.e("MainActivity", "链接成功");
        printerUtils.showToast("链接成功");
//        printerUtils.sendReceipt();
//        printerUtils.sendReceipt(moduleContext);
    }

    @Override
    public void beDefeated(String details) {
        Log.e("MainActivity", "链接失败" + details);
        printerUtils.showToast(details);
    }

    /**
     * 页面关闭时调用
     */
    public void jsmethod_close() {
        printerUtils.unPrinterService();
    }
}
