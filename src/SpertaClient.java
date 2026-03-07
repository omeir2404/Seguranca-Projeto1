import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
public class SpertaClient {
    
    
    public static void main(String[] args) {
        SpertaClient client = new SpertaClient();
        client.run();
    }

    public void run(){
        try {
            Socket clientSocket = new Socket("localhost", 22345);
            System.out.println("Connected to server!");

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            Scanner sc = new Scanner(System.in);
            System.out.print("Enter your Name:");
            String userInput = sc.nextLine();
            System.out.print("Enter your Password:");
            String userInput2 = sc.nextLine();

            out.writeObject(userInput);
            out.writeObject(userInput2);
            out.flush();

            try {
                Boolean fromServer = (Boolean) in.readObject();
                System.out.println("Authentication: " + fromServer);
                
                if (fromServer) {
                    // Command prompt loop
                    boolean running = true;
                    while (running) {

                        System.out.print("Insira comando aqui> ");
                        System.out.flush();
                        String command = sc.nextLine();
                        
                        if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                            running = false;
                            System.out.println("Disconnecting...");
                        } else if (!command.trim().isEmpty()) {
                            out.writeObject(command);
                            out.flush();
                            
                            try {
                                Object response = in.readObject();
                                System.out.println("Server: " + response);
                            } catch (Exception e) {
                                System.out.println("Error reading response: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println("Authentication failed!");
                }
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