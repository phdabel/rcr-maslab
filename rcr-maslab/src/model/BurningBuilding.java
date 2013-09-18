package model;

public class BurningBuilding {
	private int ID;
	private int tempoAtual;
	private int tempoEstimado;
	
	public BurningBuilding(int iD, int tA, int tE){
		ID = iD;
		tempoAtual = tA;
		tempoEstimado = tE;
	}
	public int getTempoRestante(int time){
		return (tempoEstimado - (time - tempoAtual));
	}
	public int getID(){
		return ID;
	}
	public int getTempoEstimado(){
		return tempoEstimado;
	}
	public int getTempoAtual(){
		return tempoAtual;
	}
}
