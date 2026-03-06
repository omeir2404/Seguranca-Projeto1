import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SpertaServer {
    
    private ServerSocket serverSocket;
    private boolean run = false;
	private Socket clientSocket;

    
    //FALTA
    public static void main(String[] args){

    }

    //Iniciar Servidor
    public void startServer (){
        System.out.println("0 mais rápido clientes...");
        Boolean running = true;

        try {
            serverSocket = new ServerSocket(23456); // porta do servidor
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        while(running){
            try{
                System.out.println("Nova conexão de: " + clientSocket.getInetAddress().getHostAddress());

                // ClientHandler clientHandler = new ClientHandler(clientSocket);
				clientSocket = serverSocket.accept();
				// ServerThread newServerThread = new ServerThread(clientSocket);
				// newServerThread.start();
            }catch(IOException e){
                if(run){
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

    //VERRRRRR
	public void run(){
			try {
				// DataOutputStream out = new
				// DataOutputStream(clientSocket.getOutputStream());
				// // ObjectOutputStream outStream = new ObjectOutputStream(serverSocket.getOutputStream());
				// ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
				ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
				ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());


				String user = null;
				String passwd = null;
			
				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("Thread: depois de receber a password e o user");
				}catch (ClassNotFoundException e1) {
					// System.err.println(e1.printStackTrace());
					e1.printStackTrace();
				}
 			
				//TODO: refazer
				//este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
				//nao faz qualquer tipo de autenticacao
				if (user.length() != 0){
					outStream.writeObject(Boolean.TRUE);
				}
				else {
					outStream.writeObject(Boolean.FALSE);
				}

				outStream.close();
				inStream.close();
 			
				serverSocket.close();

			} catch (IOException e) {
				// System.err.println(e.printStackTrace());
				e.printStackTrace();
			}
    }




    /*
    public class ClientHandler extends Thread {
    private Socket socket;
    
    ClientHandler(Socket inSoc) {
        socket = inSoc;
        System.out.println("thread do server para cada cliente");
    }
    
    public void run() {
        // Move o código que está no método run() atual para aqui
    }
}
    
    */


}


//Servidor myServer
/*
public class myServer{

	public static void main(String[] args) {
		System.out.println("servidor: main");
		myServer server = new myServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(23456);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String user = null;
				String passwd = null;
			
				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("thread: depois de receber a password e o user");
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
 			
				//TODO: refazer
				//este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
				//nao faz qualquer tipo de autenticacao
				if (user.length() != 0){
					outStream.writeObject(Boolean.TRUE);
				}
				else {
					outStream.writeObject(Boolean.FALSE);
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
				
*/