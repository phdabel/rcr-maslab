package agent;

import agent.interfaces.IPoliceForce;
import agent.worldmodel.*;
import agent.worldmodel.Object;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
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
        

        /**
         * localizar bloqueio
         */
        
	    
	    if(this.pathDefined == true && this.destiny != null)
	    {
	    	List<EntityID> currentPath = this.walk(me().getPosition());
	    	
	    	//posicao atual do agente
	    	Area myRoad = (Area)model.getEntity(me().getPosition());
	    	System.out.println("me position "+myRoad);
	    	System.out.println(("destiny "+currentPath.get(0)));
	    	Road nextRoad = (Road)model.getEntity(currentPath.get(0));
	    	if(myRoad.getStandardURN() == StandardEntityURN.ROAD && nextRoad.getStandardURN() == StandardEntityURN.ROAD)
	    	{
		    	ClosestPair cp = this.bestPoint(myRoad, nextRoad);
		    	Point2D p = cp.either();
		    	Pair<Rectangle2D, Pair<Integer, Integer>> targetCoor = this.policeAim(p);
		    	
		    	if( (myRoad.getBlockades() != null || myRoad.getBlockades().isEmpty() == false)
		    			||
		    		(nextRoad.getBlockades() != null || nextRoad.getBlockades().isEmpty() == false)
		    	
		    			)
		    	{
		    		double width = 0.0;
		    		double height = 0.0;
		    		double widthRua = myRoad.getShape().getBounds2D().getWidth();
		    		double heightRua = myRoad.getShape().getBounds2D().getHeight();
		    		int ct = 0;
		    		for(EntityID i : myRoad.getBlockades())
		    		{
		    			Blockade bloqueio = (Blockade)model.getEntity(i);
		    			width = bloqueio.getShape().getBounds2D().getWidth();
		    			height = bloqueio.getShape().getBounds2D().getHeight();
		    			if((width / widthRua) > 0.5 || (height / heightRua) > 0.5)
		    			{
		    				ct++;
		    			}
		    			
		    		}
		    		if(ct > 0){
			    		EntityID id = this.getNearestBlockade((Area)myRoad, targetCoor.second().first(), targetCoor.second().second());
			    		if(id != null){
			    			Blockade b = (Blockade)model.getEntity(id);
			    			this.currentTask = new Task(b.getID().getValue(), Object.BLOCKADE, b.getID().getValue());
			    		}else
			    		{
			    			double widthNextRoad = 0.0;
			    			double heightNextRoad = 0.0;
			    			int ctNext = 0;
			    			for(EntityID i: nextRoad.getBlockades())
			    			{
			    				Blockade bloqueio = (Blockade)model.getEntity(i);
			    				width = bloqueio.getShape().getBounds2D().getWidth();
			    				height = bloqueio.getShape().getBounds2D().getHeight();
			    				if((width / widthNextRoad) > 0.5 || (height / heightNextRoad) > 0.5)
			    				{
			    					ctNext++;
			    				}
			    			}
			    			if(ctNext > 0){
			    				EntityID nextRoadID = this.getNearestBlockade(nextRoad, me().getX(), me().getY());
				    			if(nextRoadID != null)
				    			{
				    				Blockade b = (Blockade)model.getEntity(nextRoadID);
				    				this.currentTask = new Task(b.getID().getValue(), Object.BLOCKADE, b.getID().getValue());
				    			}
			    			}
			    		}
		    		}
		    	}
	    	}
	    	
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
        		stateQueue.add(new AgentState("Unblock", new EntityID(this.currentTask.getId())));
        	}

        }else if (this.stateQueue.isEmpty()){
        	stateQueue.add(new AgentState("RandomWalk"));

        }
        
        //this.printQueue();

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
        		List<EntityID> path = this.walk(me().getPosition());
        		sendMove(time, path);
        		return;
        	case "Walk":
        		List<EntityID> pathWalk = this.walk(me().getPosition(), this.destiny);
        		sendMove(time, pathWalk);
        		return;
        	case "Unblock":
        		this.stateQueue.poll();
        		this.currentTask = null;
        		//Point2D alvo = new Point2D(currentAction.getX(), currentAction.getY());
        		Blockade alvo = (Blockade)model.getEntity(currentAction.getId());
        		this.shot(time, alvo);
        		
        		break;

        	}
        }

        /**
         * fim da máquina de estados
         */
        
        
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
        
    }
    
    public void shot(int timestep, Blockade target){
    	
    	List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(target.getApexes()), true);
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
        sendClear(timestep, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
    	//sendClear(timestep, (int)target.getX(), (int)target.getY());
	    return;
    }
    
    /**
     * 
     * @param target
     * @return
     */
    private Pair<Rectangle2D, Pair<Integer, Integer>> policeAim(Point2D target)
    {
    	Polygon mira = new Polygon();
    	mira.addPoint(me().getX(), me().getY());
    	
    	int x = me().getX() + this.policeAimPoint(target).first();
    	int y = me().getY() + this.policeAimPoint(target).second();
    	
    	mira.addPoint(x, y);
    	
    	
    	
    	Pair<Integer, Integer> pointTarget = new Pair<Integer, Integer>(x, y);
    	Pair<Rectangle2D, Pair<Integer, Integer>> result = new 
    			Pair<Rectangle2D, Pair<Integer, Integer>>((Rectangle2D)mira.getBounds2D(),pointTarget);
    	return result;
    }
    
    /**
     * função que retorna coordenadas X e Y do ponto de disparo de acordo com o MAX_RANGE (10000)
     * @param target
     * @return
     */
    private Pair<Integer, Integer> policeAimPoint(Point2D target)
    {
    	if(target != null){
    		
	    	double deltaX = (target.getX() - me().getX());
	    	double deltaY = (target.getY() - me().getY());
	    	double alfaX = deltaX / this.distance;
	    	double alfaY = deltaY / this.distance;
	    	double newX = deltaX / Math.abs(alfaX);
	    	double newY = deltaY / Math.abs(alfaY);
	    	
	    	return new Pair<Integer, Integer>((int)newX, (int)newY);
	    	
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
        
    
    private ClosestPair bestPoint(Area origin, Point2D destiny)
    {
    	Point2D[] destinyPoints = new Point2D[1];
    	destinyPoints[0] = destiny;
    	
    	List<Point2D> originAreaPoints = GeometryTools2D.vertexArrayToPoints(origin.getApexList());
    	Point2D[] originPoints = new Point2D[originAreaPoints.size()];
    	for(int i = 0; i < originAreaPoints.size(); i++)
    	{
    		destinyPoints[i] = originAreaPoints.get(i);
    	}
			
    	return this.bestPoint(originPoints, destinyPoints);
    }
    
    private ClosestPair bestDifferentPoint(Point2D origin, Area destiny)
    {
    	Point2D[] originPoints = new Point2D[1];
    	originPoints[0] = origin;
    	
    	List<Point2D> destinyAreaPoints = GeometryTools2D.vertexArrayToPoints(destiny.getApexList());
    	Point2D[] destinyPoints = new Point2D[destinyAreaPoints.size() - 1];
    	int ct = 0;
    	for(int i = 0; i < destinyAreaPoints.size(); i++)
    	{
    		
    		if(
    				(origin.getX() != destinyAreaPoints.get(i).getX())
    				||
    				(origin.getY() != destinyAreaPoints.get(i).getY())
    				){
    			
    			destinyPoints[ct] = destinyAreaPoints.get(i);
    			ct++;
    		}
    	}
			
    	return this.bestPoint(originPoints, destinyPoints);
    }
    
    private ClosestPair bestPoint(Point2D origin, Area destiny)
    {
    	Point2D[] originPoints = new Point2D[1];
    	originPoints[0] = origin;
    	
    	List<Point2D> destinyAreaPoints = GeometryTools2D.vertexArrayToPoints(destiny.getApexList());
    	Point2D[] destinyPoints = new Point2D[destinyAreaPoints.size()];
    	
    	for(int i = 0; i < destinyAreaPoints.size(); i++)
    	{    		
    		destinyPoints[i] = destinyAreaPoints.get(i);
    	}
			
    	return this.bestPoint(originPoints, destinyPoints);
    }
    
    /**
     * Função genérica para retornar o ponto mais próximo do próximo nó no caminho.
     * 
     * 
     * @param origin Area de origem ou nó atual do agente.
     * @param destiny Area de destino ou próximo nó no caminho..
     * @return Point2D ponto do nó atual que é mais próximo do próximo nó no caminho.
     */
    private ClosestPair bestPoint(Area origin, Area destiny)
    {
    	//proximo nó
    	List<Point2D> destinyAreaPoints = GeometryTools2D.vertexArrayToPoints(destiny.getApexList());
    	Point2D[] destinyPoints = new Point2D[destinyAreaPoints.size()];
    	for(int i = 0; i < destinyAreaPoints.size(); i++)
    	{
    		destinyPoints[i] = destinyAreaPoints.get(i);    		
    	}
    	
    	//nó atual
    	List<Point2D> originAreaPoints = GeometryTools2D.vertexArrayToPoints(origin.getApexList());
    	Point2D[] originPoints = new Point2D[originAreaPoints.size()];
    	for(int i = 0; i < originAreaPoints.size(); i++)
    	{
    		originPoints[i] = originAreaPoints.get(i);
    	}
    	
    	return this.bestPoint(originPoints, destinyPoints);
    }
    
    /**
     * Função bestPoint para listas de pontos de origem e destino
     * 
     * @param origin
     * @param destiny
     * @return
     */
    private ClosestPair bestPoint(Point2D[] origin, Point2D[] destiny)
    {
    	    	
    	ClosestPair c = new ClosestPair(origin, destiny);
    	
    	return c;
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
    
    public Blockade getNearestBlockade(){
    	EntityID nearest = this.getNearestBlockadeID();
    	
    	return (Blockade)model.getEntity(nearest);
    }
    
    
     public EntityID getNearestBlockadeID() {
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
    	if(area.isBlockadesDefined()){
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
