package main.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public void receiveFile(Path targetDirectory) {
        try {
            String fileName = (String) in.readObject();
            long fileSize = in.readLong();

            Path outputPath = targetDirectory.resolve(fileName);

            byte[] buffer = new byte[8192];
            long remaining = fileSize;

            try (var fos = Files.newOutputStream(outputPath)) {
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = in.read(buffer, 0, toRead);

                    if (bytesRead == -1) {
                        throw new IOException("Connection closed before file transfer completed");
                    }

                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
