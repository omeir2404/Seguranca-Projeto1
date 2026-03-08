import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
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

		private boolean authenticateClient(String user, String pass) {
			Boolean correct = false;
			if (user == null || pass == null){
				return false;
			} 
			String storedPass = Users.get(user);
			if(storedPass != null && storedPass.equals(pass)){
					correct = true;
			}
			// else{//*ele aqui pede ao server para a pass de novo, devia pedir ao cliente
			// 	Scanner sc = new Scanner(System.in);
			// 	System.out.print("Password incorreta, experimenta de novo:");
			// 	pass = sc.nextLine();
			// 	if(storedPass != null && storedPass.equals(pass)){
			// 		correct = true;
			// 	}
			// }
			return correct;

		}

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
				 
					//TODO: refazer
					//este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
					//nao faz qualquer tipo de autenticacao
					if (user != null && !user.isEmpty() && authenticateClient(user, passwd)) {
						outStream.writeObject("Cliente connectado com sucesso!");
						outStream.flush();
					}
					else {
						outStream.writeObject("Erro ao autenticar com servidor");
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
								
								// Echo response back to client
								String response = "Servidor recebido: " + cmdStr;
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