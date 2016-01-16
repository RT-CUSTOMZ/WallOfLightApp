package de.rtcustomz.walloflight.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;

public final class Client {// implements Parcelable {

    //private final static int PACKETSIZE_MAX = 572;
    private static byte[] gamma_correction_table = new byte[256];
    private static String wol_ip;
//    byte[][] sorted_rgb_byte_array = new byte[51][504];
//    DatagramSocket clientSocket;

    private Client() {

    }

    /*public Client(Parcel in) {
        in.readByteArray(this.gamma_correction_table);
        wol_ip = in.readString();
    }

    public static final Parcelable.Creator<Client> CREATOR = new Parcelable.Creator<Client>() {
        @Override
        public Client createFromParcel(Parcel in) {
            return new Client(in);
        }

        @Override
        public Client[] newArray(int size) {
            return new Client[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.gamma_correction_table);
        dest.writeString(wol_ip);
    }*/

    public static void setGamma(double gamma) {
        initialize_gamma_table(gamma);
    }

    public static void setIP(String ip_address) {
        wol_ip = ip_address;
    }

    private static void initialize_gamma_table(double gamma) {
        for (double i = 0; i <= 255; i++) {
            gamma_correction_table[(int) i] = (byte) (Math.pow(i / 255, gamma) * 255 + 0.5);
        }
    }

    public static void sendImage(Bitmap image) throws Exception {
        InetAddress wol_address = InetAddress.getByName(wol_ip);
        byte[][] sorted_rgb_byte_array = new byte[51][504];
        DatagramSocket clientSocket = new DatagramSocket();
        DatagramPacket sendPacket;
        byte[] sendData;

        //Date start = new Date(System.currentTimeMillis());
        int loop;
        for (loop = 0; loop < 1; loop++) {

            int bytes = image.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            image.copyPixelsToBuffer(buffer);
            //byte[][] sorted_rgb_byte_array = new byte[51][504];
            int pixelcolor;
            int pos_x = 0;
            int pos_y = 0;
            int offset_x = 0;

            for (int universe = 0; universe < 51; universe++) {
                for (int channel = 0; channel < 502; channel += 3) { //0-501 (rot,gruen+1,blau+2)(501,502,503)
                    if (pos_y == 0) { //1.zeile
                        if ((offset_x + pos_x) == 48) { //6.strang=neues universum
                            universe++;
                            channel = 192;
                        } else if ((offset_x + pos_x) % 8 == 0) { //neuer strang=kachel12 überspringen (8*8*3)
                            channel += 192;
                            if (channel > 501) {
                                universe++;
                                channel -= 504;
                            }
                        }
                    }
                    pixelcolor = image.getPixel((offset_x + pos_x), pos_y);//x=width=spalte,y=height=reihe
                    sorted_rgb_byte_array[universe][channel] = gamma_correction_table[Color.red(pixelcolor)];
                    sorted_rgb_byte_array[universe][channel + 1] = gamma_correction_table[Color.green(pixelcolor)];
                    sorted_rgb_byte_array[universe][channel + 2] = gamma_correction_table[Color.blue(pixelcolor)];
                    pos_x++;
                    if (pos_x > 7) {
                        pos_x = 0;
                        pos_y++;
                        if (pos_y > 87) {
                            pos_y = 0;
                            offset_x += 8;
                        }
                    }
                    if ((offset_x + pos_x) >= image.getWidth()) {
                        //Log.d("WallOfLightApp", "x is out of image! value is: " + (offset_x + pos_x));
                        break;
                    }
                }
            }

            for (byte uni = 0; uni < 51; uni++) {
                sendData = ArtNetPacket.getArtDMXPacket(sorted_rgb_byte_array[uni], uni);
                sendPacket = new DatagramPacket(sendData, sendData.length, wol_address, 6454);
                clientSocket.send(sendPacket);
            }

            sendPacket = new DatagramPacket(ArtNetPacket.ArtSyncPacket, ArtNetPacket.ArtSyncPacket.length, wol_address, 6454);
            clientSocket.send(sendPacket);
        }
        //Date end = new Date(System.currentTimeMillis());
        //Log.i("WallOfLightApp", "send " + loop + " pictures in " + (end.getTime() - start.getTime()) + " ms to: " + wol_address.getHostAddress());

        clientSocket.close();
    }
}
