package Exploration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/*Exploração armazena o mapeamento conhecido:
 * ID : {Problem,TimeStep}
 * Problem: abc { a- civis, b- fogo, c- bloqueio}
 * 0 - ausencia de problema
 * 1 - problema conhecido
 * exemplo:
 * 001- bloqueio
 * 011- fogo e bloqueio
 * */

public class ExplorationTeste {

	@SuppressWarnings("unused")
	private StandardWorldModel model;

	static int TamanhoLista = 10;

	@SuppressWarnings("rawtypes")
	public HashMap<StandardEntity, List> Exploracao = new HashMap<StandardEntity, List>();

	public ExplorationTeste(StandardWorldModel world) {
		model = world;
		// TODO Auto-generated constructor stub
	}


	/**
	 * Armazena nova informação do mapa
	 * @param time
	 *            - Step {Quando o problema foi vizualizado}
	 * @param ID
	 *            - Local do incidente
	 * @param Problem
	 *            - Problema visualizado
	 */
	@SuppressWarnings("unchecked")
	public void InsertNewInformation(int time, StandardEntity ID, String Problem) {
		// Caso o problema seja conhecido será atualizado o conhecimento
		if (Exploracao.containsKey(ID)) {
			UpdateInformation(time, ID, Problem);

			// Caso Contrario é inserio um novo conhecimento
		} else {
			Exploracao.put(ID, Arrays.asList(Problem, time));
		}

	}

	/**
	 * Atualiza informações conhecidas do mapa
	 * 
	 * @param timeAtual
	 *            - Idade da informação
	 * @param ID
	 *            - Localização
	 * @param Problem
	 *            - Problema
	 */
	@SuppressWarnings("unchecked")
	public void UpdateInformation(int timeAtual, StandardEntity ID,
			String Problem) {
		int timeAntigo;
		timeAntigo = Integer.parseInt(Exploracao.get(ID).get(1).toString());

		// Atualiza caso o tempo da informação seja mais importante
		if (timeAtual > timeAntigo) {
			Exploracao.remove(ID);
			Exploracao.put(ID, Arrays.asList(Problem, timeAtual));
		} else {
			// Não atualiza
		}

	}
	
	/**
	 * Estabelece uma probabilidade de exploracao com base na diferenca temporal:
	 *  100*(1-(0.999^(TAtual - Tvisitado))) 
	 * Assim sorteia (roleta) um node para exploracao;
	 * @param timeAtual   Tempo atual da simulação
	 * @return StandardEntity do node à visitar
	 */
	public StandardEntity GetPathEntity(int timeAtual) {
		double limite=0;
		double auxlimear;
		HashMap<StandardEntity, Double> Roleta = new HashMap<StandardEntity, Double>();
		Set<StandardEntity> chaves = Exploracao.keySet();
		
		int auxTime = 0;
		StandardEntity value = null;
		
		for (StandardEntity chave : chaves) {
			// Calculo do tempo: 100*( 1 - ( 0.999 ^ ( Tempo Atual - Tempo de Visita)))
			int time = timeAtual - Integer.parseInt(Exploracao.get(chave).get(1).toString());
			auxlimear = 100*(1-(Math.pow(0.9999,time)));
			// Gera uma Roleta
			Roleta.put(chave, auxlimear);
			limite = limite + auxlimear;
			if(auxTime <= time){
				auxTime = time;
				value =chave;
			}
			// Guarda o valor com mais tempo sem acessar
		}
		
		/*   Sorteio de um limear para exploção     */
		Random rand = new Random();
		double numerodasorte = (int)(rand.nextDouble()*limite);		
		/* Seleção do node */
		Set<StandardEntity> indices = Roleta.keySet();
		for (StandardEntity indice : indices) {
			if(numerodasorte > Roleta.get(indice).doubleValue()){
				return indice;
			}
		}
		return value;
	
	}
	
	
	/**
	 * 
	 * @return Lista de Prédios em chamas conhecidos
	 */
	@SuppressWarnings({ "unused", "null" })
	public List<StandardEntity> GetBurningBuilds(){
		HashMap<StandardEntity, Double> Roleta = new HashMap<StandardEntity, Double>();
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> Predios = null;
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(0,1).equals("1")){
				Predios.add(chave);
			}
		}
		return Predios;
	}
	
	/**
	 * 
	 * @return Lista de civis identificado em estado de perigo
	 */
	@SuppressWarnings({ "unused", "null" })
	public List<StandardEntity> GetInjuredCivilians(){

		HashMap<StandardEntity, Double> Roleta = new HashMap<StandardEntity, Double>();
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> civis = null;
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(0).equals("1")){
				civis.add(chave);
			}
		}
		return civis;
	}
	
	/**
	 * 
	 * @return Lista de bloqueios identificados
	 */
	@SuppressWarnings("null")
	public List<StandardEntity> GetBlockRoads(){
		@SuppressWarnings("unused")
		HashMap<StandardEntity, Double> Roleta = new HashMap<StandardEntity, Double>();
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> bloqueios = null;
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(1,2).equals("1")){
				bloqueios.add(chave);
			}
		}
		return bloqueios;
	}
	
	/**
	 * 
	 * @param informacoes {Lista de problemas a ser limpo}
	 * @param problema { 2- bloqueio, 1- incendio, 0- civis }
	 */
	@SuppressWarnings("unchecked")
	public void clear(List<StandardEntity> informacoes , int problema){
		// Civis
		if (problema == 0){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = "0"+aux.substring(0,1)+aux.substring(1,2);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Incendios
		if (problema == 1){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0)+"0"+aux.substring(1,2);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Bloqueios
		if (problema == 2){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0)+aux.substring(0,1)+"0";
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		
		
	}


}
