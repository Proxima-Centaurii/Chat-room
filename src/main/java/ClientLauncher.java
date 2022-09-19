import javax.swing.JOptionPane;

public class ClientLauncher {

	public static void main(String args[]){
		ClientCore client = new ClientCore(getName());
		client.setResizable(false);
		client.startRunning(getIp(), getPort());
	}
	
	private static String getName(){
		String s = "" ;
		return (s = JOptionPane.showInputDialog("Enter name below.")) != "" ? s : "IDIOT_CLIENT";
	}
	
	private static int getPort(){
		int p = Integer.parseInt(JOptionPane.showInputDialog("Enter a port below"));
		return p < 0 || p >= 65535 ? 7777:p;
	}
	
	private static String getIp(){
		String s = "127.0.0.1";
		return (s = JOptionPane.showInputDialog("Enter an ip below.")) != "" ? s : "127.0.0.1";
	}
	
}//end of class
