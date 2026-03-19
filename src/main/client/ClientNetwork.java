package main.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientNetwork {

    Socket clientSocket;

    ObjectOutputStream out;
    ObjectInputStream in;

    // send
    public void send(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // receive
    public Object receive() {
        try {
            return in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Receives a file sent by the server. The server first sends a long indicating the file size, followed by the file bytes.
     * @returns String of the file contents or null if a file was not received properly
     */
    public String receiveFile() {
        StringBuilder sb = new StringBuilder();
        try {
            // String fileName = (String) in.readObject(); // server never sends filename
            long fileSize = in.readLong();

            System.out.printf("Recebendo arquivo de tamanho %d bytes...%n", fileSize);
            byte[] buffer = new byte[8192];
            long remaining = fileSize;

            try {
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = in.read(buffer, 0, toRead);

                    if (bytesRead == -1) {
                        throw new IOException("Connection closed before file transfer completed");
                    }

                    sb.append(new String(buffer, 0, bytesRead));
                    remaining -= bytesRead;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    


    // init socket and streams
    public boolean initClientNetwork(String host, int port) {
        try{
            clientSocket = new Socket(host, port);

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            return true;

        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    // close resources
    public boolean closeClientNetwork(){
        try{
            out.close();
            in.close();
            clientSocket.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


}
