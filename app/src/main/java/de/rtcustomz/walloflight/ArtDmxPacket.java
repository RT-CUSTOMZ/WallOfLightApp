package de.rtcustomz.walloflight;

/**
 * Created by Dennis on 19.11.2015.
 */
public class ArtDmxPacket {
    public static byte[] getPacket(byte[] data, byte universe) {
        byte[] header = {0x41,0x72,0x74,0x2d,0x4e,0x65,0x74,0x00,0x00,0x50,0x00,0x0e,(byte)0xc3,0x00,0x00,0x00,0x01,(byte)0xf8};

        header[14]=universe;
        int length = data.length;
        if(length<504)
        {
            header[16]=(byte)(length/0xFF);
            header[17]=(byte)(length%0x100);
        }
        else if(length>504)
        {
            length=504;
        }

        byte[] c = new byte[header.length + length];
        System.arraycopy(header, 0, c, 0, header.length);
        System.arraycopy(data, 0, c, header.length, length);
        return c;
    }
}