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
	
	public StandardEntity GetExplorationNode(int Time, EntityID start, Map<EntityID, Set<EntityID>> graph, List<StandardEntity> nodesconhecidos, int agent) {
		Setor = graph;

		

		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);

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
				// Caso o node não foi visitados
				if (!isGoal(neighbour, nodesconhecidos)) {
					System.out.println(":--> : "+model.getEntity(neighbour));
					return  model.getEntity(neighbour);
				} 
				//System.out.println("Visitou: "+ neighbour);
				if (ancestors.containsKey(neighbour)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
				}		
			}
		} while (!found && !open.isEmpty());

		// Caso todos os nodes foram explorado....
		//exploracao.GetNewExplorationNode(Time)
		System.out.println("Exploração Completa ");
		return null;

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
