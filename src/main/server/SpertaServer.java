package main.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SpertaServer extends Thread{
    
    private ServerSocket serverSocket;
    private boolean running  = true;
	private static Map<String, String> users;
	private static Map<String, String> houses;
	private static Map<String, Map<String, String>> permissions;
	private static Map<String, Map<String, Integer>> device;
	private static Map<String, Map<String, Integer>> deviceState;

	private static String USERS_FILE = "extras/Users.txt";
	private static String HOUSES_FILE = "extras/Houses.txt";
	private static String PERMISSIONS_FILE = "extras/Permissions.txt";
	private static String LOGS_DIR = "logs/";
	private static String ATTESTATION_FILE = "extras/attestation.txt";



	private static void initializeFilePaths() {
		Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		Path projectRoot = current;

		while (current != null) {
			Path usersFile = current.resolve("extras").resolve("Users.txt");
			if (usersFile.toFile().exists()) {
				projectRoot = current;
				break;
			}
			current = current.getParent();
		}

		USERS_FILE = projectRoot.resolve("extras").resolve("Users.txt").toString();
		HOUSES_FILE = projectRoot.resolve("extras").resolve("Houses.txt").toString();
		PERMISSIONS_FILE = projectRoot.resolve("extras").resolve("Permissions.txt").toString();
		LOGS_DIR = projectRoot.resolve("logs").toString();
		ATTESTATION_FILE = projectRoot.resolve("extras").resolve("attestation.txt").toString();
	}

    private static int validateInput(String[] args){
		int port = 0;
		if (args.length > 1 || args.length < 0){
			System.out.println("Input Inválido: SpertaServer [port]");
    		System.exit(-1);
		}
		if (args.length == 0)
			return 0;
		if(args.length == 1){
			try {
				port = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("A porta tem de ser de tipo Integer");
    			System.exit(-1);
			}
			if (port < 1024 || port > 65523){
			    System.err.println("A porta tem de estar entre 1024 e 65535");
    			System.exit(-1);
			}
			return port;
		}
		return 0;
	}

	public static void initialize_server_vars(){
		users = new HashMap<>();
		houses = new HashMap<>();
		permissions = new HashMap<>();
		device = new HashMap<>();
		deviceState = new HashMap<>();


		try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
			String line;
			boolean firstLine = true;

			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					continue; // Skip header line (e.g., User:pass)
				}

				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				String[] parts = line.split(":", 2);
				if (parts.length == 2) {
					String user = parts[0].trim();
					String pass = parts[1].trim();
					if (!user.isEmpty()) {
						users.put(user, pass);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Erro ao ler " + USERS_FILE + ": " + e.getMessage());
			System.exit(-1);

		}

		loadHousesFromFile();
		loadPermissionsFromFile();
	}

	private static void loadHousesFromFile() {
		try (BufferedReader reader = new BufferedReader(new FileReader(HOUSES_FILE))) {
			String line;
			boolean firstLine = true;

			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					continue; // Skip header line (e.g., House:Owner)
				}

				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				String[] parts = line.split(":", 2);
				if (parts.length == 2) {
					String house = parts[0].trim();
					String owner = parts[1].trim();
					if (!house.isEmpty() && !owner.isEmpty()) {
						houses.put(house, owner);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Aviso ao ler " + HOUSES_FILE + ": " + e.getMessage());
		}
	}

	private static void loadPermissionsFromFile() {
		try (BufferedReader reader = new BufferedReader(new FileReader(PERMISSIONS_FILE))) {
			String line;
			boolean firstLine = true;

			while ((line = reader.readLine()) != null) {
				if (firstLine) {
					firstLine = false;
					continue; // Skip header line (e.g., House:User:Section)
				}

				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				String[] parts = line.split(":", 3);
				if (parts.length == 3) {
					String house = parts[0].trim();
					String user = parts[1].trim();
					String section = parts[2].trim();
					if (!house.isEmpty() && !user.isEmpty() && !section.isEmpty()) {
						permissions.computeIfAbsent(house, k -> new HashMap<>()).put(user, section);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Aviso ao ler " + PERMISSIONS_FILE + ": " + e.getMessage());
		}
	}

	private static synchronized void appendUserToFile(String user, String pass) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
			writer.newLine();
			writer.write(user + ":" + pass);
		} catch (IOException e) {
			System.err.println("Erro ao escrever em " + USERS_FILE + ": " + e.getMessage());
		}
	}

    //FALTA
    public static void main(String[] args){
		int port = validateInput(args);
		initializeFilePaths();
		initialize_server_vars();

		if (port ==0)
			port = 22345;
		SpertaServer server = new SpertaServer();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nCTRL+C detetado. A fechar o servidor...");
			server.closeServer();
		}));
    	server.startServer(port);
    }

    //Iniciar Servidor
    public void startServer (int port){
        System.out.println("0 mais rápido clientes...");

        try {
            serverSocket = new ServerSocket(port); // porta do servidor
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while(running){
            try{
				Socket clientSocket = serverSocket.accept();
                System.out.println("Nova conexão de: " + clientSocket.getInetAddress().getHostAddress());
				ClientHandler clientHandler = new ClientHandler(clientSocket);
				clientHandler.start();
            }catch(IOException e){
                if(running){
                    System.err.println("Erro na conexão: " + e.getMessage());
                }
            }
                
        }
  
    }

    //Fechar Servidor
    public void closeServer (){
        running = false;
        try{
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
                System.out.println("O Servidor fechou!");
            }
        }catch(IOException e){
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }


	class ClientHandler extends Thread{
    	private Socket socket;
		private ServerCommandHandler commandHandler;
		private ServerNetwork network;
		
    	ClientHandler(Socket sock) {
			this.socket = sock;
			try{
				this.network = new ServerNetwork(sock);
				this.commandHandler = new ServerCommandHandler(users, houses, permissions, device, deviceState, network);
				System.out.println("Thread do servidor criada para cliente.");
			} catch (Exception e){
				System.out.println("Erro ao criar thread do servidor");
				e.printStackTrace();
			}
    	}

		private int clientInFile(String user, String pass) {
			int correct = -1;
			if (user == null || pass == null){
				return -1;
			}

			String storedPass = users.get(user);
			if (storedPass == null){
				users.put(user, pass);
				appendUserToFile(user, pass);
				return 1;
			}
			if(storedPass.equals(pass)){
					correct = 0;
			}

			return correct;

		}

		private boolean attestClient() throws IOException {
			String attestationMessage;
            attestationMessage = (String) network.receive();

            String expectedAttestation = null;
			try (BufferedReader reader = new BufferedReader(new FileReader(ATTESTATION_FILE))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (!line.isEmpty()) {
						expectedAttestation = line;
						break;
					}
				}
			}

			boolean valid = false;
			if (expectedAttestation != null && attestationMessage != null && attestationMessage.startsWith("ATTEST ")) {
				String received = attestationMessage.substring("ATTEST ".length()).trim();
				valid = expectedAttestation.equals(received);
			}

			if (valid) {
				network.send("ATTEST OK");
				return true;
			}

			network.send("ATTEST FAILED");
			return false;
		}

		private String authenticateClient() throws IOException {
			String user;
			String passwd;

            user = (String) network.receive();
            passwd = (String) network.receive();
            System.out.println("Utilizador: " + user + "; Password: " + passwd);

            int authenticated = clientInFile(user, passwd);
			if (user != null && !user.isEmpty() && authenticated >= 0) {
				if (authenticated == 1) {
					network.send("OK-NEW-USER");
				} else {
					network.send("OK-USER");
				}
				return user;
			}

			network.send("WRONG-PWD");

            passwd = (String) network.receive();
            System.out.println("Password recebido: " + passwd);
            authenticated = clientInFile(user, passwd);
            if (authenticated >= 0) {
                network.send("OK-USER");
                return user;
            }

            return null;
		}

		@Override
		public void run(){
			try {

				if (!attestClient()) {
					network.close();
					socket.close();
					return;
				}

				String user = authenticateClient();
				if (user == null || user.isEmpty()) {
					network.close();
					socket.close();
					return;
				}

				// Command loop to handle main.client commands
                boolean connected = true;
                while (connected) {
					String cmdStr =  (String) network.receive();
					if (cmdStr == null){
						break;
					}
					System.out.println("Comando de " + user + ": " + cmdStr);

					String response = commandHandler.handleCommand(cmdStr, user);
					if (cmdStr.contains("RH")) {
						if (response.equals("OK")) {
							System.out.println("RH RETURNED OK");
							String[] parts = cmdStr.trim().split("\\s+");
							String device = parts[2];
							commandHandler.RH_sendFile(cmdStr, device);
						} else {
							network.send(response);
						}
					}
					if (cmdStr.contains("RT")) {
						if (response.equals("OK")) {
							commandHandler.RT_sendFile(cmdStr, user);
						} else {
							network.send(response);
						}
					}

					network.send(response);
				}

                network.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }



}