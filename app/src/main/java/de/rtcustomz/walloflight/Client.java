package de.rtcustomz.walloflight;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Client implements Runnable {

    private final static int PACKETSIZE_MAX = 572 ;
    private Bitmap image;

    public Client(Bitmap scaledImage) {
        this.image=scaledImage;
    }

    @Override
    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName("192.168.120.73");
            DatagramSocket clientSocket = new DatagramSocket();
            DatagramPacket sendPacket;
            byte[] sendData;

            int bytes = image.getByteCount();
            ByteBuffer buffer = ByteBuffer.allocate(bytes);
            image.copyPixelsToBuffer(buffer);
            byte[] temp;
            byte[][] sorted_rgb_byte_array = new byte[51][504];
            int pixelcolor;
            int pos_x=0;
            int pos_y=0;
            int offset_x=0;

            for(int universe=0;universe<51;universe++) {
                for(int channel=0;channel<502;channel+=3) { //0-501 (rot,gruen+1,blau+2)(501,502,503)
                    if(pos_y==0) { //1.zeile
                        if((offset_x+pos_x)==48) { //6.strang=neues universum
                            universe++;
                            channel=192;
                        }
                        else if((offset_x+pos_x)%8==0) { //neuer strang=kachel12 überspringen (8*8*3)
                            channel+=192;
                            if(channel>501)
                            {
                                universe++;
                                channel-=504;
                            }
                        }
                    }
                    pixelcolor = image.getPixel((offset_x+pos_x), pos_y);//x=width=spalte,y=height=reihe
                    sorted_rgb_byte_array[universe][channel] = (byte) Color.red(pixelcolor);
                    sorted_rgb_byte_array[universe][channel+1] = (byte) Color.green(pixelcolor);
                    sorted_rgb_byte_array[universe][channel+2] = (byte) Color.blue(pixelcolor);
                    pos_x++;
                    if(pos_x>7) {
                        pos_x=0;
                        pos_y++;
                        if(pos_y>87){
                            pos_y=0;
                            offset_x+=8;
                        }
                    }
                    if((offset_x+pos_x)>=image.getWidth())
                    {
                        Log.i("WallOfLightApp", "x is out of image 2! value is: "+(offset_x+pos_x));
                        break;
                    }
                }
            }

            for(byte uni=0;uni<51;uni++) {
                sendData = ArtDmxPacket.getPacket(sorted_rgb_byte_array[uni], uni);
                sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, 6454);
                clientSocket.send(sendPacket);
            }

            clientSocket.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
