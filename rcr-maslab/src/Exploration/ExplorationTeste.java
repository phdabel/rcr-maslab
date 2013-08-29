package Exploration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;


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
	
	private StandardWorldModel model;

	static int TamanhoLista = 10;
	
	public HashMap<StandardEntity, List> Exploracao  = new HashMap<StandardEntity, List>();
	
	public ExplorationTeste(StandardWorldModel world) {
		model = world;
		// TODO Auto-generated constructor stub
	}
	
	/** Armazena nova informação do mapa
	 * 
	 * @param time - Step {Quando o problema foi vizualizado}
	 * @param ID   - Local do incidente
	 * @param Problem - Problema visualizado
	 */
	public void InsertNewInformation(int time,StandardEntity ID, String Problem){
		// Caso o problema seja conhecido será atualizado o conhecimento
		if(Exploracao.containsKey(ID)){
			UpdateInformation(time, ID, Problem);
		
		// Caso Contrario é inserio um novo conhecimento
		}else{
			Exploracao.put(ID,Arrays.asList(Problem,time));
		}
		
	}
	
	/** Atualiza informações conhecidas do mapa
	 * 
	 * @param timeAtual  -  Idade da informação
	 * @param ID         -  Localização 
	 * @param Problem    -  Problema
	 */
	public void UpdateInformation(int timeAtual,StandardEntity ID, String Problem){
		int timeAntigo;
		timeAntigo = Integer.parseInt(Exploracao.get(ID).get(1).toString());
		
		// Atualiza caso o tempo da informação seja mais importante
		if(timeAtual > timeAntigo){
			Exploracao.remove(ID);
			Exploracao.put(ID, Arrays.asList(Problem,timeAtual));
		} else{
			// Não atualiza
		}
		
	}
	
	/** Retorna informações do caminho desejado
	 * 
	 * @param passos - Estimativa para chegar
	 * @param stepAtual  -  Tempo atual
	 * @param CaminhoDesejado - Por onde desejo ir
	 * @return
	 */
	public HashMap<StandardEntity, List> GetLastpath(int passos, int stepAtual, List<EntityID> CaminhoDesejado ){
		HashMap<StandardEntity, List> informacoes = new HashMap<StandardEntity, List>();
		
		Set<StandardEntity> chaves = Exploracao.keySet();
        for (StandardEntity chave : chaves)  
        { 
        	// Se eu conheço o caminho desejado que o outro agente deseja seguir
        	if(CaminhoDesejado.contains(Exploracao.get(chave))){
        		// Se o tempo da informação é valido
        		if(stepAtual - Integer.parseInt(Exploracao.get(chave).get(1).toString()) > passos){
        			informacoes.put(chave, Arrays.asList(Exploracao.get(chave).get(0),Exploracao.get(chave).get(1)));
        		}
        	}
        	
        }
        // caso não tenha informações sobre a rota
		if (informacoes.isEmpty()){
			return null;
		}
		// senão informa as iformações desejadas
		return informacoes;
	}
	
	
	/** Retorna uma lista das acoes preferenciais dos agentes
	 * 
	 * @param Agente - Define o tpo do agente - 1: Ambulancia , 2: Bombeiro , 3: Bloqueio
	 * @param IDPosition - Define o Id da posicao onde o agente se encontra
	 * @param Preferencia - 1: Itens mais atuais , 2: Itens mais Proximos
	 * @return
	 */
	public  List<StandardEntity> Preferencia(int Agente, StandardEntity IDPosition, int Preferencia){
		List<StandardEntity> AcoesPreferenciais = new ArrayList<StandardEntity>();
		
		if(Preferencia == 1){
			AcoesPreferenciais = Preferencia_MelhorTempo(Agente, IDPosition);
			
		}else if(Preferencia ==2 ){
			
			
		}else{
			
		}
		return AcoesPreferenciais;
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<StandardEntity> Preferencia_MelhorTempo(int Agente, StandardEntity IDPosition){
		
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
	

	


	

	

}
