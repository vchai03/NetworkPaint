import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;

//Victoria Chai
//accepts all connections from clients
//reads in messages from clients and broadcasts those messages to all other clients
public class PaintServer
{
	private ArrayList<ClientHandler> allClients = new ArrayList<ClientHandler>();  // used to broadcast messages to all connected clients

	// creates the serverSocket on port 4242.
	// continously attempts to listen for new clients
	// builds a clientHandler thread off the socket and starts that thread.
	// this constructor never ends.
	
	public  PaintServer() {
		
		//server socket accepts connections forever
		try {
			ServerSocket server = new ServerSocket(4242);
			System.out.println(server.getLocalPort());
			System.out.println(InetAddress.getLocalHost().getHostAddress());
			
			while(true) {
				
				Socket sock = server.accept();
				ClientHandler handler = new ClientHandler(sock);
				
				//spawns new thread to interact with client
				Thread thread = new Thread(handler);
				thread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// writes the message to every socket in the ArrayList instance variable.
	private void tellEveryone(String message) {
		
		synchronized (allClients) {
			for (ClientHandler client : allClients) {
				client.theWriter.println(message);
				client.theWriter.flush();
			}
		}
		
	}

	//interacts with a specific client
	public class ClientHandler implements Runnable {

		private Scanner reader;
		private Socket sock;
		private PrintWriter theWriter;
		private String name;				//client's name
		private Color color;				//client color draws with

		// initializes all instance variables
		public ClientHandler(Socket clientSocket) {

			try {
				sock = clientSocket;
				reader = new Scanner(sock.getInputStream());
				theWriter = new PrintWriter(sock.getOutputStream());
				
				//extracts name and color from message
				String message = reader.nextLine();
				String[] info = message.split(" ");
				name = info[0];
				color = new Color(Integer.parseInt(info[1]), Integer.parseInt(info[2]), Integer.parseInt(info[3]));
				
				//sends information about currently connected clients
				//and notifies all other clients of information about new client
				synchronized (allClients) {
					for (ClientHandler client : allClients) {
						theWriter.println("joined:" + client);
						theWriter.flush();
					}
					
					tellEveryone("joined:" + message);
					allClients.add(this);
				}			
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
		public String toString() {
			return name +" "+ color.getRed() + " "+color.getGreen()+ " "+color.getBlue();
		}

		public boolean equals(Object o) {
			ClientHandler other = (ClientHandler)o;
			return name.equals(other.name);
		}
		
		
		//continuously checks to see if there is an available message from the client
		// if so broadcasts received message to all other clients
		// via the outer helper method tellEveryone.
		public void run() {
			
			String message = "";
			
			//keeps sending message to all other clients while this client has not logged off
			do {
				
				if(reader.hasNextLine()) {
					message = reader.nextLine();
					tellEveryone(message);
				}
				
			}while(!message.equals("logoff:"+name));

			closeConnections();
		}

		//closes all connections and updates list of connected clients
		private void closeConnections(){

			try{
				synchronized(allClients){

					reader.close();
					theWriter.close();
					sock.close();
					allClients.remove(this);
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new PaintServer();
	}
}
