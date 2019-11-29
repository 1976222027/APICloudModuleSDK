package com.apicloud.myusbprint;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gprinter.command.EscCommand;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.jimmy.printer.common.PrinterFinderCallback;
import com.jimmy.printer.common.SendCallback;
import com.jimmy.printer.common.SendResultCode;
import com.jimmy.printer.ethernet.EthernetPrint;
import com.jimmy.printer.usb.UsbPrinter;
import com.jimmy.printer.usb.UsbPrinterFinder;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * USB打印模块
 */

public class UsbPrint extends UZModule {
    private com.jimmy.printer.usb.UsbPrint usbPrint;//usb打印工具
    private EthernetPrint ethernetPrint;//WiFi打印机
    private UsbPrinterFinder printerFinder;//查找打印机

    private static final String TAG = "UsbPrint";
    List<com.jimmy.printer.usb.UsbPrinter> mUsblist;//设备列表
    List<String> mUsbDevice;//设备列表
    private static Context mContext;

    public static Context getContexts() {
        return mContext;
    }

    public UsbPrint(final UZWebView webView) {
        super(webView);
        mContext = getContext();
        Log.e("mhywebview", "初始化");
    }

    private void initData() {
        if (mUsblist == null) {
            mUsblist = new ArrayList<>();
        }
        //初始化 USB打印工具
        if (usbPrint == null) {
            usbPrint = com.jimmy.printer.usb.UsbPrint.getInstance(getContext(), sendCallback);
        }
//        printerFinder = new UsbPrinterFinder(getContext(), printerFinderCallback);
//        printerFinder.startFinder();
    }

    public void initWifi() {
        if (mUsblist == null) {
            mUsblist = new ArrayList<>();
        }
        //初始化 WiFi 打印
        if (ethernetPrint == null) {
            ethernetPrint = EthernetPrint.getInstance(sendCallback);
            requestPermission();
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getContext(),
                    new String[]{Manifest.permission.INTERNET},
                    1);
        }
    }

    public boolean isIP(String address) {
        if (address.length() < 7 || address.length() > 15 || "".equals(address)) {
            return false;
        }
        String regex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(address);
        return mat.find();
    }

    public void testPrint() {
        String ip = mPrinterIp;
        String sport = port + "";
        boolean isIp = isIP(ip);
        if (!isIp) {
            Toast.makeText(getContext(), "ip格式错误", Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(sport)) {
            Toast.makeText(getContext(), "端口不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        com.jimmy.printer.command.EscCommand esc = new com.jimmy.printer.command.EscCommand();
        for (int i = 0; i < 13; i++) {
            esc.addSelectJustification(0);
            esc.addText("This 左\n");
            esc.addSelectJustification(1);
            esc.addText("This 中\n");
            esc.addSelectJustification(2);
            esc.addText("This 右\n");
        }
        esc.addPrintAndFeedLines((byte) 5);
        esc.addCutPaper();
        ethernetPrint.sendPrintCommand(ip, port/*Integer.parseInt(sport)*/, esc.getByteArrayCommand());
//        for (int i = 0; i < 10; i++) {
//        }
    }

    //回调
    private SendCallback sendCallback = new SendCallback() {
        @Override
        public void onCallback(int code, String printId) {
            String msg = "";
            if (code == SendResultCode.SEND_SUCCESS) {
                msg = "发送成功";
            } else if (code == SendResultCode.SEND_FAILED) {
                msg = "发送失败";
            }
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", msg.equals("发送成功"));
                ret.put("msg", printId + "" + msg);
                sendContext.success(ret, false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            Toast.makeText(getContext(), printId + " " + msg, Toast.LENGTH_LONG).show();
        }
    };

    private PrinterFinderCallback<com.jimmy.printer.usb.UsbPrinter> printerFinderCallback = new PrinterFinderCallback<com.jimmy.printer.usb.UsbPrinter>() {
        JSONObject ret = new JSONObject();

        @Override
        public void onStart() {
            Log.d(TAG, "startFind开始查找打印机 print");
        }

        @Override
        public void onFound(com.jimmy.printer.usb.UsbPrinter usbPrinter) {
            //listAdapter.addData(usbPrinter);
            Log.d(TAG, "onFound 设备名deviceName = " + usbPrinter.getPrinterName());
            try {
                ret.put("status", true);
                ret.put("msg", "找到设备" + usbPrinter.getPrinterName() +
                        "\nPID = " + usbPrinter.getUsbDevice().getProductId() +
                        "\nVID = " + usbPrinter.getUsbDevice().getVendorId());
                backContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFinished(List<UsbPrinter> usbPrinters) {
            Log.d(TAG, "打印机数：printCount = " + usbPrinters.size());
            //获取打印机
            mUsblist.clear();//add 每次拔插更新
            mUsblist.addAll(usbPrinters);
            if (mUsblist.size() == 0) {
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "USB设备失联");
                    backContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * 页面关闭时调用
     */
    public void jsmethod_closeBr() {
        if (printerFinder != null) {
            printerFinder.unregisterReceiver();
        }
    }

    //打印数据
    public void sendReceipt(UZModuleContext sendContext, boolean isbox, int isInstruction, UsbPrinter item) {
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
        com.gprinter.command.EscCommand esc = new com.gprinter.command.EscCommand();
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
                    esc.addSelectPrintModes(com.gprinter.command.EscCommand.FONT.FONTA, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.CENTER);
                    }
//                    if (2 == isInstruction) {
//                        try {
//                            Bitmap bmp = encodeQRCode(datas, ErrorCorrectionLevel.L, 8);
//                            esc.addRastBitImage(bmp, 100, 0);
//                        } catch (WriterException e) {
//                            e.printStackTrace();
//                        }
//                    } else {
                    // 设置纠错等级
                    esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                    // 设置qrcode模块大小
                    esc.addSelectSizeOfModuleForQRCode((byte) 5);
                    // 设置qrcode内容
                    esc.addStoreQRCodeData(datas);
                    esc.addPrintQRCode();// 打印QRCode
//                    }
                } else if ("printTitle".equals(rowtype)) {
                    String text = itemDataObj.optString("text");
                    String alignment = itemDataObj.optString("alignment", "left");
                    esc.addSelectPrintModes(com.gprinter.command.EscCommand.FONT.FONTA, com.gprinter.command.EscCommand.ENABLE.ON, com.gprinter.command.EscCommand.ENABLE.ON, com.gprinter.command.EscCommand.ENABLE.ON, com.gprinter.command.EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(com.gprinter.command.EscCommand.ENABLE.ON, com.gprinter.command.EscCommand.ENABLE.ON, com.gprinter.command.EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.CENTER);
                    }
                    esc.addText(text);
                } else if ("printColumnsText".equals(rowtype)) {
                    esc.addSelectPrintModes(com.gprinter.command.EscCommand.FONT.FONTA, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.LEFT);
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
                    esc.addSelectPrintModes(com.gprinter.command.EscCommand.FONT.FONTA, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    esc.addSetKanjiFontMode(com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF, com.gprinter.command.EscCommand.ENABLE.OFF);
                    if (alignment.equals("right")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.RIGHT);
                    } else if (alignment.equals("left")) {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.LEFT);
                    } else {
                        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.CENTER);
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
        Log.e("mhy", datas.toString());
        // 发送数据
//        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        //发送数据给打印机
        usbPrint.sendPrintCommand(item, bytes);
        //发送WiFi打印机
        ethernetPrint.sendPrintCommand(mPrinterIp, port/*Integer.parseInt(sport)*/, bytes);

    }

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
        return c / k == 0;
    }

    public void openBox() {
        com.gprinter.command.EscCommand esc = new com.gprinter.command.EscCommand();
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//        esc.addPrintAndFeedLines((byte) 8);
        Vector<Byte> datas = esc.getCommand();
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        //发送数据给打印机
        usbPrint.sendPrintCommand(mUsblist.get(0), bytes);
    }

    /**
     * 打印
     */
    public void sendReceipt() {

        com.gprinter.command.EscCommand esc = new com.gprinter.command.EscCommand();
//        esc.addInitializePrinter();
//        esc.addPrintAndFeedLines((byte) 3);
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
//        esc.addText("Sample\n"); // 打印文字
//        esc.addPrintAndLineFeed();
//
//        /* 打印文字 */
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
//        esc.addText("Print text\n"); // 打印文字
//        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字
//
//        /* 打印繁体中文 需要打印机支持繁体字库 */
//        String message = "佳博智匯票據打印機\n";
//        // esc.addText(message,"BIG5");
//        esc.addText(message, "GB2312");
//        esc.addPrintAndLineFeed();
//
//        /* 绝对位置 具体详细信息请查看GP58编程手册 */
//        esc.addText("智汇");
//        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
//        esc.addSetAbsolutePrintPosition((short) 6);
//        esc.addText("网络");
//        esc.addSetAbsolutePrintPosition((short) 10);
//        esc.addText("设备");
//        esc.addPrintAndLineFeed();
//

        /* 打印一维条码 */
        esc.addText("Print code128\n"); // 打印文字
        esc.addSelectPrintingPositionForHRICharacters(com.gprinter.command.EscCommand.HRI_POSITION.BELOW);//
        // 设置条码可识别字符位置在条码下方
        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
        esc.addPrintAndLineFeed();

        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        esc.addSelectJustification(com.gprinter.command.EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("打印结束!\r\n"); // 打印结束
        esc.addPrintAndFeedLines((byte) 3);
        esc.addCutPaper();//切纸
// 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        for (UsbPrinter usbPrinter : mUsblist) {
            usbPrint.sendPrintCommand(usbPrinter, bytes);
        }

    }

    void sendReceiptWithResponse() {
        EscCommand esc = new EscCommand();
        esc.addText("This 1\n");
        esc.addText("This 2\n");
        esc.addText("This 3\n");
        esc.addText("This 4\n");
        esc.addPrintAndFeedLines((byte) 4);//4行空白
        esc.addCutPaper();//切纸
        //esc.addCleanCache();
        //发送数据给打印机
        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        for (UsbPrinter usbPrinter : mUsblist) {
            usbPrint.sendPrintCommand(usbPrinter, bytes/*esc.getByteArrayCommand()*/);
        }


        //sendReceipt(sendContext, isbox, isInstruction);//数据，钱箱 ,初始化
    }

    //    同步映射在该类下定义public类型，jsmethod前缀sync后缀的return类型函数，接收UZModuleContext类型参数，并同步返回经ModuleResult包装过的数据。
//    如何一个异步接口给Javascript
//    在该类下定义public类型，jsmethod前缀的void类型函数，并接收UZModuleContext类型参数， 将操作结果通过success 或者error回调。
    public void jsmethod_requestPermission(UZModuleContext moduleContext) {
        backContext = moduleContext;
        if (type.equals("usb")||type.equals("usb-Xprinter")) {
            if (usbPrint != null) {
                if (printerFinder == null) {
                    printerFinder = new UsbPrinterFinder(getContext(), printerFinderCallback);
                }
                printerFinder.startFinder();
            } else {
//            JSONObject ret = new JSONObject();
//            try {
//                ret.put("status", false);
//                ret.put("msg", "未初始化");
//                moduleContext.success(ret, true);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
                initData();
                if (printerFinder == null) {
                    printerFinder = new UsbPrinterFinder(getContext(), printerFinderCallback);
                }
                printerFinder.startFinder();
            }
        }else{
                initWifi();//TODO 如何判断WiFi连接
        }
    }


    /**
     * 打印小票
     *
     * @param moduleContext
     */
    UZModuleContext sendContext;//打印数据

    public void jsmethod_printData(UZModuleContext moduleContext) {
        sendContext = moduleContext;
//        sendReceiptWithResponse();
//        sendReceipt();

        if (usbPrint != null) {
            if (printerFinder == null) {
                // printerFinder = new UsbPrinterFinder(getContext(), printerFinderCallback);
                //  printerFinder.startFinder();return;
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未获取权限，未找到usb设备");
                    backContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (mUsblist.size() == 0) {
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("msg", "USB设备未连接");
                        backContext.success(ret, false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (UsbPrinter usbPrinter : mUsblist) {
                        sendReceipt(moduleContext, isbox, isInstruction, usbPrinter);
                    }

                }
            }
        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "未初始化");
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        jsmethod_closeBr();
    }

    /**
     * 开启钱箱
     *
     * @param moduleContext
     */
    public void jsmethod_openCashBox(UZModuleContext moduleContext) {
        sendContext = moduleContext;
//        getContext().unregisterReceiver(receiver);
        if (usbPrint != null) {
            if (printerFinder == null) {
                // printerFinder = new UsbPrinterFinder(getContext(), printerFinderCallback);
                //  printerFinder.startFinder();return;
                JSONObject ret = new JSONObject();
                try {
                    ret.put("status", false);
                    ret.put("msg", "未获取权限，未找到usb设备");
                    backContext.success(ret, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (mUsblist.size() == 0) {
                    JSONObject ret = new JSONObject();
                    try {
                        ret.put("status", false);
                        ret.put("msg", "USB设备未连接");
                        backContext.success(ret, false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (mUsblist.size() != 0) {
                    openBox();
                }
            }
        } else {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("msg", "未初始化");
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        jsmethod_closeBr();
//        openBox();
//        if (mUsblist.size() == 0) {
//            JSONObject ret = new JSONObject();
//            try {
//                ret.put("status", false);
//                ret.put("msg", "USB设备未连接");
//                backContext.success(ret, false);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else  {
//            openBox();
//        }
    }

    /**
     * 初始化小票打印机
     *
     * @param moduleContext
     */
    UZModuleContext backContext;//打印机配置
    int isInstruction;
    boolean isbox;
    int port;
    String mPrinterIp, type = "wifi";
    boolean isXpriner = false;

    public void jsmethod_initPrint(UZModuleContext moduleContext) {
        // * 初始 usb**/
        if (mUsbDevice == null) {
            mUsbDevice = new ArrayList<>();
        }
        backContext = moduleContext;
        type = moduleContext.optString("type");
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(mContext, "请输入打印机类型", Toast.LENGTH_SHORT).show();
            return;
        }
        isInstruction = moduleContext.optInt("isinstruction", 1);
        isbox = moduleContext.optBoolean("isbox", true);
        mPrinterIp = moduleContext.optString("mPrinterIp");
        port = moduleContext.optInt("port", 9100);
//        if (usbPrint == null) {
//            initData();
//        }
        isXpriner = false;
        switch (type) {
            case "usb-Xprinter":
                isXpriner = true;
                initData();
                break;
            case "usb":
                initData();
                break;
            default:
                initWifi();
                boolean isIp = isIP(mPrinterIp);
                if (!isIp) {
                    Toast.makeText(getContext(), "IP地址格式错误", Toast.LENGTH_LONG).show();
                    return;
                }
                if (TextUtils.isEmpty(port + "")) {
                    Toast.makeText(getContext(), "端口不能为空", Toast.LENGTH_LONG).show();
                    return;
                }
                break;
        }
//        isXpriner = false;
//        if (type.equals("usb-Xprinter")) {
//            isXpriner = true;
//        } else if (type.equals("usb")) {
//            getUsbDeviceList();
//            if (mUsbDevice.size() <= 0) {
//                JSONObject ret = new JSONObject();
//                try {
//                    ret.put("status", false);
//                    ret.put("msg", "usb未连接打印机");
//                    backContext.success(ret, false);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                return;
//            }
//            //通过USB设备名找到USB设备
//            UsbDevice usbDevice = deviceList.get(0);
//            //判断USB设备是否有权限
//            if (manager.hasPermission(usbDevice)) {
//                //连接 usb
//            } else {//请求权限
//                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("com.android.example.USB_PERMISSION"), 0);
//                manager.requestPermission(usbDevice, mPermissionIntent);
//            }
//        } else {
//
////            int IP1;  //IP的第一个数
////            int IP2;  //IP的第二个数
////            int IP3;  //IP的第三个数
////            int IP4;  //IP的第四个数
////            String[] split = mPrinterIp.split("\\.");
////            //检查输入的IP地址是否有效
////            if (split.length != 4) {
////                Toast.makeText(getContext(), "请输入正确的IP地址", Toast.LENGTH_SHORT).show();
////                return;
////            }
////            IP1 = Integer.parseInt(split[0]);
////            IP2 = Integer.parseInt(split[1]);
////            IP3 = Integer.parseInt(split[2]);
////            IP4 = Integer.parseInt(split[3]);
////            if (IP1 > 255 || IP2 > 255 || IP3 > 255 || IP4 > 255) {
////                Toast.makeText(getContext(), "IP地址无效", Toast.LENGTH_SHORT).show();
////                return;
////            }
//            //初始化端口信息
//
//        }
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            ret.put("msg", "初始化完成");
            backContext.success(ret, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    UsbManager manager;
    List<UsbDevice> deviceList = new ArrayList<>();
    UsbDevice device;

    public void getUsbDeviceList() {
        manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        // Get the list of attached devices
        HashMap<String, UsbDevice> devices = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        Log.d(TAG, "count " + count);
        if (count > 0) {
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                String devicename = device.getDeviceName();
                Log.e("wang", devicename + "---------");
                if (checkUsbDevicePidVid(device)) {
                    mUsbDevice.add(devicename);
                    deviceList.add(device);
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


}
