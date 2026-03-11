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
import java.util.HashMap;
import java.util.Map;

public class SpertaServer extends Thread{
    
    private ServerSocket serverSocket;
    private boolean running  = true;
	private Socket clientSocket;
	private static Map<String, String> Users;
	private static Map<String, String> Houses;
	private static Map<String, Map<String, String>> Permissions;

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
		// Mu

		try (BufferedReader reader = new BufferedReader(new FileReader("extras/Users.txt"))) {
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
			System.err.println("Erro ao ler extras/Users.txt: " + e.getMessage());
			System.exit(-1);

		}
	}

	private static synchronized void appendUserToFile(String user, String pass) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("extras/Users.txt", true))) {
			writer.newLine();
			writer.write(user + ":" + pass);
		} catch (IOException e) {
			System.err.println("Erro ao escrever em extras/Users.txt: " + e.getMessage());
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
    	ClientHandler(Socket sock) {
			this.socket = sock;
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

		//Método CREATE
		private String createHouse(String user, String house) {
			if(Houses.containsKey(house)){
				return "NOK";
			}
			Houses.put(house, user);
			return "OK";
		}

		//VER
		//Método ADD
		private String addHouse(String user, String userAdd, String house, String section) {
			if(!Houses.containsKey(house)){
					return "NOHM";
				}
				if(!Users.containsKey(userAdd)){
					return "NOUSER";
				}
				if(!Houses.get(house).equals(user)){
					return "NOPERM";
				}
				
				Map<String, String> permissions = Permissions.get(house);
    			permissions.put(userAdd, section);
				return "OK";
		}	

		//VER
		private String handleCommand(String cmd, String user) {
			if (cmd == null || cmd.trim().isEmpty()) {
				return "ERRO: Não colocaste o comando!";
			}
			cmd = cmd.trim();
			String[] parts = cmd.split(" ");
			String comando = parts[0].toUpperCase();
		
			//Comando CREATE
			if (comando.equals("CREATE")) {
				if (parts.length != 2) {
					return "ERRO: O correto é: CREATE <hm>";
				}
				String houseName = parts[1];
				return createHouse(user, houseName);
			//Comando ADD
			}else if(comando.equals("ADD")){
				if (parts.length != 5) {
					return "ERRO: O correto é:  ADD <user1> <hm> <s>";
				}
				String userAdd = parts[1];
    			String house = parts[2];
    			String section = parts[3];
				return addHouse(user, userAdd, house, section);
			}
				return "ERRO: Comando desconhecido: " + comando;
		}


		@Override
		public void run(){
			try {
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());

				String user = null;
				String passwd = null;
			
				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("Utilizador: " + user + "; Password: " + passwd);
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			 
				int authenticated = authenticateClient(user, passwd);
				if (user != null && !user.isEmpty() && authenticated >= 0) {
					if(authenticated == 1){
						outStream.writeObject("OK-NEW-USER");
					}
					else
						outStream.writeObject("OK-USER");
					outStream.flush();

				}
				else {
					outStream.writeObject("WRONG-PWD");

					try {
						passwd = (String)inStream.readObject();
						System.out.println("Password recebido: " + passwd);
						authenticated = authenticateClient(user, passwd);
						if (authenticated >= 0){
							outStream.writeObject("OK-USER");
							outStream.flush();
						}
						
					}catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
					outStream.flush();
				}

				// Command loop to handle client commands
				try {
					boolean connected = true;
					while (connected) {
						try {
							// Object command = inStream.readObject();
							String cmdStr =  (String) inStream.readObject();
							System.out.println("Comando de " + user + ": " + cmdStr);
							
							String response = handleCommand(cmdStr, user);
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