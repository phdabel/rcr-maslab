package util;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Entity;
import rescuecore2.misc.WorkerThread;
import rescuecore2.misc.collections.LazyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

import org.hamcrest.core.IsNull;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.Area;

/**
 * Classe que executa o algoritmo de roteamento.
 */
public final class MASLABBFSearch {

	private Map<EntityID, Set<EntityID>> graph;
	private StandardWorldModel model;

	/**
	 * Construct a new SampleSearch.
	 * 
	 * @param world
	 *            The world model to construct the neighbourhood graph from.
	 */
	public MASLABBFSearch(StandardWorldModel world) {
		model = world;
		Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
			@Override
			public Set<EntityID> createValue() {
				return new HashSet<EntityID>();
			}
		};
		for (Entity next : world) {
			if (next instanceof Area) {
				Collection<EntityID> areaNeighbours = ((Area) next)
						.getNeighbours();
				neighbours.get(next.getID()).addAll(areaNeighbours);
			}
		}
		setGraph(neighbours);
	}

	/**
	 * Construct a new ConnectionGraphSearch.
	 * 
	 * @param graph
	 *            The connection graph in the form of a map from EntityID to the
	 *            set of neighbouring EntityIDs.
	 */
	public MASLABBFSearch(Map<EntityID, Set<EntityID>> graph) {
		setGraph(graph);
	}

	/**
	 * Set the neighbourhood graph.
	 * 
	 * @param newGraph
	 *            The new neighbourhood graph.
	 */
	public void setGraph(Map<EntityID, Set<EntityID>> newGraph) {
		this.graph = newGraph;
	}

	/**
	 * Get the neighbourhood graph.
	 * 
	 * @return The neighbourhood graph.
	 */
	public Map<EntityID, Set<EntityID>> getGraph() {
		return graph;
	}

	/**
	 * Do a breadth first search from one location to the closest (in terms of
	 * number of nodes) of a set of goals.
	 * 
	 * @param start
	 *            The location we start at.
	 * @param Bloqueios
	 *            Os bloqueios conhecidos durante o trajeto
	 * @param goals
	 *            The set of possible goals.
	 * @return The path from start to one of the goals, or null if no path can
	 *         be found.
	 */
	public List<EntityID> breadthFirstSearch(EntityID start,
			List<EntityID> Bloqueios, EntityID... goals) {
		return breadthFirstSearch(start, Bloqueios, Arrays.asList(goals));
	}

	public List<EntityID> breadthFirstSearch(EntityID start,
			Collection<EntityID> goals) {
		return breadthFirstSearch(start, new ArrayList<EntityID>(), goals);
	}

	public List<EntityID> breadthFirstSearch(EntityID start, EntityID goals) {
		return breadthFirstSearch(start, new ArrayList<EntityID>(), goals);
	}

	/**
	 * Do a breadth first search from one location to the closest (in terms of
	 * number of nodes) of a set of goals.
	 * 
	 * @param start
	 *            The location we start at.
	 * @param goals
	 *            The set of possible goals.
	 * @return The path from start to one of the goals, or null if no path can
	 *         be found.
	 */
	public List<EntityID> breadthFirstSearch(EntityID start,
			List<EntityID> Bloqueios, Collection<EntityID> goals) {
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		try {
			do {
				next = open.remove(0);
				if (isGoal(next, goals)) {
					found = true;
					break;
				}
				if (!isBlocked(next, Bloqueios)) {
					Collection<EntityID> neighbours = graph.get(next);
					if (neighbours.isEmpty()) {
						continue;
					}
					for (EntityID neighbour : neighbours) {
						if (isGoal(neighbour, goals)) {
							ancestors.put(neighbour, next);
							next = neighbour;
							found = true;
							break;
						} else {
							if (!ancestors.containsKey(neighbour)) {
								open.add(neighbour);
								ancestors.put(neighbour, next);
							}
						}
					}
				}
			} while (!found && !open.isEmpty());
		} catch (Exception e) {
			EntityID current = next;
			List<EntityID> path = new LinkedList<EntityID>();
			do {
				path.add(0, current);
				current = ancestors.get(current);
				if (current == null) {
					throw new RuntimeException(
							"Found a node with no ancestor! Something is broken.");
				}
			} while (current != start);
		}
		if (!found) {
			// No path
			EntityID current = next;
			List<EntityID> path = new LinkedList<EntityID>();
			do {
				path.add(0, current);
				current = ancestors.get(current);
				if (current == null) {
					throw new RuntimeException(
							"Found a node with no ancestor! Something is broken.");
				}
			} while (current != start);
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);
		return path;
	}

	private boolean isGoal(EntityID e, Collection<EntityID> test) {
		return test.contains(e);
	}

	private boolean isBlocked(EntityID e, Collection<EntityID> test) {
		return test.contains(e);
	}

	public List<EntityID> breadthFirstSearch(EntityID start, List<EntityID> Bloqueios, 
			EntityID goal, Boolean ConsiderarBuildings) {
		Collection<EntityID> goals = Arrays.asList(goal);
		List<EntityID> open = new LinkedList<EntityID>();
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		EntityID next = null;
		boolean found = false;
		ancestors.put(start, start);
		do {
			next = open.remove(0);
			if (isGoal(next, goals)) {
				found = true;
				break;
			}
			
			if (!isBlocked(next, Bloqueios)) {
				Collection<EntityID> neighbours = graph.get(next);
				if(neighbours.isEmpty()){
					continue;
				}
				if (!ConsiderarBuildings){
					Collection<EntityID> aux = new ArrayList<EntityID>(neighbours);
					for(EntityID neighbour : aux) {
						if(model.getEntity(neighbour).getClass().equals(Building.class)){
							System.out.println(neighbour.getValue());
							neighbours.remove(neighbour);
						}
					}
				}
				if (neighbours.isEmpty()) {
					continue;
				}
				for (EntityID neighbour : neighbours) {
					if (isGoal(neighbour, goals)) {
						ancestors.put(neighbour, next);
						next = neighbour;
						found = true;
						break;
					} else {
						if (!ancestors.containsKey(neighbour)) {
							open.add(neighbour);
							ancestors.put(neighbour, next);
						}
					}
				}
			}
		} while (!found && !open.isEmpty());
		if (!found) {
			// No path
			EntityID current = next;
			List<EntityID> path = new LinkedList<EntityID>();
			do {
				path.add(0, current);
				current = ancestors.get(current);
				if (current == null) {
					throw new RuntimeException(
							"Found a node with no ancestor! Something is broken.");
				}
			} while (current != start);
			return null;
		}
		// Walk back from goal to start
		EntityID current = next;
		List<EntityID> path = new LinkedList<EntityID>();
		do {
			path.add(0, current);
			current = ancestors.get(current);
			if (current == null) {
				throw new RuntimeException(
						"Found a node with no ancestor! Something is broken.");
			}
		} while (current != start);
		return path;
	}
	
}
