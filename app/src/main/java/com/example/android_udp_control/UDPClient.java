package com.example.android_udp_control;

import java.io.IOException;
import java.net.*;
import java.nio.*;


public class UDPClient
{
    String msg          = "Hello UDP server";
    String rxCorrectMsg = "OK";
    String hostname;
    int port;
    DatagramSocket socket;
    InetAddress address;

    public UDPClient(String addr, int prt)
    {
        super();
        hostname = addr;
        port     = prt;
    }

    // 1 - correct connection; 0 - wrong server, -1 - exception, -2 - timeout,
    // -3 - failed while waiting for a message, -4 - wrong values
    public int initConnection()
    {
        try {
            address = InetAddress.getByName(hostname);
            socket = new DatagramSocket(8888);
        }
        catch (Exception ex) {
            System.out.println("Caught an exception!");
            ex.printStackTrace();
            socket.close();
            return -1;
        }
        return 1;
    }

    public void closeSocket()
    {
        if (socket != null && !socket.isClosed())
            socket.close();
    }

    public void setSocket()
    {
        try {
            address = InetAddress.getByName(hostname);
            socket = new DatagramSocket(8888);
        }
        catch (Exception ex) {
            System.out.println("Caught an exception while setting the socket!");
            ex.printStackTrace();
        }
    }

    public void sendCommand(String msg)
    {
        try {
            if (msg.equals("none"))  {
                msg = "0,0,0";
            }
            // Expected format: "L,R,A" (e.g. "1200,1100,900")
            String[] parts = msg.split(",");
            if (parts.length != 3) return;

            int L_motor = Integer.parseInt(parts[0].trim());
            int R_motor = Integer.parseInt(parts[1].trim());
            int arm_pos = Integer.parseInt(parts[2].trim());

            ByteBuffer buffer = ByteBuffer.allocate(6);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putShort((short)(L_motor & 0xFFFF));
            buffer.putShort((short)(R_motor & 0xFFFF));
            buffer.putShort((short)(arm_pos & 0xFFFF));
            byte[] data = buffer.array();

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        }
        catch (Exception ex) {
            System.out.println("Caught an exception while sending a request!");
            ex.printStackTrace();
        }
    }

    public String receiveData()
    {
        byte[] buf = new byte[512];
        try {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            InetAddress srvAddress = packet.getAddress();

            if (srvAddress.equals(address)) {
                String received = new String(buf, 0, packet.getLength());
                return received;
            }
        }
        catch (Exception ex) {
            System.out.println("Caught an exception while receiving a message!");
            ex.printStackTrace();
        }
        return "NOK";

    }


}
