package model;

import java.util.List;
import java.util.LinkedList;

import rescuecore2.standard.entities.FireBrigade;

public class BurningBuilding {
	private int ID;
	private int tempoAtual;
//	private int tempoEstimado;
	private int importancia;
	private List<Integer> IDsCorrespondentes = new LinkedList<Integer>();
	
	public BurningBuilding(int iD, int tA, int imp){
		ID = iD;
		tempoAtual = tA;
		importancia = imp;
		IDsCorrespondentes.add(iD);
	}

/*	public BurningBuilding(int iD, int tA, int tE){
		ID = iD;
		tempoAtual = tA;
		tempoEstimado = tE;
	}
*/
/*	public int getTempoRestante(int time){
		return (tempoEstimado - (time - tempoAtual));
	}
*/
	public int getID(){
		return ID;
	}
	public int getTempoAtual(){
		return tempoAtual;
	}
/*	public int getTempoEstimado(){
		return tempoEstimado;
	}
*/
	public int getImportancia(){
		return importancia;
	}
	public void addID(int id){
		IDsCorrespondentes.add(id);
	}
	public List<Integer> getIDsCorrespondentes(){
		return IDsCorrespondentes;
	}
	public void AtualizarImportancia(int imp){
		importancia = imp;
	}
	public void RemoverIDCorrespondente(int id){
		IDsCorrespondentes.remove(id);
	}
}
