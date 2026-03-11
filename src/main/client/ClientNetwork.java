package main.client;

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
