package main.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SpertaServer extends Thread{
    
    private ServerSocket serverSocket;
    private boolean running  = true;
	private Socket clientSocket;
	private static Map<String, String> Users;
	private static Map<String, String> Houses;
	private static Map<String, Map<String, String>> permissions;
	private static Map<String, Map<String, Integer>> device;
	private static Map<String, Map<String, Integer>> deviceState;

	private static String USERS_FILE = "extras/Users.txt";
	static String HOUSES_FILE = "extras/Houses.txt";
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
		Users = new HashMap<>();
		Houses = new HashMap<>();
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
						Users.put(user, pass);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Erro ao ler " + USERS_FILE + ": " + e.getMessage());
			System.exit(-1);

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
		initialize_server_vars();

		if (port ==0)
			port = 22345;
		SpertaServer server = new SpertaServer();
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
				clientSocket = serverSocket.accept();
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
		
    	ClientHandler(Socket sock) {
			this.socket = sock;
			this.commandHandler = new ServerCommandHandler(Users, Houses, permissions, device, deviceState);
			System.out.println("Thread do servidor criada para cada cliente.");
    	}

		private int authenticateClient(String user, String pass) {
			int correct = -1;
			if (user == null || pass == null){
				return -1;
			}

			String storedPass = Users.get(user);
			if (storedPass == null){
				Users.put(user, pass);
				appendUserToFile(user, pass);
				return 1;
			}
			if(storedPass.equals(pass)){
					correct = 0;
			}

			return correct;

		}

		private boolean attestClient(ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException {
			String attestationMessage;
			try {
				attestationMessage = (String) inStream.readObject();
			} catch (ClassNotFoundException e) {
				System.err.println("Erro a ler atestação do cliente: " + e.getMessage());
				outStream.writeObject("ATTEST FAILED");
				outStream.flush();
				return false;
			}

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
				outStream.writeObject("ATTEST OK");
				outStream.flush();
				return true;
			}

			outStream.writeObject("ATTEST FAILED");
			outStream.flush();
			return false;
		}

		private String authenticateClient(ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException {
			String user;
			String passwd;

			try {
				user = (String) inStream.readObject();
				passwd = (String) inStream.readObject();
				System.out.println("Utilizador: " + user + "; Password: " + passwd);
			} catch (ClassNotFoundException e1) {
				System.err.println("Erro a ler credenciais do cliente: " + e1.getMessage());
				return null;
			}

			int authenticated = clientInFile(user, passwd);
			if (user != null && !user.isEmpty() && authenticated >= 0) {
				if (authenticated == 1) {
					outStream.writeObject("OK-NEW-USER");
				} else {
					outStream.writeObject("OK-USER");
				}
				outStream.flush();
				return user;
			}

			outStream.writeObject("WRONG-PWD");
			outStream.flush();

			try {
				passwd = (String) inStream.readObject();
				System.out.println("Password recebido: " + passwd);
				authenticated = clientInFile(user, passwd);
				if (authenticated >= 0) {
					outStream.writeObject("OK-USER");
					outStream.flush();
					return user;
				}
			} catch (ClassNotFoundException e1) {
				System.err.println("Erro a ler nova password do cliente: " + e1.getMessage());
			}

			return null;
		}

		@Override
		public void run(){
			try {
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

				if (!attestClient(inStream, outStream)) {
					outStream.close();
					inStream.close();
					socket.close();
					return;
				}

				String user = authenticateClient(inStream, outStream);
				if (user == null || user.isEmpty()) {
					outStream.close();
					inStream.close();
					socket.close();
					return;
				}

				// Command loop to handle main.client commands
				try {
					boolean connected = true;
					while (connected) {
						try {
							// Object command = inStream.readObject();
							String cmdStr =  (String) inStream.readObject();
							System.out.println("Comando de " + user + ": " + cmdStr);
							
							String response = commandHandler.handleCommand(cmdStr, user);
							outStream.writeObject(response);
							outStream.flush();
						} catch (EOFException e) {
							// Client disconnected
							connected = false;
							System.out.println("Cliente " + user + " desconectado");
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }



}