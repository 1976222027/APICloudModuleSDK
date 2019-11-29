package com.apicloud.usbPrinter.xinye;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UsbPrinters {

    private static final String TAG = "UsbPrinters";
    //private final UsbDevice mDevice;
    private final UsbDeviceConnection mConnection;
    private final UsbInterface mInterface;
    private final UsbEndpoint mEndpoint;

    private static final int TRANSFER_TIMEOUT = 1000;

    private static int[] PRINTER_VID = {
            34918, 1137, 1659, 1137, 1155, 26728, 17224, 7358
    };

    public static UsbPrinters open(Context c, int vid, int pid) throws IOException {
        UsbUtil u = new UsbUtil(c);
        //for(UsbDevice d : u.findDevicesByVid(PRINTER_VID)) {
        List<UsbDevice> devs = u.findDevicesByVid(new int[]{vid});
        for (UsbDevice d : devs) {
            if (d.getVendorId() == vid) {
                if (pid != 0) { // ignore pid
                    if (d.getProductId() == pid) {
                        return new UsbPrinters(c, d);
                    }
                } else {
                    return new UsbPrinters(c, d);
                }
            }
        }
        return null;
    }

    /*
     *
     */
    public static UsbPrinters open(Context c) throws IOException {
        UsbUtil u = new UsbUtil(c);
        List<UsbDevice> devs = u.findDevicesByVid(PRINTER_VID);
        if (devs.size() > 0) {
            return new UsbPrinters(c, devs.get(0));
        }
        return null;

    }

    public static void requestUsbPrinter(Context c,UZModuleContext moduleContext) {
        UsbUtil u = new UsbUtil(c);
        List<UsbDevice> devs = u.findDevicesByVid(PRINTER_VID);
        if (devs.size() == 0) {
            JSONObject ret = new JSONObject();
            try {
                ret.put("status", false);
                ret.put("err", "设备未连接上");
                moduleContext.success(ret, true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            Toast.makeText(c, "设备未连接上", Toast.LENGTH_SHORT).show();
            return;
        }
        for (UsbDevice d : devs) {
            u.requestPermission(d,moduleContext);
        }
    }

    public UsbPrinters(Context context, UsbDevice device) throws IOException {
        UsbInterface iface = null;
        UsbEndpoint epout = null;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            iface = device.getInterface(i);
            if (iface == null)
                throw new IOException("failed to get interface " + i);

            int epcount = iface.getEndpointCount();
            for (int j = 0; j < epcount; j++) {
                UsbEndpoint ep = iface.getEndpoint(j);
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epout = ep;
                    break;
                }
            }
            if (epout != null)
                break;
        }
        if (epout == null) {
            throw new IOException("no output endpoint.");
        }
        //mDevice = device;
        mInterface = iface;
        mEndpoint = epout;
        UsbManager usbman = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mConnection = usbman.openDevice(device);
        if (mConnection == null) {
            throw new IOException("failed to open usb device.");
        }
        mConnection.claimInterface(mInterface, true);
    }

    public void write(byte[] data) throws IOException {
        if (mConnection.bulkTransfer(mEndpoint, data, data.length,
                TRANSFER_TIMEOUT) != data.length)
            throw new IOException("failed to write usb endpoint.");
    }

    public void close() {
        mConnection.releaseInterface(mInterface);
        mConnection.close();
    }

    public static enum ALIGNMENT {
        LEFT, CENTER, RIGHT,
    }

    public static enum FONT {
        FONT_A, FONT_B,
    }

    public void init() throws IOException {
        byte[] cmd = {0x1B, 0x40};
        write(cmd);
    }

    public void selectAlignment(ALIGNMENT alignment) throws IOException {
        byte iAlignment = 0;

        switch (alignment) {
            case LEFT:
                iAlignment = 0;
                break;
            case CENTER:
                iAlignment = 1;
                break;
            case RIGHT:
                iAlignment = 2;
                break;
            default:
                iAlignment = 0;
        }

        byte[] b = {0x1B, 0x61, iAlignment};
        write(b);
    }

    public void selectFont(FONT font) throws IOException {
        byte[] cmd = {0x1B, 0x4D, 0};
        switch (font) {
            case FONT_A:
                cmd[2] = 0;
                break;
            case FONT_B:
                cmd[2] = 1;
                break;
            default:
        }
        write(cmd);
    }

    public void setFontStyleBold(boolean bold) throws IOException {
        byte[] cmd = {0x1B, 0x45, (byte) (bold ? 1 : 0)};
        write(cmd);
    }

    public void setFontStyleUnderline(boolean underlined) throws IOException {
        byte[] cmd = {0x1B, 0x2D, (byte) (underlined ? 1 : 0)};
        write(cmd);
    }

    public void setFontSize(int width, int height) throws IOException {
        byte options = 0;

        options |= ((width - 1) << 4);
        options |= (height - 1);

        byte[] cmd = {0x1D, 0x21, options};
        write(cmd);
    }

    public void feedLine(int count) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++)
            sb.append('\n');
        write(sb.toString().getBytes());
    }

    public void printString(String string, FONT font, ALIGNMENT alignment, boolean bold, boolean underlined, boolean doubleHeight, boolean doubleWidth) throws IOException {
        selectFont(font);
        setFontStyleBold(bold);
        setFontStyleUnderline(underlined);
        setFontSize(doubleWidth ? 2 : 1, doubleHeight ? 2 : 1);
        selectAlignment(alignment);
        write(string.getBytes("GBK"));
    }

    public void cutPaper() throws IOException {
        byte[] cmd = {0x1D, 0x56, 0};
        write(cmd);
    }

    // for usb printer
    private static final byte[] ZERO = new byte[1024 * 4];    //

    public void checkReady() throws IOException {
        write(ZERO);
    }

    public boolean ready() {
        try {
            checkReady();
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
    }

    /**
     * 打印二维码
     * @throws IOException
     */
    public void QRCode() throws IOException {


    }

}
