package de.rtcustomz.walloflight.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public final class Client {
    private static final byte[] gamma_correction_table = new byte[256];
    private static String wol_ip;

    private Client() {

    }

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

        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);
        int pixelcolor;
        int pos_x = 0;
        int pos_y = 0;
        int offset_x = 0;

        for (int universe = 0; universe < 51; universe++) {
            for (int channel = 0; channel < 502; channel += 3) { //0-501 (red,green+1,blue+2)(501,502,503)
                if (pos_y == 0) { //1.line
                    if ((offset_x + pos_x) == 48) { //6. row = new universe
                        universe++;
                        channel = 192;
                    } else if ((offset_x + pos_x) % 8 == 0) { //new row = skip tile 12 (8*8*3)
                        channel += 192;
                        if (channel > 501) {
                            universe++;
                            channel -= 504;
                        }
                    }
                }
                pixelcolor = image.getPixel((offset_x + pos_x), pos_y);
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

        clientSocket.close();
    }
}
