package agent;

import Exploration.Exploration;
import Exploration.WalkingInSector;
import agent.interfaces.IPoliceForce;

import java.io.EOFException;
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
import util.MASLABRouting;
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
	private Exploration exploracao = new Exploration(model);
	private int temObjetivo = 0; // define se o policial ja tem uma tarefa
	private int radioControl = 0; // define se existe algum pedido de socorro
	private List<EntityID> pathtoclean;
	private int maxView;
	private StandardEntity node; // node objetivo
	private int Setor;
	
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
		Setor = random.nextInt(6);
		//Setores.UNDEFINED_SECTOR;
		
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		
		
		Area location = (Area) location();
		search = new MASLABBFSearch(model);
		// Atualiza o ambiente ao redor do agente
		PerceberAmbiente(time, me().getPosition(model));
		exploracao = exploration;
		//exploracao.InsertNewInformation(time, me().getPosition(model), "000", 0, 0);		
		
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Sunbscribe to channel 1
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
		}

		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}

		

		// Verifica se existe um objetivo especifico
		if (temObjetivo == 1) {
			System.out.println("Atualizando Objetivo: "+ node);
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
			
					System.out.println("Verificando portas");
				    // caso exista uma porta onde estou e caso eu esteja no rumo do objetivo
					if ( (haveDoors(me().getPosition(), model) != null)){
						
						List<EntityID> doors = haveDoors(me().getPosition(),model);
						// verifica a lista de portas da rua
						for( EntityID dor : doors){
							// caso esteja bloqueada 
							StandardEntity auxdor = model.getEntity(dor);
							if(((Road) auxdor).isBlockadesDefined()){
								System.out.println("Porta bloqueada: "+dor);
								pathtoclean = routing.Explorar(me().getPosition(),Setor, Bloqueios, dor);
								sendMove(time, pathtoclean);
								node = auxdor;
								System.out.println("Novo Objetivo: Porta identificada ("+node.getID()+")");
								temObjetivo = 1;
							}
						}
					 }

				}
				//System.out.println("Me: "+me().getPosition()+ " Objetivo: "+ node.getID()) 
				if( me().getPosition().equals(node.getID()) )  
				{
					StandardEntity local = model.getEntity(me().getPosition());
					// Verifica se estou em uma road
					System.out.println("Alcancei o meu objetivo");
					// Tem uma merda aqui --- quando entra num edificio 
					try {
					
					if(((Road) local).getStandardURN().equals(StandardEntityURN.ROAD)){
					
						System.out.println("é uma Rua o/");
						// Verifica se n existe bloqueio 
						if (!((Road) model.getEntity(me().getPosition())).isBlockadesDefined()){
							System.out.println("não Existe mais nada para limpar aqui o/");
							temObjetivo = 0;	
						}
					
						else{
							// Quando ele cai numa porta ele da merda
							//System.out.println("Merda ......");
							temObjetivo = 0;
						}
					}
					} catch (Exception e) {
						temObjetivo = 0;
					}
				}
				// Continua se movendo
				sendMove(time, routing.Explorar(me().getPosition(),Setor, Bloqueios, node.getID()));
				return;
			}

		} else {
			
			// Verifica se existe uma rua explorada e que esteja bloqueada
			System.out.println("Nenhum objetivo .... Verificando Nodes Explorados");
			if(exploracao.GetBlockRoads(exploration.Exploracao)!=null){
				// checar se essa rua esta sob a rota de responsabilidade do agente
				for(StandardEntity road: exploracao.GetBlockRoads(exploration.Exploracao)){
					if( model.getEntity(road.getID()) != null){
						//isGoal(road.getID(), pathtoclean)){
						node = road;
						System.out.println("Resultado exploração:"+node.getID());
						pathtoclean = routing.Explorar(me().getPosition(),Setor, Bloqueios, node.getID());
						sendMove(time, pathtoclean);
						temObjetivo = 1;
						return;
					}
					}
				}
			
			
			
			System.out.println("Iniciando nova exploração...");
			
			// senão obter nova rota para explorar/limpar
			WalkingInSector walking = new WalkingInSector(model);
			Map<EntityID, Set<EntityID>> mapa = sectoring.MapSetor2;
			mapa = search.getGraph();
			//System.out.println("Objetivo: "+mapa.values());
		
			node = walking.GetExplorationNode(time, me().getPosition(model).getID(), mapa, exploracao.GetExplorationNodes(exploration.Exploracao),0);
			
			if (node == null){
				System.out.println("Roleta....");
				node  = exploracao.GetNewExplorationNode(time,exploracao.Exploracao,0);
			}
			//System.out.println("Posicao Atual: "+ me().getPosition()+" Objetivo: "+ node.getID());
			
			pathtoclean = routing.Explorar(me().getPosition(),Setor, Bloqueios, node.getID());
					
			//Mover(me().getID(),Setor, node.getID());
			//System.out.println(" indo por : "+pathtoclean);
			
			//gotoDestino(me().getID(), node.getID(), Bloqueios, false, 0);
			//routing.Explorar();
			
			//System.out.println("Iniciando exploração: " + node.getID().toString());
			temObjetivo = 1;
			sendMove(time, pathtoclean);
			System.out.println("Novo Objetivo: "+ node);
			return;
			
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
	 *  retorna todos os vizinhos de um buildings que sejam roads
	 *  supõe-se que um vizinho de um building que seja road é uma porta,
	 *  então este é adicionado a lista e retornado
	 * 
	 * @param e EntityID
	 * @param world WorldModel
	 * @return Doors List
	 */
	private List<EntityID> haveDoors(EntityID e, StandardWorldModel world)
	{
		List<EntityID> doors = new ArrayList<EntityID>();
		StandardEntity entity = world.getEntity(e);
		//se o no do parametro é road
		if(entity.getStandardURN().equals(StandardEntityURN.ROAD))
		{
			Road r = (Road)entity;
			//procura portas entre cada vizinho
			for(EntityID n : r.getNeighbours())
			{
				StandardEntity _entity = world.getEntity(n);
				//se o vizinho é uma building
				if(_entity.getStandardURN().equals(StandardEntityURN.BUILDING))
				{
					
					//adiciona o nó do parâmetro e o retorna, pois é uma porta
					//doors.add(n);
					doors.add(r.getID());
					// return doors;
					//senão, se o vizinho é road
				}else if(_entity.getStandardURN().equals(StandardEntityURN.ROAD)){
					Road r2 = (Road)_entity;
					for(EntityID n2 : r2.getNeighbours())
					{
						StandardEntity _entity2 = world.getEntity(n2);
						if(_entity2.getStandardURN().equals(StandardEntityURN.BUILDING))
						{
							//doors.add(n2);
							doors.add(r2.getID());
						}
					}
				}
			}
		}
		return doors;
	}
	
	
}
