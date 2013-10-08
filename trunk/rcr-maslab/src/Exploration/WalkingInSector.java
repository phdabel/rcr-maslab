package Exploration;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class WalkingInSector {

	private StandardWorldModel model;
	private Map<EntityID, Set<EntityID>> Setor;
	private List<StandardEntity> nodesconhecidos;

	/**
	 * Obtém o node a ser explorado
	 * @param int Time - Tempo atual da simulação 
	 * @param EntityID start - Posição do Agente
	 * @param StandardWorldModel world - mapa
	 * @param Map<EntityID, Set<EntityID>> graph - Setor
	 * @return StandardEntity Node - Node a ser explorado
	 */
	
	public StandardEntity GetExplorationNode(int Time, EntityID start,
			StandardWorldModel world, Map<EntityID, Set<EntityID>> graph) {
		
		model = world;
		Setor = graph;
		Exploration exploracao = new Exploration(world);
		nodesconhecidos = exploracao.GetExplorationNodes();

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
			if (neighbours.isEmpty()) {
				continue;
			}
			// Para os nodes filhos
			for (EntityID neighbour : neighbours) {
				// Caso os nodes ja foram visitados
				if (isGoal(neighbour, nodesconhecidos)) {
					if (ancestors.containsKey(neighbour)) {
						open.add(neighbour);
						ancestors.put(neighbour, next);
					}
					// Caso o node ainda não foi explorado
				} else {
					// Atualiza a lista de exploração
					exploracao.InsertNewInformation(Time, model.getEntity(neighbour), "000", 0, 0);
					return  model.getEntity(neighbour);
				}
			}
		} while (!found && !open.isEmpty());

		// Caso todos os nodes foram explorado....
		return exploracao.GetNewExplorationNode(Time);

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
		return nodesconhecidos2.contains(e);
	}

}
