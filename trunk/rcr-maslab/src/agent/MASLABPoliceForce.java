package agent;

import Exploration.Exploration;
import Exploration.WalkingInSector;
import agent.interfaces.IPoliceForce;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import firesimulator.world.Civilian;
import rescuecore.objects.World;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import util.Channel;
import util.MASLABBFSearch;
import util.MASLABSectoring;
import util.MSGType;
import util.Setores;

/**
 * A sample police force agent.
 */
public class MASLABPoliceForce extends MASLABAbstractAgent<PoliceForce>
		implements IPoliceForce {

	/**
	 * 
	 * Variaveis Sample Agent
	 * 
	 */
	private static final String DISTANCE_KEY = "clear.repair.distance";
	public static final String MAX_VIEW_KEY = "perception.los.max-distance";
	
	private int distance;
	private Exploration exploracao = new Exploration(model);;
	private int temObjetivo = 0; // define se o policial ja tem uma tarefa
	private int radioControl = 0; // define se existe algum pedido de socorro
	private List<EntityID> pathtoclean;
	private int maxView;
	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */
	private int resultado;

	/*
	 * 
	 * Métodos Standard Agent
	 */

	public MASLABPoliceForce(int pp) {
		super(pp);
	}

	@Override
	public String toString() {
		return "MASLAB police force";
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.ROAD);
		maxView = config.getIntValue(MAX_VIEW_KEY);
		distance = config.getIntValue(DISTANCE_KEY);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		Area location = (Area) location();
		search = new MASLABBFSearch(model);
		
		
		//System.out.println(exploracao.Exploracao.toString());
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Sunbscribe to channel 1
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
		}

		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}

		// Atualiza o node onde o agente está
		

		
		LookAround(time);
		System.out.println("nodes explorados:" +exploracao.Exploracao.toString());
		exploracao.InsertNewInformation(time,model.getEntity(me().getPosition()), "000", 0, 0);
		// Verifica se existe um objetivo especifico
		if (temObjetivo == 1) {
			// Caso exista um chamado
			if (radioControl == 1) {

			}
			// Continuar tarefa priori de alocação
			else {
				// Am I near a blockade?
				Blockade target = getTargetBlockade(time);
				if (target != null) {
				// Verifica se o bloqueio se encontra sob a rota de limpeza ou onde eu estou
				if (isGoal(target.getPosition(), pathtoclean) || getTargetBlockade(location, distance)!= null) {
						System.out.println("Existe um bloqueio onde estou *.*");
						// Caso afirmativo limpar
						Logger.info("Clearing blockade " + target);
						sendSpeak(time, 1, ("Clearing " + target).getBytes());
						// sendClear(time, target.getX(), target.getY());
						List<Line2D> lines = GeometryTools2D.pointsToLines(
								GeometryTools2D.vertexArrayToPoints(target
										.getApexes()), true);
						double best = Double.MAX_VALUE;
						Point2D bestPoint = null;
						Point2D origin = new Point2D(me().getX(), me().getY());
						for (Line2D next : lines) {
							Point2D closest = GeometryTools2D
									.getClosestPointOnSegment(next, origin);
							double d = GeometryTools2D.getDistance(origin,
									closest);
							if (d < best) {
								best = d;
								bestPoint = closest;
							}
						}
						Vector2D v = bestPoint.minus(new Point2D(me().getX(),
								me().getY()));
						v = v.normalised().scale(1000000);
						sendClear(time, (int) (me().getX() + v.getX()),
								(int) (me().getY() + v.getY()));
						return;

					}
					 if (haveDoors(me().getPosition(), model) != null){
						 
					 }

				}
				//System.out.println("Target: "+target);
				System.out.println("Estou longe do bloqueio");
				temObjetivo = 0;
				

			}

		} else {
			
			// senão obter nova rota para explorar/limpar
			WalkingInSector walking = new WalkingInSector(model);
			Map<EntityID, Set<EntityID>> mapa = sectoring.MapSetor2;
			mapa = search.getGraph();
			
			//System.out.println("Objetivo: "+ mapa.values().toString());
			
			StandardEntity node = walking.GetExplorationNode(time, me().getPosition(model).getID(), mapa, exploracao.GetExplorationNodes(), exploracao.GetNewExplorationNode(time),0);
			pathtoclean = routing.Explorar(me().getPosition(),
					Setores.UNDEFINED_SECTOR, Bloqueios, node.getID());

			System.out.println("Iniciando exploração: " + pathtoclean);
			temObjetivo = 1;
			sendMove(time, pathtoclean);
			
		}
		

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
		Collection<StandardEntity> e = model
				.getEntitiesOfType(StandardEntityURN.ROAD);
		List<EntityID> result = new ArrayList<EntityID>();
		for (StandardEntity next : e) {
			Road r = (Road) next;
			if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
				result.add(r.getID());
			}
		}
		return result;
	}

	private Blockade getTargetBlockade(int time) {
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
		// Logger.debug("Looking for nearest blockade in " + area);
		if (area == null || !area.isBlockadesDefined()) {
			// Logger.debug("Blockades undefined");
			return null;
		}
		List<EntityID> ids = area.getBlockades();
		// Find the first blockade that is in range.
		int x = me().getX();
		int y = me().getY();
		for (EntityID next : ids) {
			Blockade b = (Blockade) model.getEntity(next);
			double d = findDistanceTo(b, x, y);
			// Logger.debug("Distance to " + b + " = " + d);
			if (maxDistance < 0 || d < maxDistance) {
				// Logger.debug("In range");
				return b;
			}
		}
		// Logger.debug("No blockades in range");
		return null;
	}

	private int findDistanceTo(Blockade b, int x, int y) {
		// Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
		List<Line2D> lines = GeometryTools2D.pointsToLines(
				GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
		double best = Double.MAX_VALUE;
		Point2D origin = new Point2D(x, y);
		for (Line2D next : lines) {
			Point2D closest = GeometryTools2D.getClosestPointOnSegment(next,
					origin);
			double d = GeometryTools2D.getDistance(origin, closest);
			// Logger.debug("Next line: " + next + ", closest point: " + closest
			// + ", distance: " + d);
			if (d < best) {
				best = d;
				// Logger.debug("New best distance");
			}

		}
		return (int) best;
	}

	/**
	 * Get the blockade that is nearest this agent.
	 * 
	 * @return The EntityID of the nearest blockade, or null if there are no
	 *         blockades in the agents current location.
	 */
	/*
	 * public EntityID getNearestBlockade() { return
	 * getNearestBlockade((Area)location(), me().getX(), me().getY()); }
	 */
	/**
	 * Get the blockade that is nearest a point.
	 * 
	 * @param area
	 *            The area to check.
	 * @param x
	 *            The X coordinate to look up.
	 * @param y
	 *            The X coordinate to look up.
	 * @return The EntityID of the nearest blockade, or null if there are no
	 *         blockades in this area.
	 */
	/*
	 * public EntityID getNearestBlockade(Area area, int x, int y) { double
	 * bestDistance = 0; EntityID best = null;
	 * Logger.debug("Finding nearest blockade"); if (area.isBlockadesDefined())
	 * { for (EntityID blockadeID : area.getBlockades()) {
	 * Logger.debug("Checking " + blockadeID); StandardEntity entity =
	 * model.getEntity(blockadeID); Logger.debug("Found " + entity); if (entity
	 * == null) { continue; } Pair<Integer, Integer> location =
	 * entity.getLocation(model); Logger.debug("Location: " + location); if
	 * (location == null) { continue; } double dx = location.first() - x; double
	 * dy = location.second() - y; double distance = Math.hypot(dx, dy); if
	 * (best == null || distance < bestDistance) { bestDistance = distance; best
	 * = entity.getID(); } } } Logger.debug("Nearest blockade: " + best); return
	 * best; }
	 */
	/*
	 * 
	 * Métodos Definidos por nós
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
	 */
	public int getResultado() {
		return resultado;
	}

	public void setResultado(int resultado) {
		this.resultado = resultado;
	}

	private boolean isGoal(EntityID e, List<EntityID> nodesconhecidos2) {
		if (nodesconhecidos2.contains(e) && getBlockedRoads().contains(e))
		{return true;}
		return false;
				//nodesconhecidos2.contains(e);
	}

	/**
	 * Método que identifica as portas pertencentes a uma determinada road
	 * 
	 * @param EntityID
	 *            - road
	 * @return List<EntityID> doors
	 */

	private void LookAround(int time){
		
		// civis .substring(0, 1) incendio .substring(1, 2) bloqueio .substring(2, 3)
		Collection<StandardEntity> Cenario = model.getObjectsInRange(me().getID(), maxView);
		
		for(StandardEntity objeto_cenario: Cenario){
			//System.out.println(objeto_cenario.getID()+ " : "+ objeto_cenario.toString()+ " "+objeto_cenario.getProperties().toString());
			if(!objeto_cenario.getProperties().toString().contains("urn:rescuecore2.standard:property:temperature (undefined)")
					&& 
					!objeto_cenario.getProperties().toString().contains("urn:rescuecore2.standard:property:blockades (undefined)"))
			{
				
				
				
				String problem = "000";
			
				// caso seja um edificio
			if(model.getEntitiesOfType(StandardEntityURN.BUILDING).contains(objeto_cenario)){
				
				Building predio = new Building(objeto_cenario.getID());

				if ( predio.getTemperature()> 10){
					problem = problem.substring(0, 1)+"1"+problem.substring(2,3);
					//exploracao.InsertNewInformation(time, objeto_cenario, "010", 0, 0);
				}
			}
			
			// caso seja um outro policial
			if(model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).contains(objeto_cenario)){
				// Outro policial
				if(me().getID() != objeto_cenario.getID()){
					// Comunicação aqui
				}	
			}
			
			if( model.getEntitiesOfType(StandardEntityURN.POLICE_FORCE).contains(objeto_cenario) ||
				model.getEntitiesOfType(StandardEntityURN.CIVILIAN).contains(objeto_cenario) ||
				model.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM).contains(objeto_cenario) ||
				model.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).contains(objeto_cenario)){
				Human h = (Human) objeto_cenario;
				if (h == me()) {
					continue;
				}
				if (h.isHPDefined() && h.isBuriednessDefined()
						&& h.isDamageDefined() && h.isPositionDefined()
						&& h.getHP() > 0
						&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
					problem = "1"+problem.substring(1, 2)+problem.substring(2,3);
				}		
			}
			// caso seja uma road
			if(model.getEntitiesOfType(StandardEntityURN.ROAD).contains(objeto_cenario)){
				Road road = new Road(objeto_cenario.getID());
				
				if(road.isBlockadesDefined()){
					problem = problem.substring(0, 1)+problem.substring(1,2)+"1";
				}
			}else{
				exploracao.InsertNewInformation(time, objeto_cenario, problem, 0, 0);
			}
			
			if(!problem.equals("000")){
				System.out.println(problem+" o/:"+objeto_cenario);
			}
			}
		}
		
	}
	private List<EntityID> haveDoors(EntityID road, StandardWorldModel world) {
				 
		return null;
	}
}
