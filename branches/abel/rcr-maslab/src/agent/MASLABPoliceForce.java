package agent;

import agent.interfaces.IPoliceForce;
import agent.worldmodel.*;
import agent.worldmodel.Object;
import agent.worldmodel.AgentState;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Vector2D;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;

/**
 * A sample police force agent.
 */
public class MASLABPoliceForce extends MASLABAbstractAgent<PoliceForce> implements IPoliceForce {

    /**
     *
     * Variaveis Sample Agent
     *
     */
    private static final String DISTANCE_KEY = "clear.repair.distance";
    private int distance;
    /**
     *
     * Variaveis definidas por nós
     *
     */
    private int resultado;
    /*
     *
     * Métodos Standard Agent
     * 
     */

    @Override
    public String toString() {
        return "MASLAB police force";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        distance = config.getIntValue(DISTANCE_KEY);
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        
        
        /**
         * INSERÇÃO DE TAREFAS NA FILA
         */
        if(this.currentTask != null && this.stateQueue.isEmpty() && this.currentTask.getObject() == Object.BLOCKADE)
        {
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk"){
        		stateQueue.remove(stateQueue.peek());
        	}
        	Task tmp = this.currentTask;
        	EntityID tmpID = new EntityID(tmp.getId());
        	
        	if(me().getPosition() != tmpID)
        	{
        		stateQueue.add(new AgentState("Walk", tmpID));
        		stateQueue.add(new AgentState("Unblock", tmpID));        		
        	}else{
        		stateQueue.add(new AgentState("Unblock", tmpID));
        		
        	}
        		
        }else if (this.stateQueue.isEmpty() && time > config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)){
        	stateQueue.add(new AgentState("RandomWalk"));
        	
        }
        this.printQueue();
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na fila de ações
         */
        if(!stateQueue.isEmpty())
        {
        	
        	AgentState currentAction = stateQueue.peek();
        	switch(currentAction.getState())
        	{
        		
        		case "RandomWalk":
        			//System.out.println("Random Walk da pilha de estados.");
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				currentPath = this.walk(currentPath, me().getPosition());
        				pathDefined = true;
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				pathDefined = false;
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        				currentPath.clear();
        				return;
        			}else if(pathDefined == true){
        				currentPath = this.walk(currentPath, me().getPosition());
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        				return;
        			}
        			break;
        		
        			//procurar por bloqueio proximo do agente
        		case "LookingNearBlockade":
        			Blockade target = getTargetBlockade();
        			if(stateQueue.peek().getState() == "LookingNearBlockade")
        			{
        					stateQueue.poll();
        			}
        			if(target != null)
        			{
        				this.addBeginingQueue(new AgentState("Unblock", target.getID()));
        				this.addBeginingQueue(new AgentState("Walk", target.getPosition()));
        			}
        			break;
        		case "Walk":
        			if(currentPath.isEmpty() && pathDefined == false){
        				//currentPath = search.breadthFirstSearch(me().getPosition(), currentAction.getId());
        				currentPath = search.breadthFirstSearch(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath, me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					this.addBeginingQueue(new AgentState("Unblock",me().getPosition()));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() >2 && pathDefined == true)
        			{
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath, me().getPosition());
        				
        				if(lastPath.size() == currentPath.size())
        				{
        					this.addBeginingQueue(new AgentState("Unblock", me().getPosition()));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				sendMove(time,currentPath);
        				pathDefined = false;
        				return;
        			}
        			
        			break;
        		
        		case "Unblock":
        			Area newTarget = (Area)model.getEntity(currentAction.getId());
        			if(newTarget.isBlockadesDefined())
        			{
        				/**
        				 * pegar ponto mais próximo da próxima Road
        				 */
        				Area nextLocation = (Area)model.getEntity(this.nextVertex());
        				List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(nextLocation.getApexList()), true);
    					double best = Double.MAX_VALUE;
    					Point2D bestPoint = null;
    					Point2D origin = new Point2D(me().getX(), me().getY());
    					for(Line2D next : lines)
    					{
    						Point2D closest = GeometryTools2D.getClosestPoint(next, origin);
    						double d = GeometryTools2D.getDistance(origin, closest);
    						if(d < best)
    						{
    							best = d;
    							bestPoint = closest;
    						}
    					}
    					Vector2D v = bestPoint.minus(new Point2D(me().getX(), me().getY()));
    					v = v.normalised().scale(1000000);
    					/**
    					 * 
    					 */
    					
        				Double bestBlockadeDist = Double.MAX_VALUE;
        				Point2D candidateBlockade = null;
        				Vector2D v2 = null;
        				for(EntityID b:newTarget.getBlockades())
        				{
        					Blockade bloqueio = (Blockade)model.getEntity(b);
        					List<Line2D> linesBlockade = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(bloqueio.getApexes()), true);
        					double bestBlockadeD = Double.MAX_VALUE;
        					Point2D bestBlockadePoint = null;
        					for(Line2D nextLine: linesBlockade)
        					{
        						Point2D closestBlockade = GeometryTools2D.getClosestPoint(nextLine, new Point2D(v.getX(),v.getY()));
        						double dBlock = GeometryTools2D.getDistance(new Point2D(v.getX(), v.getY()), closestBlockade);
        						if(dBlock < bestBlockadeD)
        						{
        							bestBlockadeD = dBlock;
        							bestBlockadePoint = closestBlockade;
        						}
        						v2 = bestBlockadePoint.minus(new Point2D(me().getX(), me().getY()));
            					v2 = v2.normalised().scale(1000000);
        					}
        					if(bestBlockadeD < bestBlockadeDist)
        					{
        						candidateBlockade = bestBlockadePoint;
        						bestBlockadeDist = bestBlockadeD;
        					}
        				}
        			       			
        			sendClear(time, (int) (me().getX() + v2.getX()), (int) (me().getY() + v2.getY()));
        			return;
        		
        			}else{
        				stateQueue.poll();
        			}
        			
        			break;
        	}
        }
        /**
         * fim da máquina de estados
         */
        /*
        // Am I near a blockade?
        Blockade target = getTargetBlockade();
        if (target != null) {
            
            sendSpeak(time, 1, ("Clearing " + target).getBytes());
//            sendClear(time, target.getX(), target.getY());
            
            return;
        }
        // Plan a path to a blocked area
        List<EntityID> path = search.breadthFirstSearch(me().getPosition(), getBlockedRoads());
        if (path != null) {
            Logger.info("Moving to target");
            Road r = (Road) model.getEntity(path.get(path.size() - 1));
            Blockade b = getTargetBlockade(r, -1);
            sendMove(time, path, b.getX(), b.getY());
            Logger.debug("Path: " + path);
            Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
            return;
        }
        Logger.debug("Couldn't plan a path to a blocked road");
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());*/
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
    }

    /**
     *
     * Métodos Sample Agent
     *
     */
    private List<EntityID> getBlockedRoads() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
        List<EntityID> result = new ArrayList<EntityID>();
        for (StandardEntity next : e) {
            Road r = (Road) next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                result.add(r.getID());
            }
        }
        return result;
    }
    
    private void cleanBlockadeAhead(int time){
    	
    	Blockade newTarget = getTargetBlockade();
		
		if(newTarget != null && newTarget.isPositionDefined()){
			Logger.info("Clearing blockade " + newTarget);
			
			List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(newTarget.getApexes()), true);
            double best = Double.MAX_VALUE;
            Point2D bestPoint = null;
            Point2D origin = new Point2D(me().getX(), me().getY());
            for (Line2D next : lines) {
                Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
                double d = GeometryTools2D.getDistance(origin, closest);
                if (d < best) {
                    best = d;
                    bestPoint = closest;
                }
            }
            Vector2D v = bestPoint.minus(new Point2D(me().getX(), me().getY()));
            v = v.normalised().scale(1000000);
            sendClear(time, (int) (me().getX() + v.getX()), (int) (me().getY() + v.getY()));
			return;
			//sendClear(time, newTarget.getID());
		}else{
			stateQueue.poll();
		}
    	
    }

    private Blockade getTargetBlockade() {
        Logger.debug("Looking for target blockade");
        Area location = (Area) location();
        Logger.debug("Looking in current location");
        Blockade result = getTargetBlockade(location, distance);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area) model.getEntity(next);
            result = getTargetBlockade(location, distance);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Blockade getTargetBlockade(Area area, int maxDistance) {
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        int x = me().getX();
        int y = me().getY();
        for (EntityID next : ids) {
            Blockade b = (Blockade) model.getEntity(next);
            double d = findDistanceTo(b, x, y);
            //            Logger.debug("Distance to " + b + " = " + d);
            if (maxDistance < 0 || d < maxDistance) {
                //                Logger.debug("In range");
                return b;
            }
        }
        //        Logger.debug("No blockades in range");
        return null;
    }

    private int findDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int) best;
    }

    /**
     * Get the blockade that is nearest this agent.
     *
     * @return The EntityID of the nearest blockade, or null if there are no
     * blockades in the agents current location.
     */
    /*
     public EntityID getNearestBlockade() {
     return getNearestBlockade((Area)location(), me().getX(), me().getY());
     }
     */
    /**
     * Get the blockade that is nearest a point.
     *
     * @param area The area to check.
     * @param x The X coordinate to look up.
     * @param y The X coordinate to look up.
     * @return The EntityID of the nearest blockade, or null if there are no
     * blockades in this area.
     */
    /*
     public EntityID getNearestBlockade(Area area, int x, int y) {
     double bestDistance = 0;
     EntityID best = null;
     Logger.debug("Finding nearest blockade");
     if (area.isBlockadesDefined()) {
     for (EntityID blockadeID : area.getBlockades()) {
     Logger.debug("Checking " + blockadeID);
     StandardEntity entity = model.getEntity(blockadeID);
     Logger.debug("Found " + entity);
     if (entity == null) {
     continue;
     }
     Pair<Integer, Integer> location = entity.getLocation(model);
     Logger.debug("Location: " + location);
     if (location == null) {
     continue;
     }
     double dx = location.first() - x;
     double dy = location.second() - y;
     double distance = Math.hypot(dx, dy);
     if (best == null || distance < bestDistance) {
     bestDistance = distance;
     best = entity.getID();
     }
     }
     }
     Logger.debug("Nearest blockade: " + best);
     return best;
     }
     */
    /*
     *
     * Métodos Definidos por nós
     * 
     */
    @Override
    public int somar(int x, int y) {
        return x + y;
    }

    @Override
    public int subtrair(int x, int y) {
        return x - y;
    }

    /*
     *
     * Métodos Acessores se necessário
     *
     * 
     */
    public int getResultado() {
        return resultado;
    }

    public void setResultado(int resultado) {
        this.resultado = resultado;
    }
}
