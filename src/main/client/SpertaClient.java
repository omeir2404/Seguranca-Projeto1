package main.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class SpertaClient {
    private static final String HELP_TEXT = "Comandos disponiveis:\n • CREATE <hm> # Criar casa <hm> - utilizador é Owner \n • ADD <user1> <hm> <s> # Adicionar utilizador <user1> à casa <hm>, seção <s> \n • RD <hm> <s> # Registar um Dispositivo na casa <hm>, na seção <s> \n • EC <hm> <d> <int> # Enviar valor <int> de estado/temporização, do dispositivo <d> da casa <hm>, para o servidor. \n • RT <hm># Receber a informação sobre o último comando (estados/temporizações) enviado a cada dispositivo da casa <hm>, desde que o utilizador tenha permissões. \n • RH <hm> <d># Receber o Histórico (ficheiro de log .csv) de comandos enviados ao dispositivo <d> da casa <hm>, desde que o utilizador tenha permissões.\n • EXIT ou QUIT # Terminar a ligação ao servidor e fechar o cliente\n • LS ou LS <hm> para listar todas as casas que o utilizador tem acesso ou para listar todos os dispositivos e seus temporizadores de uma casa específica <hm>";
    private static ClientNetwork network;


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
        network = new ClientNetwork();
        network.initClientNetwork(server.host, server.port);
        client.run(userId, password);
    }

    private boolean isAuthenticatedResponse(String authResponse) {
        return authResponse.equals("OK-USER")
                || authResponse.equals("OK-NEW-USER")
                || authResponse.equals("Cliente connectado com sucesso!");
    }

    private boolean authenticate(Scanner sc, String userId, String password){
        network.send(userId);
        network.send(password);

        String authResponse = (String) network.receive();
        System.out.println("Server: " + authResponse);

        if (authResponse.equals("WRONG-PWD")) {
            System.out.print("Escreva aqui a password correta: ");
            password = sc.nextLine();
            network.send(password);

            authResponse = (String) network.receive();
            System.out.println("Server: " + authResponse);
        }

        if (isAuthenticatedResponse(authResponse)) {
            System.out.println("Connected to main.server!");
            System.out.println(HELP_TEXT);
            return true;
        }

        if (authResponse.equals("WRONG-PWD")) {
            System.out.println("Password Errada! CHÃOO");
        } else {
            System.out.println("A Autenticação falhou: " + authResponse);
        }

        return false;
    }

    private void commandLoop(Scanner sc){
        boolean running = true;

        while (running) {
            System.out.print("Insere aqui o comando: ");
            System.out.flush();
            String command = sc.nextLine();

            if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                running = false;
                System.out.println("A desconectar...");
            } else if (!command.trim().isEmpty()) {
                network.send(command);
                String[] parts = command.trim().split("\\s+");
                String commandName = parts[0].toUpperCase();

                Object responseObj = network.receive();
                if (!(responseObj instanceof String response)) {
                    System.out.printf("Resposta inesperada do servidor: %s%n", responseObj);
                    continue;
                }

                String status = response;

                if ("OK".equals(status) && "RT".equals(commandName) && parts.length == 2) {
                    String house = parts[1];
                    Path out = Paths.get("logs", "RT_" + house + ".txt");
                    long sizeBytes = network.receiveFile(out);
                    if (sizeBytes >= 0) {
                        System.out.println("OK");
                        System.out.println("Tamanho recebido: " + sizeBytes + " bytes");
                        System.out.println("Ficheiro RT recebido: " + out);
                    } else {
                        System.out.println("Erro ao receber ficheiro RT.");
                    }
                } else if ("OK".equals(status) && "RH".equals(commandName) && parts.length == 3) {
                    String house = parts[1];
                    String device = parts[2];
                    Path out = Paths.get("logs", "RH_" + house + "_" + device + ".csv");
                    long sizeBytes = network.receiveFile(out);
                    if (sizeBytes >= 0) {
                        System.out.println("OK");
                        System.out.println("Tamanho recebido: " + sizeBytes + " bytes");
                        System.out.println("Ficheiro RH recebido: " + out);
                    } else {
                        System.out.println("Erro ao receber ficheiro RH.");
                    }
                } else {
                    System.out.println(status);
                }
            }
        }
    }

    private boolean attestation() throws IOException{
        long size;
        try (InputStream is = SpertaClient.class.getResourceAsStream("SpertaClient.class")) {
            if (is == null) throw new IOException("Could not load SpertaClient.class resource for attestation");
            size = is.readAllBytes().length;
        }
        String attestMsg = "ATTEST SpertaClient:" + size;
        network.send(attestMsg);

        String response = (String) network.receive();
        if (response.equals("ATTEST OK")) {
            System.out.println("ATTESTATION OK");
            return true;
        } else {
            System.out.println("ATTESTATION FAILED");
            return false;
        }
    }

    public void run(String userId, String password) {
        Scanner sc = new Scanner(System.in);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCTRL+C detetado. A fechar o cliente...");
            network.closeClientNetwork();
        }));
        try {
            if(!attestation()){
                System.exit(-1);
            }
            if (authenticate(sc, userId, password)) {
                commandLoop(sc);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}