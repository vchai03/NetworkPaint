
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

//Victoria Chai
//sends information about itself to the server 
//and receives information about other clients from server
public class PaintClient extends JFrame implements ActionListener
{

	private Scanner reader;				// used to handle incoming messages
	private PrintWriter writer;			// outgoing messages to server
	private Socket sock;				// creates the connection

	private ArrayList<Point> points;
	private DrawPanel canvas;

	private Person me;
	private int currentSize;

	private DefaultListModel<String> friendModel;		//adds names of ppl in to the room
	private ArrayList<Person> allFriends;			

	private final String IP_ADDRESS = "192.168.1.4";

	public PaintClient(){

		setLayout(null);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(900, 650);

		currentSize = 25;
		points = new ArrayList<Point>();

		allFriends = new ArrayList<Person>();

		canvas = new DrawPanel();
		canvas.setBounds(40,40,575,525);
		add(canvas);

		friendModel = new DefaultListModel<String>();
		JList<String> friendList = new JList<String>(friendModel);		
		
		friendList.setCellRenderer(new RowRenderer());
		
		JScrollPane friendScroll = new JScrollPane(friendList);
		friendScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Friends"));
		friendScroll.setBounds(675,40,150,250);
		add(friendScroll);

		JMenu sizeMenu = new JMenu("Size");
		String[] sizes = {"10","25","50"};
		for(String nextSize: sizes) {
			JMenuItem nextItem = new JMenuItem(nextSize);
			sizeMenu.add(nextItem);
			nextItem.addActionListener(this);
		}

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(sizeMenu);
		setJMenuBar(menuBar);

		JButton logOff = new JButton("Log Off");
		logOff.addActionListener(this);
		logOff.setBounds(775,440,80,18);
		add(logOff);

		setUpNetworking();

		// IncomingReader is an inner class that implements the Runnable interface
		// the run method is responsible for reading data from the server 

		Thread clientThread = new Thread(new IncomingReader());
		clientThread.start();

		setVisible(true);
	}
	
	// sets up the socket to read for the server on port 4242.  
	// sets up the PrintWriter as well.
	private void setUpNetworking() {

		Scanner keyboard = new Scanner(System.in);
		System.out.print("Enter your name: ");

		Random r = new Random();

		me = new Person(keyboard.nextLine(),new Color(r.nextInt(256),r.nextInt(256),r.nextInt(256)));

		this.getContentPane().setBackground(me.color);

		setTitle("Paint Client - " +me.name);

		//sets up connections with server
		try {
			sock = new Socket(IP_ADDRESS, 4242);
			reader = new Scanner(sock.getInputStream());
			writer = new PrintWriter(sock.getOutputStream());
			
			//sends information about me to server
			writer.println(me.toString());
			writer.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	//reacts to a menu item or logoff being entered
	public void actionPerformed(ActionEvent ae) {
		String trigger = ae.getActionCommand();

		if(trigger.equals("10")||trigger.equals("25")||trigger.equals("50")) {
			currentSize = Integer.parseInt(trigger);
		}
		else {

			//log off was pressed
			try{
				writer.println("logoff:"+me.name);
				writer.flush();
				
				sock.close();				

			}catch(IOException e){
				e.printStackTrace();
			}
			System.exit(0);
		}
	}

	// responsible for listening for messages from the server
	// handles a point being received, a new person joining or leaving
	public class IncomingReader implements Runnable {

		// this is all done in a separate thread to allow you to read and draw at the same time
		public void run() {
			
			String message = "";
			
			do {

				if (reader.hasNextLine()) {
					
					message = reader.nextLine();
					String[] info = message.split(" ");
					
					//a point is received; list of points and canvas are updated
					if(message.indexOf("joined:") == -1 && message.indexOf("logoff") == -1) {
						Color ptColor = getColor(info[2], info[3], info[4]);

						synchronized (points) {
							points.add(new Point(Integer.parseInt(info[0]), Integer.parseInt(info[1]), ptColor, Integer.parseInt(info[5])));
						}
						canvas.repaint();
					}
					
					//new client has joined; friends list is updated
					else if (message.indexOf("joined") != -1) {
						String name = info[0].substring(info[0].indexOf(":") + 1);
						friendModel.addElement(name);
						allFriends.add(new Person(name, getColor(info[1], info[2], info[3])));

					} 
					
					//someone else logged off; friends list is updated
					else if (message.indexOf("logoff") != -1 && message.indexOf(me.name) == -1) {
						String name = info[0].substring(info[0].indexOf(":") + 1);
						friendModel.removeElement(name);
						removeFriend(name);
					}
				}

			} while (message.indexOf(me.name) == -1);

			reader.close();
			writer.close();
		}
	}
	
	//returns Color given the red, green, and blue String values
	private Color getColor(String red, String green, String blue) {
		return new Color(Integer.parseInt(red), Integer.parseInt(green), Integer.parseInt(blue));
	}
	
	//removes friend from friends list given name
	private void removeFriend(String name) {
		int index = 0;
		
		while(!allFriends.get(index).name.equals(name))
			index++;
		
		allFriends.remove(index);
	}

	//JPanel that can be drawn on by clicking and dragging the mouse
	public class DrawPanel extends JPanel implements MouseMotionListener{

		public DrawPanel(){

			this.addMouseMotionListener(this);

		}

		public void paintComponent(Graphics g){		

			super.paintComponent(g);
			setBackground(Color.white);

			synchronized(points) {
				//paint points
				for(Point nextP: points){

					g.setColor(nextP.ptColor);
					g.fillOval(nextP.xLoc, nextP.yLoc, nextP.ptSize, nextP.ptSize);
				}
			}
		}

		//called whenever the mouse is dragged
		//adds new Points to the AL, updates the canvas
		//and sends the point to the server
		public void mouseDragged(MouseEvent mouse) {
			
			Point newPoint = new Point(mouse.getX(), mouse.getY(), me.color, currentSize);
			
			synchronized (points) {
				points.add(newPoint); 
				this.repaint();
				writer.println(newPoint.toString());
				writer.flush();	
			}			
					
		}

	}

	public class Point{

		private int xLoc;
		private int yLoc;
		private Color ptColor;
		private int ptSize;

		public Point(int x, int y, Color color, int size){
			xLoc = x;
			yLoc = y;
			ptColor = color;
			ptSize = size;
		}

		public String toString() {
			return xLoc+" "+yLoc+" "+ptColor.getRed() +" "+ptColor.getGreen()+" "+ptColor.getBlue()+ " "+ptSize+" ";
		}
	}

	//this class is used to help color code the friends list
	public class Person{
		private String name;
		private Color color;

		public Person(String n, Color c) {
			name = n;
			color = c;
		}

		//this constructor and equals method is really 
		//only used to help update the arraylist of people
		public Person(String n) {
			name = n;
		}

		public boolean equals(Object o) {
			Person p = (Person)o;
			return p.name.equals(name);
		}

		public String toString() {
			return name +" "+color.getRed()+ " "+color.getGreen()+" "+color.getBlue();
		}
	}

	//this class color codes the JList
	//uses the arraylist of friends
	public class RowRenderer extends DefaultListCellRenderer{

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			setBackground(allFriends.get(index).color); 
			return c;
		}
	}

	public static void main(String[] args) {
		new PaintClient();
	}
}

