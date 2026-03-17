package main.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerNetwork {

    ObjectOutputStream out;
    ObjectInputStream in;


    public ServerNetwork(Socket socket) throws IOException {
        // ObjectOutputStream MUST come first to avoid deadlock
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in  = new ObjectInputStream(socket.getInputStream());
    }



    // send
    public void send(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // send a file
    public void sendFile(Path filePath) {
        try (InputStream fis = Files.newInputStream(filePath)) {
            // send file size
            out.writeLong(Files.size(filePath));
            out.flush();

            // send file bytes
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
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


    // close
    public void close() {
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
