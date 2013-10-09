package Exploration;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class WalkingInSector {

	private StandardWorldModel model;
	private Map<EntityID, Set<EntityID>> Setor;
	//private ;
	//Exploration exploracao;
	
	public WalkingInSector(StandardWorldModel world){
		model = world;
	}
	
	/**
	 * Obtém o node a ser explorado
	 * @param int Time - Tempo atual da simulação 
	 * @param EntityID start - Posição do Agente
	 * @param StandardWorldModel world - mapa
	 * @param Map<EntityID, Set<EntityID>> graph - Setor
	 * @param StandardEntity node atual
	 * @param classe de agente - 0: policial 1: bombeiro 2: ambulancia
	 * @return StandardEntity Node - Node a ser explorado
	 */
	
	public StandardEntity GetExplorationNode(int Time, EntityID start, Map<EntityID, Set<EntityID>> graph, List<StandardEntity> nodesconhecidos, StandardEntity ultimonode, int agent) {
		Setor = graph;
		
		//nodesconhecidos = exploracao.GetExplorationNodes();

		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		//System.out.println("ME: "+start.toString()+ " open:"+open.toString());
		do {
			next = open.remove(0);
			Collection<EntityID> neighbours = Setor.get(next);
			// se não existir nodes filhos continua
			//System.out.println("Vizinhos: "+ neighbours.toString());
			if (neighbours.isEmpty()) {
				continue;
			}
			// Para os nodes filhos
			for (EntityID neighbour : neighbours) {
				// Caso os nodes ja foram visitados
				if (isGoal(neighbour, nodesconhecidos)) {
					//System.out.println("Visitou: "+ neighbour);
					if (ancestors.containsKey(neighbour)) {
						// caso seja um policial 
						if(agent == 0){
							// apenas as roads 
							if(model.getEntitiesOfType(StandardEntityURN.BUILDING).contains(neighbour)){
								// SEM PREDIOS
							}else{
								// o/
							open.add(neighbour);
							ancestors.put(neighbour, next);
							}	
						}else{
						open.add(neighbour);
						ancestors.put(neighbour, next);
						}
					}
					// Caso o node ainda não foi explorado
				} else {
					//System.out.println("Buscando novo node de Exploração: "+ neighbour + "Já Explorado: " + nodesconhecidos );
					// Atualiza a lista de exploração
					if(model.getEntitiesOfType(StandardEntityURN.BUILDING).contains(neighbour)){
						
					}else{
						return  model.getEntity(neighbour);
					}
				}
			}
		} while (!found && !open.isEmpty());

		// Caso todos os nodes foram explorado....
		//exploracao.GetNewExplorationNode(Time)
		//System.out.println("Exploração Completa: "+ ultimonode);
		return ultimonode;

	}

	/**
	 * Verifica se um determinado node já foi explorado
	 * 
	 * @param EntityID
	 *            e - Node
	 * @param List
	 *            <StandardEntity> nodesconhecidos2 - Lista de exploração
	 * @return true/false
	 */
	private boolean isGoal(EntityID e, List<StandardEntity> nodesconhecidos2) {
		return nodesconhecidos2.contains(model.getEntity(e));
	}

}
