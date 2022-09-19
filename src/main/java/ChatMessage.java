import java.io.Serializable;

public class ChatMessage implements Serializable {

	private static final long serialVersionUID = -6088368388163356260L;
	
	public static final int TEXT = 0 , JOIN = 1, LEAVE = 2, SERVER_CLOSED = 3;
	private int type;
	private String msg,sender;
	
	public ChatMessage(String msg,String sender,int type){
		this.msg = msg;
		this.sender = sender;
		this.type =type;
	}
	
	public String getMessage(){
		return msg;
	}
	public int getType(){
		return type;
	}
	public String getSender(){
		return sender;
	}
	public String getFullMessage(){
		switch(type){
		case TEXT:
			return String.format("[%s]: %s", sender,msg);
		case JOIN:
			return String.format("%s joined the room.", sender);
		case LEAVE:
			return String.format("%s disconnected.", sender);
		}
		return null;
	}
	
	public void setMessage(String msg){
		this.msg = msg;
	}
	public void setType(int type){
		this.type = type;
	}
	
}//end of class
