package model;

public class AbstractMessage {

	public String MSG = "";
	public static final String MSG_SEPARATOR = "-";
	public static final String MSG_FIM = ",";
	
	public AbstractMessage (String...strings){
        //monta a mensagem em um string
        for (int i = 0; i < strings.length; i++) {
            if (i < strings.length - 1) {
            	MSG += strings[i] + MSG_SEPARATOR;
            } else {
            	MSG += strings[i];
            }
        }
        MSG += MSG_FIM;
	}
	
	public String getMSG(){
		return MSG;
	}
	
	public String getMSG_SEPARATOR(){
		return MSG_SEPARATOR;
	}

	public String getMSG_FIM(){
		return MSG_FIM;
	}
	
	public String toString(){
		return getMSG();
	}
}
