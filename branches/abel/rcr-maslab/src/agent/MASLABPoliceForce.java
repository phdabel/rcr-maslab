package agent;

import agent.interfaces.IPoliceForce;
import agent.worldmodel.*;
import agent.worldmodel.Object;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Vector2D;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;
import sample.DistanceSorter;
import util.MASLAB.ClosestPair;

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
    //largura necessária paraum gente passar
    private static final int WIDTH_FOR_PASS_THROUGH = 2500;
    //range maximo para o disparo do policial
    private static final int MAX_RANGE = 10000;
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
        
        if(this.currentPath.isEmpty() && this.pathDefined == true){
        	this.stateQueue.poll();
        }


        /*if(!this.currentPath.isEmpty())
        	this.bestPoint();*/
        //Boolean target = getClosestBlockade();
        /*if(target)
        {
        	double _px, _py;
        	Rectangle2D p = this.policeAim(this.bestPoint());
        	if(p.getMaxX() != me().getX())
        	{
        		_px = p.getMaxX();
        	}else{
        		_px = p.getMinX();
        	}
        	
        	if(p.getMaxY() != me().getY())
        	{
        		_py = p.getMaxY();
        	}else{
        		_py = p.getMinY();
        	}
        	sendClear(time, (int)_px, (int)_py);
        	return;
        }*/

        /**
         * localizar bloqueio
         */
        
	     Blockade target = (Blockade)model.getEntity(this.getNearestBlockade());

	     
	     if( (target != null &&
	    		 (((Road)model.getEntity(target.getPosition())).getShape().getBounds2D().getWidth() -
	    		 target.getShape().getBounds2D().getWidth()) < 2000
	    		 )  )
	     {
		     Area local = (Area)model.getEntity(me().getPosition());
		     this.currentTask = new Task(target.getID().getValue(), Object.BLOCKADE, target.getPosition().getValue());
		        
		        
	     }

        /**
         * && this.stateQueue.isEmpty()
         * INSERÇÃO DE TAREFAS NA FILA
         */
        if(this.currentTask != null && this.currentTask.getObject() == Object.BLOCKADE)
        {
        	
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk"){
        		stateQueue.remove(stateQueue.peek());
        	}else if(stateQueue.isEmpty()){
        		Task tmp = this.currentTask;
        		EntityID tmpID = new EntityID(tmp.getPosition());

        		if(me().getPosition() != tmpID)
        		{
        			
        			stateQueue.add(new AgentState("Walk", tmpID));
        			stateQueue.add(new AgentState("Unblock", new EntityID(tmp.getId())));        		
        		}else{
        			
        			stateQueue.add(new AgentState("Unblock", new EntityID(tmp.getId())));

        		}
        	}

        }else if (this.stateQueue.isEmpty()){
        	stateQueue.add(new AgentState("RandomWalk"));

        }
        
        System.out.println("Caminho do agente: "+this.currentPath);
        System.out.println("Posicao Atual: "+me().getPosition());
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
        		if(this.currentPath.isEmpty() && this.pathDefined == false)
        		{

        			this.currentPath = this.walk(this.currentPath, me().getPosition());
        			this.pathDefined = true;

        			sendMove(time, this.currentPath);
        			return;
        		}else if(this.pathDefined == true){
        			
        			this.lastPath = this.currentPath;
        			this.currentPath = this.walk(this.currentPath, me().getPosition());
        			sendMove(time, this.currentPath);
        			return;
        		}
        		break;

        	case "Walk":
        		
        		if(currentPath.isEmpty() && this.pathDefined == false){
        			pathDefined = true;
        			this.currentPath = search.breadthFirstSearch(me().getPosition(), new EntityID(this.currentTask.getPosition()));
        			sendMove(time, currentPath);
        			return;
        		
        		}else if(this.pathDefined == true){
        			this.currentPath = this.walk(this.currentPath, me().getPosition());
        			sendMove(time, currentPath);
        			return;
        		}

        		break;

        	case "Unblock":
        		this.stateQueue.poll();
        		this.shot(time);
        		/*
        		 * Blockade _target = (Blockade)model.getEntity(new EntityID(this.currentTask.getId()));
        		 */
        		
        		
        		/*Blockade _target = (Blockade)model.getEntity(new EntityID(this.currentTask.getId()));
        		if (_target != null) {

        			sendSpeak(time, 1, ("Clearing " + _target).getBytes());
        			this.stateQueue.poll();
        			
        			List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(_target.getApexes()), true);
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
                    sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
                    return;*/   			
        			/*

        			if(!this.currentPath.isEmpty()){
        				Area nextNode = (Area)model.getEntity(this.currentPath.get(0));
        				List<Point2D> nextNodeInPath = GeometryTools2D.vertexArrayToPoints(nextNode.getApexList());

        				Area currentNode = (Area)model.getEntity(me().getPosition());
        				List<Point2D> currentNodeInPath = GeometryTools2D.vertexArrayToPoints(currentNode.getApexList());

        				Point2D[] origin = new Point2D[currentNodeInPath.size()]; 
        				Point2D[] destiny = new Point2D[nextNodeInPath.size()];
        				for(int i = 0; i < currentNodeInPath.size(); i++)
        				{
        					origin[i] = currentNodeInPath.get(i);
        				}

        				for(int i = 0; i < nextNodeInPath.size(); i++)
        				{
        					destiny[i] = nextNodeInPath.get(i);
        				}


        				ClosestPair c = new ClosestPair(origin, destiny);
        				Point2D bestPoint = c.either();
        				

        			}*/

        		//}else{
        		//	this.stateQueue.poll();
        		//}
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
    
    public void shot(int timestep){
    	double _px, _py;
    	if(this.bestPoint() != null){
	    	Rectangle2D p = this.policeAim(this.bestPoint());
	    	if(p.getMaxX() != me().getX())
	    	{
	    		_px = p.getMaxX();
	    	}else{
	    		_px = p.getMinX();
	    	}
	    	
	    	if(p.getMaxY() != me().getY())
	    	{
	    		_py = p.getMaxY();
	    	}else{
	    		_py = p.getMinY();
	    	}
	    	sendClear(timestep, (int)(me().getX()+_px), (int)(me().getY()+_py));
	    	return;
    	}
    }
    
    private Rectangle2D humanBlockade(Human h, int range)
    {
    	return this.pointBlockade(h.getX(), h.getY(), range);
    }
    
    private Rectangle2D pointBlockade(int x, int y, int range){
    	
    	Polygon poly = new Polygon();
    	
    	poly.addPoint(x+range, y);
    	poly.addPoint(x+range, y-range);
    	poly.addPoint(x, y-range);
    	poly.addPoint(x-range, y-range);
    	poly.addPoint(x-range, y);
    	poly.addPoint(x-range, y+range);
    	poly.addPoint(x, y+range);
    	poly.addPoint(x+range, y+range);
    	
    	return poly.getBounds2D();
    	
    }
    
    private Rectangle2D policeAim(Point2D target)
    {
    	if(target != null){
	    	Polygon mira = new Polygon();
	    	double divX = target.getX() / 10000;
	    	double divY = target.getY() / 10000;
	    	double newX = target.getX() / Math.abs(divX);
	    	double newY = target.getY() / Math.abs(divY);
	    	mira.addPoint(me().getX(), me().getY());
	    	mira.addPoint((int)newX, (int)newY);
	    	
	    	return mira.getBounds2D();
    	}else{
    		return null;
    	}
    	
    }
    
    private List<Human> getHumans() {
        List<Human> targets = new ArrayList<Human>();
        Polygon poly = new Polygon();
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, 
        		StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
            Human h = (Human)next;
            if (h == me()) {
                continue;
            }
            Area a;
            if (h.isHPDefined()
                && h.getHP() > 0
                && (h.getBuriedness() > 0 || h.getDamage() > 0)
                
                ) {
            	
                targets.add(h);
            }
        }
        Collections.sort(targets, new DistanceSorter(location(), model));
        return targets;
    }
    
    private Point2D bestPoint()
    {
    	Point2D bestPoint = null;
    	
    	if(!this.currentPath.isEmpty())
		{
    		//proximo nó
			Area nextNode = (Area) model.getEntity(this.currentPath.get(0));
			List<Point2D> nextNodeInPath = GeometryTools2D.vertexArrayToPoints(nextNode.getApexList());
			Point2D[] destiny = new Point2D[nextNodeInPath.size()];
			
			//nó atual
			Area currentNode = (Area) model.getEntity(me().getPosition());
			List<Point2D> currentNodeInPath = GeometryTools2D.vertexArrayToPoints(currentNode.getApexList());
			Point2D[] origin = new Point2D[currentNodeInPath.size()];
			
			for(int i = 0; i < currentNodeInPath.size(); i++)
			{
				origin[i] = currentNodeInPath.get(i);
			}
			
			for(int i = 0; i < nextNodeInPath.size(); i++)
			{
				destiny[i] = nextNodeInPath.get(i); 
			}
			
			ClosestPair c = new ClosestPair(origin, destiny);
        	bestPoint = (Point2D)c.either();
        	
		}
    	return bestPoint;
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
            Road r = (Road)next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                result.add(r.getID());
            }
        }
        return result;
    }

    private Blockade getTargetBlockade() {
        Logger.debug("Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("Looking in current location");
        Blockade result = getTargetBlockade(location, distance);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = getTargetBlockade(location, distance);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    private Boolean getClosestBlockade(){

    	Logger.debug("Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("Looking in current location");
        Boolean result = getClosestBlockade(location, distance);
        if (result != false) {
            return true;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = getClosestBlockade(location, distance);
            if (result != false) {
                return true;
            }
        }
        return false;
    	
    }
    
    private Boolean getClosestBlockade(Area area, int maxDistance)
    {
    	if(area == null || !area.isBlockadesDefined())
    	{
    		return false;
    	}
    	List<EntityID> ids = area.getBlockades();
    	int x = me().getX();
    	int y = me().getY();
    	for(EntityID next : ids){
    		Blockade b = (Blockade)model.getEntity(next);
    		List<Point2D> pointsOfBlockade = GeometryTools2D.vertexArrayToPoints(b.getApexes());
    		for(Point2D p : pointsOfBlockade)
    		{
    			
    			if(this.bestPoint() != null && this.policeAim(this.bestPoint()).contains(p.getX(), p.getY()))
    			{
    				return true;
    			}
    		}
    	}
    	return false;
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
            Blockade b = (Blockade)model.getEntity(next);
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
        return (int)best;
    }

    /**
     * Get the blockade that is nearest this agent.
     *
     * @return The EntityID of the nearest blockade, or null if there are no
     * blockades in the agents current location.
     */
    
    
     public EntityID getNearestBlockade() {
    	 return getNearestBlockade((Area)location(), me().getX(), me().getY());
     }
     
    /**
     * Get the blockade that is nearest a point.
     *
     * @param area The area to check.
     * @param x The X coordinate to look up.
     * @param y The X coordinate to look up.
     * @return The EntityID of the nearest blockade, or null if there are no
     * blockades in this area.
     */
    
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
