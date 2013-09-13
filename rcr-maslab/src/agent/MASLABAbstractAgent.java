package agent;

import agent.interfaces.IAbstractAgent;
import agent.worldmodel.AgentState;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.Constants;
import rescuecore2.log.Logger;

import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import util.BFSearch;
import util.MASLABRouting;
import agent.worldmodel.Task;

/**
   Abstract base class for sample agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class MASLABAbstractAgent<E extends StandardEntity> extends StandardAgent<E> implements IAbstractAgent{
    
    /**
     *
     * Variaveis Sample Agent
     * 
     */
    private static final int RANDOM_WALK_LENGTH = 50;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();

    /**
       The search algorithm.
    */
    protected BFSearch search;

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
       Cache of refuge IDs.
    */
    protected List<EntityID> refugeIDs;

    private Map<EntityID, Set<EntityID>> neighbours;

    
    /**
     *
     * Variaveis definidas por nós
     * 
     */
    
    /**
       The routing algorithms.
    */
    protected MASLABRouting routing;    
 
    /**
     *  ABEL
     *  fila de estados stateQueue
     */
    protected Queue<AgentState> stateQueue = new LinkedList<AgentState>();
    /**
     *  tarefa atual do agente
     *  
     */
    protected Task currentTask = null;
    /**
     *  variavel booleana que informa se ha um caminho definido
     */
    protected Boolean pathDefined = false;
    //protected List<EntityID> currentPath = new ArrayList<EntityID>();
    protected EntityID destiny = null;
    protected List<EntityID> lastPath = new ArrayList<EntityID>();
    
    
    
    /**
	   Cache of Hydrant IDs.
    */
    protected List<EntityID> hydrantIDs;
    
    
    /**
     *
     * Métodos Standard Agent
     * 
     */
    
    /**
       Construct an AbstractSampleAgent.
    */
    protected MASLABAbstractAgent() {
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        hydrantIDs = new ArrayList<EntityID>();
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
            if (next instanceof Hydrant) {
            	hydrantIDs.add(next.getID());
            }
        }
        //Criação de uma lista com hidrantes e refúgios para os bombeiros
        List<EntityID> waterIDs = new ArrayList<EntityID>();
        waterIDs.addAll(refugeIDs);
    	waterIDs.addAll(hydrantIDs);
    	search = new BFSearch(model);
        
        
        
        
        //TODO - Depois de ter os setores carregados, passar para o construtor do objeto routing
        //TODO - Carregar os hashtables e principais pontos da via principal
        //TODO - Carregar os roadIDs do principal e dos setores 
        Hashtable<EntityID, List<EntityID>> PontosPrincipais = new Hashtable<EntityID, List<EntityID>>();
        
        List<EntityID> principalIDs = new ArrayList<EntityID>();
        principalIDs.add(new EntityID(274));
        principalIDs.add(new EntityID(976));
        
        List<EntityID> aux = new ArrayList<EntityID>();
        aux.add(new EntityID(255));
        PontosPrincipais.put(new EntityID(274), aux);
        
        routing = new MASLABRouting(search.getGraph(), search.getGraph(), search.getGraph(), search.getGraph(), search.getGraph(),
        		refugeIDs,waterIDs,buildingIDs, model, PontosPrincipais);
        
        
        
        
        
        
        neighbours = search.getGraph();
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    
    /**
     *
     * Métodos Sample Agent
     * 
     */

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = ((Human)me()).getPosition();
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            for (EntityID next : possible) {
                if (seen.contains(next)) {
                    continue;
                }
                current = next;
                found = true;
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        return result;
    }
    
    /*
     *
     * Métodos Definidos por nós
     * 
     */
    
    public void debug(int time, String str){
    	System.out.println(time + " - " + me().getID() + " - " + str);
    }
    
    

    /*
     * 
     * Métodos Acessores se necessário
     * 
     * 
     */    
    protected List<EntityID> walk(EntityID origin){
    	
    	List<EntityID> path = new ArrayList<EntityID>();
    	
    	if(this.pathDefined == false || this.destiny == location().getID()){
    		
    		Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
    		List<Road> road = new ArrayList<Road>();
    		EntityID destiny = null;
    		for (StandardEntity next : e)
    		{
    			Road r = (Road)next;
    			road.add(r);
    		}
    		
    		Integer index = new Random().nextInt(road.size());
	    		
	    	destiny = road.get(index).getID();
	    	this.destiny = destiny;
	    	
    		path = search.breadthFirstSearch(origin, destiny);
    		
    		//path = this.getDijkstraPath(local, destiny, this.mapTmp);
    	}else{
    		path = this.walk(origin, this.destiny);
    	}
    	this.pathDefined = true;
    	return path;
    	
    }
    
    protected List<EntityID> walk(EntityID local, EntityID destiny)
    {
    	List<EntityID> path = new ArrayList<EntityID>();
    	path = search.breadthFirstSearch(local, destiny);
    	return path;
    }
    
    
    /**
     * método walk
     * @param path - currentPath do agente
     * @param local - local de destino
     * @return
     */
    /*protected List<EntityID> walk(List<EntityID> path, EntityID local) {
    	
    	if(path.isEmpty() || this.pathDefined == false){
    		
    		Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
    		List<Road> road = new ArrayList<Road>();
    		EntityID destiny = null;
    		for (StandardEntity next : e)
    		{
    			Road r = (Road)next;
    			road.add(r);
    		}
    		
    		Integer index = new Random().nextInt(road.size());
	    		
	    	destiny = road.get(index).getID();
	    	
    		path = search.breadthFirstSearch(local, destiny);
    		
    		//path = this.getDijkstraPath(local, destiny, this.mapTmp);
    	}else if(!this.currentPath.isEmpty()){
    		
    		//path = search.breadthFirstSearch(location().getID(), path.get((path.size() - 1)));
    		List<EntityID> tmp = new ArrayList<EntityID>();
    		int ct = 0;
    		for(EntityID p : path)
    		{
    			
    			if(p != local && ct == 0)
    			{
    				
    				tmp.add(p);
    			}else if(p == local)
    			{
    				ct = 1;
    				
    				tmp.add(p);
    				break;
    			}
    		}

    		//System.out.println("Tamanho do caminho atual "+path.size());
    		System.out.println("caminho realizado: "+tmp);
    		System.out.println("tamanho do caminho realizado: "+tmp.size());
    		path.removeAll(tmp);
    		//System.out.println("Tamanho para remoção "+tmp.size());
    	}
    	return path;
    }
    */
    
    /**
     * retorna proxima EntityID do caminho
     */
    /*protected EntityID nextVertex()
    {
    	int ct = 0;
    	for(EntityID next:currentPath)
    	{
    		if(location().getID() == next && ct == 0)
    		{
    			ct = 1;
    		}else if(ct == 1)
    		{
    			return next;
    		}
    	}
		return null;
    	
    }/*
    
    /**
     * adiciona um novo estado ao início da fila
     * @param newState
     */
    protected void addBeginingQueue(AgentState newState)
    {
    	Queue<AgentState> tmpQueue = new LinkedList<AgentState>();
		tmpQueue.addAll(stateQueue);
		stateQueue.clear();
		stateQueue.add(newState);
		stateQueue.addAll(tmpQueue);
    }
    
    /**
     * imprime a fila no timestep
     */
    protected void printQueue()
    {
    	int ct = 1;
    	System.out.println("------------------------------------");
        for(AgentState a : this.stateQueue)
        {
        	System.out.println("Estado "+ct+" - "+a.getState());
        	System.out.println("Alvo: "+a.getId());
        	ct++;
        }
        System.out.println("------------------------------------");
    }
}
