import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class SpertaClient {
    


        
    public static void main(String[] args) {
        SpertaClient client = new SpertaClient();
        client.run();
    }

    public void run(){
        try {
            Socket clientSocket = new Socket("localhost", 23456);

            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

            String userInput = "Nexus";

            String userInput2 = "ECLIPSE";
            out.writeObject(userInput);
            out.writeObject(userInput2);

            try {
                Boolean fromServer = (Boolean) in.readObject();
                System.out.println("From server: " + fromServer);
            } catch (Exception e) {
                e.printStackTrace();
            }


            out.close();
            in.close();

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }


    }