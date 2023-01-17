package org.cpen533;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.net.UnknownHostException;


public class StudentIDClient {
    private static final int MAX_PAYLOAD_SIZE = 16 * 1024;
    private static final int RETRY_COUNT = 3;
    private static final int DEFAULT_TIMEOUT = 100;


    private static byte[] createRequest(int studentID) {
        // Create a unique request ID
        byte[] requestID = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(requestID);

        try {
            buffer.putInt(InetAddress.getLocalHost().hashCode());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // Add the client port
        buffer.putShort((short) 0);

        buffer.putShort((short) (Math.random() * Short.MAX_VALUE));

        buffer.putLong(System.nanoTime());

        // Add the student ID to the payload
        byte[] payload = new byte[16];
        ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).putInt(studentID);

        // Concatenate the request ID and payload to create the request message
        byte[] request = new byte[requestID.length + payload.length];
        System.arraycopy(requestID, 0, request, 0, requestID.length);
        System.arraycopy(payload, 0, request, requestID.length, payload.length);
        return request;
    }


    private static byte[] sendRequest(DatagramSocket socket, byte[] request, InetAddress serverAddress, int serverPort) throws IOException {
        for (int i = 0; i <= RETRY_COUNT; i++) {
            // Send the request
            socket.send(new DatagramPacket(request, request.length, serverAddress, serverPort));

            try {
                // Receive the reply
                byte[] reply = new byte[MAX_PAYLOAD_SIZE];
                DatagramPacket packet = new DatagramPacket(reply, reply.length);
                socket.receive(packet);

                byte[] payload = new byte[packet.getLength()-16];
                System.arraycopy(reply, 16, payload, 0, payload.length);
                return payload;
            } catch (IOException e) {
                // Timeout occurred, increase the timeout and retry
                socket.setSoTimeout((int) (socket.getSoTimeout() * Math.pow(2, i)));
            }
        }
        throw new IOException("Request failed after " + RETRY_COUNT + " retries");
    }

    public static void main(String[] args) throws IOException {
        int studentID = Integer.parseInt(args[2]);
        int serverPort = Integer.parseInt(args[1]);

        InetAddress serverAddress = InetAddress.getByName(args[0]);

        // Create a new datagram socket
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(DEFAULT_TIMEOUT);

            // Create a request message
            byte[] request = createRequest(studentID);

            // Send the request and wait for a reply
            byte[] reply = sendRequest(socket, request, serverAddress, serverPort);

            // Extract the secret code from the reply message
            int secretCodeLength = ByteBuffer.wrap(reply, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            byte[] secretCode = new byte[secretCodeLength];
            System.arraycopy(reply, 4, secretCode, 0, secretCodeLength);

            System.out.print("Student ID: ");
            System.out.println(studentID);
            System.out.print("Secret Code Length: ");
            System.out.println(secretCodeLength);
            System.out.print("Secret Code: ");
            System.out.println(StringUtils.byteArrayToHexString(secretCode));

        }
    }}
