import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class SpertaClient {
    
    public static class ServerAddress {
        String host;
        int port;
        
        ServerAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
    

    public static ServerAddress validateServerAddress(String serverAddress) {
        String host;
        int port = 22345; // default port
        
        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid serverAddress format. Use: <IP/hostname>[:Port]");
                System.exit(-1);
            }
            
            try {
                port = Integer.parseInt(parts[1]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Port must be between 1024 and 65535");
                    System.exit(-1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number");
                System.exit(-1);
            }

            host = parts[0];
        } else {
            host = serverAddress;
        }
        
        if (host.isEmpty()) {
            System.err.println("Hostname/IP cannot be empty");
            System.exit(-1);
        }
        
        return new ServerAddress(host, port);
    }

    // SpertaClient <serverAddress> <user-id> <password> 
    // • <serverAddress> identifica o servidor. O formato de serverAddress é o seguinte: 
    // <IP/hostname>[:Port].  O  endereço  IP/hostname  do  servidor  é  obrigatório  e  o 
    // porto é opcional. Por omissão, o cliente deve ligar-se ao porto 22345 do servidor.  
    // • <user-id> - string que identifica o utilizador no servidor SpertaServer.  
    // • <password> - string correspondente à password do utilizador
    public static void main(String[] args) {
        if (args.length != 3){
            System.err.println("Usage: SpertaClient <serverAddress> <user-id> <password>");
            System.exit(-1);
        }
        
        ServerAddress server = validateServerAddress(args[0]);
        String userId = args[1];
        String password = args[2];
        
        if (userId.isEmpty() || password.isEmpty()) {
            System.err.println("User-id and password cannot be empty");
            System.exit(-1);
        }

        SpertaClient client = new SpertaClient();
        client.run(server.host, server.port, userId, password);
    }

    public void run(String host, int port, String userId, String password){
        try {
            Socket clientSocket = new Socket(host, port);

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            Scanner sc = new Scanner(System.in);
            
            // Send authentication credentials
            out.writeObject(userId);
            out.writeObject(password);
            out.flush();

            try {
                String authResponse = (String) in.readObject();
                System.out.println("Server: " + authResponse);
                
                if(authResponse.equals("WRONG-PWD")){
                    out.flush();
                    System.out.print("Escreva aqui a password correta: ");
                    password = sc.nextLine();
                    out.writeObject(password);
                    authResponse = (String) in.readObject();
                    out.flush();
                }
                // Handle authentication response: OK-USER, OK-NEW-USER, WRONG-PWD
                // Temporarily also accept "Cliente connectado com sucesso!" until server implements proper protocol
                if (authResponse.equals("OK-USER") || authResponse.equals("OK-NEW-USER") || 
                    authResponse.equals("Cliente connectado com sucesso!")) {
                    // Command prompt loop
                    boolean running = true;
                    System.out.println("Connected to server!");
                    System.out.println("• CREATE <hm> # Criar casa <hm> - utilizador é Owner \n" +
                                            "• ADD <user1> <hm> <s> # Adicionar utilizador <user1> à casa <hm>, seção <s> \n" +
                                            "• RD <hm> <s> # Registar um Dispositivo na casa <hm>, na seção <s> \n" +
                                            "• EC <hm> <d> <int> # Enviar valor <int> de estado/temporização, do dispositivo <d> da casa <hm>, para o servidor. \n" +
                                            "• RT <hm># Receber a informação sobre o último comando (estados/temporizações) enviado a cada dispositivo da casa <hm>, desde que o utilizador tenha permissões. \n" +
                                            "• RH <hm> <d># Receber o Histórico (ficheiro de log .csv) de comandos enviados ao dispositivo <d> da casa <hm>, desde que o utilizador tenha permissões. ");
                    while (running) {
                        System.out.print("Insere aqui o comando: ");
                        System.out.flush();
                        String command = sc.nextLine();
                        
                        if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                            running = false;
                            System.out.println("A desconectar...");
                        } else if (!command.trim().isEmpty()) {
                            out.writeObject(command);
                            out.flush();
                            
                            try {
                                Object response = in.readObject();
                                System.out.println(response);
                            } catch (Exception e) {
                                System.out.println("Erro a ler resposta: " + e.getMessage());
                            }
                        }
                    }
                } 
                else if (authResponse.equals("WRONG-PWD")) {
                    System.out.println("Password Errada! CHÃOO");
                }
                else {
                   System.out.println("A Autenticação falhou: " + authResponse);
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