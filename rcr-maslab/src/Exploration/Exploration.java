package Exploration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/*Exploração armazena o mapeamento conhecido em um Hash da forma:
 *  	- StandardEntity ID : {String Problem, int TimeStep}
 * O problema é definido da seguinte forma: abc { a- civis, b- fogo, c- bloqueio}, onde:
 * 		- 0 : ausencia de problema
 * 		- 1 : problema conhecido
 * Por exemplo:
 * 		- 001 : bloqueio
 * 		- 011 : fogo e bloqueio
 *      - 000 : nada
 * */

public class Exploration {

	@SuppressWarnings("unused")
	private StandardWorldModel model;
	static int TamanhoLista = 10;
	@SuppressWarnings("rawtypes")
	public HashMap<StandardEntity, List> Exploracao = new HashMap<StandardEntity, List>();
	public Exploration(StandardWorldModel world) {
		model = world;
		// TODO Auto-generated constructor stub
	}


	/**
	 * Armazena nova informação do mapa
	 * @param int time- Step {Quando o problema foi vizualizado}
	 * @param StandardEntity ID - Local do incidente
	 * @param String Problem - Problema visualizado
	 */
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
	 * @param int timeAtual- Idade da informação
	 * @param StandardEntity ID - Localização
	 * @param String Problem- Problema
	 */
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
	 *  100*(1-(0.999^(TAtual - Tvisitado))) {normaliza os valores num intervalo de 0 à 100}
	 * Assim sorteia (roleta) um node para exploracao;
	 * @param int timeAtual - Tempo atual da simulação
	 * @return StandardEntity node - node à visitar
	 */
	public StandardEntity GetNewExplorationNode(int timeAtual) {
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
			limite = limite + auxlimear;
			Roleta.put(chave, limite);

			// Guarda o valor com mais tempo sem acessar
			if(auxTime <= time){
				auxTime = time;
				value =chave;
			}

		}
		
		/*   Sorteio de um limear para exploção     */
		Random rand = new Random();
		double numerodasorte = (int)(rand.nextDouble()*limite);		
		/* Seleção do node */
		Set<StandardEntity> indices = Roleta.keySet();
		for (StandardEntity indice : indices) {
			if(numerodasorte <= Roleta.get(indice).doubleValue()){
				return indice;
			}
		}
		return value;
	
	}
	
	
	/**
	 * Obtém a lista de edificios em chamas identificados
	 * @return List<StandardEntity> Edificios - Lista de Prédios em chamas conhecidos
	 */
	public List<StandardEntity> GetBurningBuilds(){
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> Predios =  new ArrayList<>();
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(1,2).equals("1")){
				Predios.add(chave);
			}
		}
		return Predios;
	}
	
	/**
	 * Obtém a lista de civis feridos identificados
	 * @return List<StandardEntity> Civis - Lista de civis identificado em estado de perigo
	 */
	public List<StandardEntity> GetInjuredCivilians(){
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> civis = new ArrayList<>();
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(0,1).equals("1")){
				civis.add(chave);
			}
		}
		return civis;
	}
	
	/**
	 * Obtém a lista de Bloqueios identificados
	 * @return List<StandardEntity> Bloqueio - Lista de bloqueios identificados
	 */
	public List<StandardEntity> GetBlockRoads(){
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> bloqueios =  new ArrayList<>();
		
		for (StandardEntity chave : chaves) {
			if(Exploracao.get(chave).get(0).toString().substring(2,3).equals("1")){
				bloqueios.add(chave);
			}
		}
		return bloqueios;
	}
	
	/**
	 * Limpa as informações da exploração do agente
	 * @param List<StandardEntity> informacoes - Lista de problemas a ser limpo
	 * @param int problema - 0- civis, 1- incendio, 2- bloqueio 
	 */
	public void clear(List<StandardEntity> informacoes , int problema){
		// Civis
		if (problema == 0){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = "0"+aux.substring(1,2)+aux.substring(2,3);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Incendios
		if (problema == 1){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0,1)+"0"+aux.substring(2,3);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Bloqueios
		if (problema == 2){
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(0).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(1).toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0,1)+aux.substring(1,2)+"0";
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Obtém uma lista das ultimas ações dos agentes
	 * @return List<StandardEntity> UltimosNodesVisitados - Retorna uma lista dos ultimos lugares visitados pelo agente
	 */
	public List<StandardEntity> GetLastAction(){
		
		   List mapKeys = new ArrayList<StandardEntity>(Exploracao.keySet());
		   List mapValues = new ArrayList(Exploracao.get(1));
		   
		   Collections.sort(mapValues);
		   Collections.sort(mapKeys);

		   LinkedHashMap sortedMap = new LinkedHashMap();

		   Iterator valueIt = mapValues.iterator();
		   while (valueIt.hasNext()) {
		       Object val = valueIt.next();
		    Iterator keyIt = mapKeys.iterator();

		    while (keyIt.hasNext()) {
		        Object key = keyIt.next();
		        String comp1 = Exploracao.get(key).toString();
		        String comp2 = val.toString();

		        if (comp1.equals(comp2)){
		        	Exploracao.remove(key);
		            mapKeys.remove(key);
		            sortedMap.put((String)key, (Double)val);
		            break;
		        }

		    }

		}
		return (List<StandardEntity>) sortedMap;
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	public List<StandardEntity> GetExplorationNodes() {
		List<StandardEntity> Explorationnodes = new ArrayList<>();
		Explorationnodes = (List<StandardEntity>) Exploracao.keySet();
	
		return Explorationnodes;

	}


}
