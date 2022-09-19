import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ServerCore extends JFrame {

	private static final long serialVersionUID = -6220335488480708165L;
	private static volatile boolean isRunning;
	private static boolean debugMode = true;
	private static int uniqueNumber = 1,port;

	private ServerSocket server;
	private ArrayList<ClientThread> clients;
	
	private String name;
	private int serverid = 0;
	private JTextField inputText;
	private JTextArea screenText;
	
	
	public ServerCore(final String name){
		super("[SERVER] Chat Room");
		this.name = name;
		isRunning = false;
		clients = new ArrayList<ClientThread>();
		screenText = new JTextArea();
		screenText.setEditable(false);
		inputText = new JTextField();
		inputText.setEditable(false);
		inputText.addActionListener(new ActionListener(){
			int mType;
			public void actionPerformed(ActionEvent e){
				mType = determineType(e.getActionCommand());
				if(mType == ChatMessage.SERVER_CLOSED) stop();
				else publishMessage(new ChatMessage(e.getActionCommand(),name,ChatMessage.TEXT),serverid);
				inputText.setText("");
			}
		});
		add(new JScrollPane(screenText));
		add(inputText,BorderLayout.SOUTH);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent ev){
				try{
					stop();
				}catch(Exception err){
					err.printStackTrace();
				}
				dispose();
				System.exit(0);
			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600,350);
		setVisible(true);
		outputLocalAddress();
	}
	
	private int determineType(String s){
		if(s.equals("~!exit")) return ChatMessage.SERVER_CLOSED;
		else return ChatMessage.TEXT;
	}
	
	public void startRunning(int port,int maxConnections){
		ServerCore.port = port;
		Socket t = null;
		ClientThread  ct = null;
		isRunning = true;
		try{
			server = new ServerSocket(port, maxConnections);
			pushMessage("Waiting for someone to connect...");
			while(isRunning){
				if(clients.size() > 0) allowTyping(true);
					t = server.accept();
					if(!isRunning) break;
					ct = new ClientThread(t);
					clients.add(ct);
					ct.start();		
			}//end of main loop
			try{
				server.close();
				t.close();
				for(int i=clients.size()-1;i>=0;i--){
					ct = clients.get(i);
					try{
						ct.out.close();
						ct.in.close();
						ct.socket.close();
					}catch(IOException ex){}
				}
			}catch(Exception ex){}		
		}catch(IOException ex){}
		while(true){
			if(clients.size() == 0) break;
		}
		System.exit(0);
	}
		
	protected void stop(){
		isRunning = false;
		publishMessage(new ChatMessage("",name,ChatMessage.SERVER_CLOSED),serverid);
		try{
			new Socket("localhost",port);
		}catch(IOException ioe){}
	}
	
	
	private synchronized void publishMessage(ChatMessage cmsg,int id){
		//String s = name + ": " + msg + "\t" + date.format(new Date());		
		if(screenText != null) pushMessage(cmsg.getFullMessage());	
		for(ClientThread t : clients){
			//call send message method of client t with the string message
			if(t.id != id){
				if(!t.sendMessage(cmsg)) 
					removeClient(t.id);
			}
		}
	}
	
	private synchronized void removeClient(int id){
		for(int i=clients.size()-1;i>=0;i--){
			if(clients.get(i).id == id){
				clients.remove(i);
				break;
			}
		}
		if(clients.size() == 0) allowTyping(false);
	}
	
	
	private void pushMessage(final String msg){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				screenText.append(msg+"\n");
			}
		});
	}
	private void allowTyping(final boolean state){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				inputText.setEditable(state);
			}
		});
	}
	
	private void outputLocalAddress(){
		try{
			InetAddress me =  InetAddress.getLocalHost();
			String  dottedQuad = me.getHostAddress();
			pushMessage("Your address is: "+ dottedQuad);
		}catch(UnknownHostException ukh){pushMessage("Your ip could not be read.");}
	}
	
	class ClientThread extends Thread{
		
		Socket socket;
		ObjectInputStream in;
		ObjectOutputStream out;
		
		int id;
		String name;
		ChatMessage cmsg;
		
		public ClientThread(Socket socket){
			id = uniqueNumber++;
			this.socket = socket;
			try{
				out = new ObjectOutputStream(socket.getOutputStream());
				out.flush();
				in = new ObjectInputStream(socket.getInputStream());
			}catch(IOException ioe){ioe.printStackTrace();}
		}
		
		public void run(){
			//publish LOG message
			while(isRunning){
				//readMessage
				try{
					if(!isRunning) break;
					cmsg = (ChatMessage) in.readObject();
				}
				catch(EOFException eof){dlog("Connection closed. ["+name+"]\n");}
				catch(ClassNotFoundException cnfe){dlog(String.format("A message could not be read. [CNFE,name = %s]\n", name));}
				catch(IOException ioe){ioe.printStackTrace();dlog(String.format("The message could not be received. [I/O E, name = %s]\n", name));}
				//analyze message					
				if(cmsg.getType() == ChatMessage.TEXT){publishMessage(cmsg,id);}
				else if(cmsg.getType() == ChatMessage.JOIN){
					name = cmsg.getSender();
					publishMessage(cmsg,id);
				}
				else if(cmsg.getType() == ChatMessage.LEAVE){
					publishMessage(cmsg,id);
					break;
				}
				
			}//end of main loop
			
			removeClient(id);
			disconnect();
		}//end of run method
		
		public boolean sendMessage(ChatMessage cmsg){
			if(!socket.isConnected()){
				disconnect();
				return false;
			}
			try{
				out.writeObject(cmsg);
				out.flush();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.out.print("Could not send a message to " + name+"\n");
			}
			return true;
		}
		
		public void disconnect(){
			try{
				if(out != null) out.close();
				if(in != null) in.close();
				if(socket != null) socket.close();
			}catch(IOException ioe){ioe.printStackTrace();}
			dlog(name+" disconnected.\n");
		}
		
	}//end of thread class
	
	
	public void dlog(String s){
		if(debugMode)
			System.out.print(s);
	}
	
	
	
}//end of class
