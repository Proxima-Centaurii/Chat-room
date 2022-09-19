import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

import javax.swing.*;

public class ClientCore extends JFrame {

	private static final long serialVersionUID = -4098147767220584966L;
	private static volatile boolean isRunning;
	private static boolean debugMode = true,toggle = true;
	private String name;
	
	private JTextArea screenText;
	private JTextField inputText;
	
	private Socket connection;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private ChatMessage rmsg;
	
	public ClientCore(final String name){
		super("[CLIENT] Chat Room");
		this.name = name;
		isRunning = false;
		new ChatMessage("",this.name,ChatMessage.TEXT);
		screenText = new JTextArea();
		screenText.setEditable(false);
		inputText = new JTextField();
		inputText.setEditable(false);
		inputText.addActionListener(new ActionListener(){
			int mType;
			public void actionPerformed(ActionEvent e){
				mType = determineType(e.getActionCommand());
				sendMessage(new ChatMessage(e.getActionCommand(),name,mType));
				if(mType == ChatMessage.LEAVE) StopAll();
				inputText.setText("");
			}
		});
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				StopAll();
				sendMessage(new ChatMessage("",name,ChatMessage.LEAVE));
				toggle = !toggle;
				if(toggle)
					super.windowClosing(e);
			}
		});
		add(new JScrollPane(screenText));
		add(inputText,BorderLayout.SOUTH);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600,350);
		setVisible(true);
		outputLocalAddress();
	}
	
	public void startRunning(String ip,int port){
		isRunning = true;
		try{
			connectToServer(ip,port);
			setUpStreams();
			chat();	
		}
		catch(EOFException eof){dlog("Server closed the connection");}
		catch(UnknownHostException ue){
			ue.printStackTrace();
			dlog("Connection failed! Unknown host: " + ip);
			JOptionPane.showMessageDialog(null,
					String.format("Could not connect to: %s:%s\nClosing application.", ip,port),
					"Connection failed",
					JOptionPane.ERROR_MESSAGE);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			dlog("Could not connect to specified server!\n");
		}
		finally{
			closeConnection();
			System.exit(0);
		}
	}
	
	private void connectToServer(String ip,int port) throws IOException{
		pushMessage("Connectiong to "+ip+":"+port+" ...");

		connection = new Socket(ip, port);

		//Wait for 10 seconds when attempting to connect to the server for the first time
		connection.setSoTimeout(10000);

		pushMessage("Connection accepted!");
	}
	private void setUpStreams() throws IOException{
		pushMessage("Setting up streams...");
		out = new ObjectOutputStream(connection.getOutputStream());
		out.flush();
		in = new ObjectInputStream(connection.getInputStream());
		out.writeObject(new ChatMessage("",name,ChatMessage.JOIN));
		out.flush();
		pushMessage("You joined the room.");
	}
	private void chat() throws IOException{
		allowTyping(true);

		//After a connection is established, remove the timeout limit (0 = infinite)
		connection.setSoTimeout(0);

		while(isRunning){
			try{
				rmsg = (ChatMessage) in.readObject();
			}catch(ClassNotFoundException cnfe){
				pushMessage("The message received could not be read.");
			}
			//analyze message
			if(rmsg.getType() == ChatMessage.SERVER_CLOSED) isRunning = false;
			//display message
			else pushMessage(rmsg.getFullMessage());
		}
	}
	
	private void allowTyping(final boolean state){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				inputText.setEditable(state);
			}
		});
	}
	
	private int determineType(String s){
		if(s.equals("~!exit")){return ChatMessage.LEAVE;}
		else{return ChatMessage.TEXT;}
	}
	
	private void sendMessage(ChatMessage cmsg){
		try{
			out.writeObject(cmsg);
			out.flush();
			if(cmsg.getType() == ChatMessage.TEXT)
				pushMessage(cmsg.getFullMessage());
		}catch(IOException ioe){}
	}
	
	private void pushMessage(final String s){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				screenText.append(s+"\n");
			}
		});
	}
	
	private void outputLocalAddress(){
		try{
			InetAddress me = InetAddress.getLocalHost();
			String ip_address = me.getHostAddress();
			pushMessage("Your address is: "+ip_address);
		}catch(UnknownHostException uhk){
			pushMessage("Your address could not be determined.");
		}
	}
	
	private void closeConnection(){
		dlog("Clossing the streams and the socket.\n");
		allowTyping(false);
		try{
			if(out != null)
				out.close();
			if(in != null)
				in.close();

			if(connection != null  && !connection.isClosed())
				connection.close();
		}catch(Exception e){
			dlog("Streams not propperly closed!");
		}
	}
	
	private void StopAll(){
		isRunning = false;
		//cmsg.setType(ChatMessage.LOGOUT);
	//	sendMessage(cmsg);
	}
	
	public void dlog(String s){
		if(debugMode){System.out.print(s);}
	}
}//end of class
