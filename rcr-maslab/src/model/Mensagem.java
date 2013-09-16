package model;

public class Mensagem {

	private String MSG;
    protected final String MSG_SEPARATOR = "-";
    protected final String MSG_FIM = ".";
	
	public Mensagem (String...strings){
        //monta a mensagem em um string
        for (int i = 0; i < strings.length; i++) {
            if (i < strings.length - 1) {
            	MSG += strings[i] + MSG_SEPARATOR;
            } else {
            	MSG += strings[i];
            }
            MSG += MSG_FIM;
        }
	}
	
	public String getMSG(){
		return MSG;
	}
}
