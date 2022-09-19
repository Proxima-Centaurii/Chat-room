import javax.swing.JOptionPane;

public class ServerLauncher {
	
	public static void main(String args[]){
		ServerCore server = new ServerCore(getName());
		server.setResizable(false);
		server.startRunning(getPort(), 4);
	}
	
	
	private static String getName(){
		String s = "";
		return (s = JOptionPane.showInputDialog("Enter a name below.")) != "" ? s : "IDIOT_HOST";
	}
	
	private static int getPort(){
		int p = Integer.parseInt(JOptionPane.showInputDialog("Enter a port below"));
		return p < 0 || p >= 65535 ? 7777:p;
	}
	
}//end of class
