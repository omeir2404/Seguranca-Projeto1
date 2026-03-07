import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;

public class SpertaServer extends Thread{
    
    private ServerSocket serverSocket;
    private boolean run = false;
	private Socket clientSocket;

    
    //FALTA
    public static void main(String[] args){
		int port = 22345;
		if (args.length > 0) {
        	port = Integer.parseInt(args[0]);
    	}
		SpertaServer server = new SpertaServer();
    	server.startServer(port);
    }

    //Iniciar Servidor
    public void startServer (int port){
        System.out.println("0 mais rápido clientes...");
        Boolean running = true;

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
        run = false;
        try{
            if(!serverSocket.isClosed() && serverSocket != null){
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
			System.out.println("Thread do server criada para cada cliente.");
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
						System.out.println("User: " + user + "; Password: " + passwd);
					}catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
				 
					//TODO: refazer
					//este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
					//nao faz qualquer tipo de autenticacao
					if (user != null && user.length() != 0){
						outStream.writeObject(Boolean.TRUE);
						outStream.flush();
					}
					else {
						outStream.writeObject(Boolean.FALSE);
						outStream.flush();
					}
	
					// // Command loop to handle client commands
					// ############################################
					// * o Chat diz que ha um problema de conexao(o server quebra a conexao dps de autenticar o user) e isso resolve
					// ############################################
					// try {
					// 	boolean connected = true;
					// 	while (connected) {
					// 		try {
					// 			Object command = inStream.readObject();
					// 			String cmdStr = (String) command;
					// 			System.out.println("Command from " + user + ": " + cmdStr);
								
					// 			// Echo response back to client
					// 			String response = "Server received: " + cmdStr;
					// 			outStream.writeObject(response);
					// 			outStream.flush();
					// 		} catch (EOFException e) {
					// 			// Client disconnected
					// 			connected = false;
					// 			System.out.println("Client " + user + " disconnected");
					// 		}
					// 	}
					// } catch (ClassNotFoundException e) {
					// 	e.printStackTrace();
					// }
	
					outStream.close();
					inStream.close();
					socket.close();
	
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
    }



}