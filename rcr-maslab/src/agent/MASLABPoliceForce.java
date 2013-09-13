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
        
	    
	    if(this.pathDefined && this.destiny != null)
	    {
	    	List<EntityID> currentPath = this.walk(me().getPosition());
	    	
	    	Point2D mePos = new Point2D(me().getX(), me().getY());
	    	Point2D p = this.bestPoint(mePos, (Area)model.getEntity(currentPath.get(0)));
	    	if(p!=null)
	    	{
	    		Pair<Rectangle2D, Pair<Integer, Integer>> targetCoor = this.policeAim(p);
	    		Collection<StandardEntity> obj = model.getObjectsInRectangle(me().getX(), me().getY(), targetCoor.second().first(), targetCoor.second().second());
	    		System.out.println("qtd objetos "+obj.size());
	    		if(!obj.isEmpty())
	    		{
		    		for(StandardEntity o : obj)
		    		{
		    			System.out.println(o.getStandardURN());
		    			if(o.getStandardURN() == StandardEntityURN.ROAD)
		    			{
		    				if(((Road)o).isBlockadesDefined()){
		    					this.currentTask = new Task(o.getID().getValue(), Object.BLOCKADE, targetCoor.second().first(), targetCoor.second().second());
		    				}
		    			}
		    		}
	    		}
	    		
	    		/*
	    		List<Point2D> pTarget = GeometryTools2D.vertexArrayToPoints(target.getApexes());
	    		for(Point2D pT : pTarget)
	    		{
	    			if(targetCoor.first().getBounds2D().contains(pT.getX(), pT.getY()))
	    			{
	    				System.out.println("retangulo contem bloqueio");
	    				this.currentTask = new Task(target.getID().getValue(), Object.BLOCKADE, targetCoor.second().first(), targetCoor.second().second());
	    			}
	    		}*/
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
        		stateQueue.add(new AgentState("Unblock", this.currentTask.getX(), this.currentTask.getY()));
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
        		Point2D alvo = new Point2D(currentAction.getX(), currentAction.getY());
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
    
    public void shot(int timestep, Point2D target){
    	
    	Pair<Rectangle2D, Pair<Integer, Integer>> p = this.policeAim(target);
	    sendClear(timestep, (int)(me().getX()+p.second().first()), (int)(me().getY()+p.second().second()));
	    return;
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
    
    /**
     * 
     * @param target
     * @return
     */
    private Pair<Rectangle2D, Pair<Integer, Integer>> policeAim(Point2D target)
    {
    	Polygon mira = new Polygon();
    	mira.addPoint(me().getX(), me().getY());
    	int x = this.policeAimPoint(target).first();
    	int y = this.policeAimPoint(target).second();
    	int width = 100;
    	mira.addPoint(x, y);
    	/*
    	mira.addPoint(x+width, y+width);
    	mira.addPoint(x-width, y-width);
    	mira.addPoint(me().getX()+width, me().getY()+width);
    	mira.addPoint(me().getX()-width, me().getY()-width);*/
    	
    	
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
	    	double alfaX = deltaX / this.MAX_RANGE;
	    	double alfaY = deltaY / this.MAX_RANGE;
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
        
    
    private Point2D bestPoint(Area origin, Point2D destiny)
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
    
    private Point2D bestPoint(Point2D origin, Area destiny)
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
    private Point2D bestPoint(Area origin, Area destiny)
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
    private Point2D bestPoint(Point2D[] origin, Point2D[] destiny)
    {
    	Point2D bestPoint = null;
    	    	
    	ClosestPair c = new ClosestPair(origin, destiny);
    	bestPoint = (Point2D)c.either();
    	
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
