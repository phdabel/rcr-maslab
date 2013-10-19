package agent;

import Exploration.Exploration;
import Exploration.ExplorationWithComunication;
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

import model.AbstractMessage;
import model.BurningBuilding;
import firesimulator.world.Civilian;
import gis2.scenario.ClearAllFunction;
import rescuecore.objects.World;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.misc.geometry.spatialindex.BBTree;
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
	private List<EntityID> pathtoclean = new ArrayList<EntityID>();;
	private StandardEntity node; // node objetivo
	private int Setor = 6;
	private int objetivoSetor = 5; // Variavel auxiliar que define o objetivo do
	private int PortaBloqueada = 0;
	private String MSG_SEPARATOR = "-";
	private String MSG_FIM = ",";
	private List<EntityID> ObrigacoesSoterramento = new ArrayList<EntityID>();; // lista
																				// de
																				// chamado
																				// do
																				// setor
	private List<EntityID> ComunicarIncendios = new ArrayList<EntityID>();
	private int controletempoParado=0;
	private int controletempoParadoTotal=0;
	private EntityID PosicaoPassada;
	private List<EntityID> MensageActivites = new ArrayList<EntityID>();
	private int ocupado = 0;
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
		AbstractMessage m = new AbstractMessage();
		MSG_SEPARATOR = m.getMSG_SEPARATOR();
		MSG_FIM = m.getMSG_FIM();
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
		setSector();
		// Setores.UNDEFINED_SECTOR;
		System.out.println("PF on!");

	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		//System.out.println("EU SOU: "+me().getID().getValue());
// INICIO CONTROLE DA COMUNICAÇÃO / PERCEPÇÃO DO AMMBIENTE --------------------------------------------------------------------# 
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
		}

		
		search = new MASLABBFSearch(model);
		// Atualiza o ambiente ao redor do agente
		PerceberAmbiente(time, me().getPosition(model));

		if(time==1){
			PosicaoPassada = me().getPosition();
		}

		// TODO - Enviar Mensagens
		List<AbstractMessage> m = new ArrayList<AbstractMessage>();
		for (BurningBuilding bb : IncendiosComunicar) {
			m.add(new AbstractMessage(String.valueOf(MSGType.BURNING_BUILDING
					.ordinal()), String.valueOf(bb.getID()), String.valueOf(bb
					.getTempoAtual()), String.valueOf(bb.getImportancia())));
			ComunicarIncendios.add(new EntityID(bb.getID()));
		}
		if (time > 4)
			IncendiosComunicar.clear();
		
		if (m.size() > 0) {
			sendMessage(MSGType.BURNING_BUILDING, true, time, m);
		}
		
		// Verifica se existe policiais ao redor
		ProcurarPolicias(time, me().getPosition(model));

		// Receber Mensagens
		List<String> msgs = heardMessage(heard);
		List<String> mensagens = new ArrayList<String>();
		for (String s : msgs) {
			String x[] = s.split(MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(MSG_FIM)));
		}

		// Armazena as informações recebidas

		for (String s : mensagens) {
			List<String> msg = Arrays.asList(s.split(MSG_SEPARATOR));
			Boolean bAdd = true;
			BurningBuilding alvoAux = null;
			// Caso seja uma mensagem por radio
			if (msg.size() == 2) {
				// Se for um infeliz bloqueado adicionar a lista
				if (Integer.parseInt(msg.get(0)) == MSGType.UNBLOCK_ME
						.ordinal()) {
					if (!ObrigacoesSoterramento.contains(new EntityID(Integer
							.parseInt(msg.get(1))))) {
						ObrigacoesSoterramento.add(new EntityID(Integer.parseInt(msg
								.get(1))));
						//System.out.println("NOVA CHAMADA :  "+msg.get(1) +" ------------------------------------");
					}
				}
			}
			
			// Caso seja uma mensagem por voz
			if (msg.size() == 3) {
				if (Integer.parseInt(msg.get(0)) == MSGType.UNBLOCK_ME
						.ordinal()) {
					// verificar se o objetivo é o mesmo
					if (node.equals(new EntityID(Integer.parseInt(msg.get(1))))) {
						// Menor id permanece com a tarefa
						if (Integer.parseInt(me().getID().toString()) > Integer
								.parseInt(new EntityID(Integer.parseInt(msg
										.get(2))).toString())) {
							try {
								MensageActivites.remove(new EntityID(Integer.parseInt(msg.get(1))));
								ObrigacoesSoterramento.remove(new EntityID(Integer.parseInt(msg.get(1))));
								ocupado = 0;
							} catch (Exception e) {
							}
						}
					}
				}
			}
		}
// FIM CONTROLE DA COMUNICAÇÃO / PERCEPÇÃO DO AMMBIENTE  --------------------------------------------------------------------# 
	 
// INICIO DA VERIFICAÇÃO DA CONCLUSAO DA TAREFA -----------------------------------------------------------------------------#
		// Verifica se cheguei ao meu objetivo (NODE
		//System.out.println(node);
		if(node != null ){
		if (me().getPosition().equals(node.getID())) {
			//System.out.println("Estou sob meu Objetivo");
			StandardEntity local = model.getEntity(me().getPosition());
			try {
				// Verifica se estou em uma road
				if (((Road) local).getStandardURN().equals(StandardEntityURN.ROAD)) {
					// Verifica se n existe bloqueio
					if (!((Road) model.getEntity(me().getPosition())).isBlockadesDefined() && ((Road) model.getEntity(me().getPosition())).getBlockades().isEmpty() ) {
							// Remove a tarefa da lista de busca  e fica livre
						if(ObrigacoesSoterramento.contains(me().getPosition())){
							ObrigacoesSoterramento.remove(me().getPosition());
							}
						//System.out.println("Terminei minha tarefa");
							ocupado = 0;
					}

				}
				// se n for uma rua então n tenho nada para fazer aqui- sou "PULISÍA MANO"
				else{
					if(ObrigacoesSoterramento.contains(me().getPosition())){
					
					ObrigacoesSoterramento.remove(me().getPosition());
					}
					//System.out.println("Terminei minha tarefa");
					ocupado = 0;
				}
			} catch (Exception e){
				ocupado = 0;
			}
			
			ObrigacoesSoterramento.remove(me().getPosition());
		}
		}
		
// FIM DA VERIFICAÇÃO DA CONCLUSAO DA TAREFA -----------------------------------------------------------------------------#

// INICIO DA REALIZAÇÃO DA TAREFA  -----------------------------------------------------------------------------#

		if(ocupado == 1){
			List<EntityID> doors = haveDoors(me().getPosition(),model);
			
			ClearAllRoads(time);
			
			if(PortaBloqueada == 1){
				ClearAllRoads(time);
			}
			
			// TEM PORTAS?????
			if ((haveDoors(me().getPosition(), model) != null)) {

				
				// verifica a lista de portas da rua
				for (EntityID dor : doors) {
					// caso esteja bloqueada
					StandardEntity auxdor = model.getEntity(dor);
					if (((Road) auxdor).isBlockadesDefined() && !((Road) auxdor).getBlockades().isEmpty()) {
						//System.out.println("Limpando a Porta ....");
						node = model.getEntity(auxdor.getID());
						pathtoclean = routing.Explorar(me().getPosition(), Setor, Bloqueios, auxdor.getID());
					    Road r = (Road)model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
					    Blockade b = getTargetBlockade(r, -1);
					    
					    if(b!= null && b.getID() != me().getPosition()){
					    	//System.out.println(" Posição errada");

					    try {
					    	node = auxdor;
					    	//System.out.println(" MERDAAAA "+ pathtoclean);
					    	ocupado = 1;
					    	PortaBloqueada =1;
					    	sendMove(time, pathtoclean, b.getX(), b.getY());
					    	return;
						} catch (Exception e) {
							//System.out.println("aqui");
							ocupado = 0;
							// TODO: handle exception
						}
					    }
					    //System.out.println(" PQPQPQPQPQQPPQ");
						
					}
				}
			}
			
			

	        /*
	    	pathtoclean = routing.Explorar(me().getPosition(), Setor,Bloqueios, node.getID());
	        Road r = (Road)model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
	        Blockade b = getTargetBlockade(r, -1);
	        try {
	        	sendMove(time, pathtoclean, b.getX(), b.getY());
	        	return;
			} catch (Exception e) {
				sendMove(time, pathtoclean);
				return;
			}
	        */
	    	
		}
		
		
// FIM DA REALIZAÇÃO DA TAREFA  -----------------------------------------------------------------------------#

// INICIO DO DEBUG -----------------------------------------------------------------------------------------#
if(time>1){
			// verifica se estou na mesma posição
			if(( me().getPosition().equals(PosicaoPassada))){
				controletempoParadoTotal++;
				// verifica se eh uma rua
				if (model.getEntity(me().getPosition()).getStandardURN().equals(StandardEntityURN.ROAD)){
					// verifica se tem bloqueio 
					if (((Road) model.getEntity(me().getPosition())).isBlockadesDefined() && !((Road) model.getEntity(me().getPosition())).getBlockades().isEmpty() ){
						controletempoParado = 0;
					}else{
						controletempoParado ++;
					}
				}else{
					controletempoParado ++;
				}
			}else{
				controletempoParadoTotal = 0;
			}
			PosicaoPassada = me().getPosition();

			// ZERA TUDO
			if(controletempoParadoTotal > 10){
				//System.out.println("BUG");
				ObrigacoesSoterramento.clear();
				ocupado = 0;
			}
			
			//if(controletempoParado>=3){
			//	//System.out.println("Pensando muito");
			//	ocupado = 0;
			// }
}
		
//FIM DO DEBUG -----------------------------------------------------------------------------------------#

// INICIO DA VERIFICAÇÃO DOS PEDIDOS DE SOCORRO
if (!ObrigacoesSoterramento.isEmpty()) {
	
	List<EntityID> caminho0 = routing.Explorar(me().getPosition(), Setor, Bloqueios,ObrigacoesSoterramento.get(0));
	List<EntityID> caminho1;
	node = model.getEntity(ObrigacoesSoterramento.get(0));
	// ////System.out.println("Opção01: "+ObrigacoesSoterramento.get(0));
	// busca a ação de resgate mais proximo
	for (EntityID soterrados : ObrigacoesSoterramento) {

		caminho1 = routing.Explorar(me().getPosition(),
				Setor, Bloqueios, soterrados);
		// ////System.out.println("Opção: "+soterrados);
		// busca o menor caminho
		if (caminho1.size() <= caminho0.size()) {
			node = model.getEntity(soterrados);
		}

	}
	// atualiza obrigações
	ocupado = 1;
	// gera novo caminho
	pathtoclean = routing.Explorar(me().getPosition(),Setor, Bloqueios, node.getID());
	//StandardEntity destination = model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
	
    try {
    	Road r = (Road)model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
        Blockade b = getTargetBlockade(r, -1);
    	//System.out.println("Na na na na na na na na na na na na na na na na... BATMAN! (Visto indo para:"+node.getID());
    	sendMove(time, pathtoclean, b.getX(), b.getY());
	} catch (Exception e) {
		System.out.println(me().getID()+": AVISO: usando rota 'segura' pois ia dar excecao no destino: "+node.getID());
		sendMove(time, pathtoclean);
	}
	return;
}
// FIM DA VERIFICAÇÃO DOS PEDIDOS DE SOCORRO 

//INICIO DA EXPLORAÇÃO --------------------------------------------------------------------------------#
// Verifica se existe uma rua explorada e que esteja bloqueada
if (exploration.GetBlockRoads() != null) {
	// checar se essa rua esta sob a rota de responsabilidade do agente
	for (StandardEntity road : exploration.GetBlockRoads()) {
		if (model.getEntity(road.getID()) != null) {
			node = road;
			pathtoclean = routing.Explorar(me().getPosition(), Setor,Bloqueios, node.getID());
		    Road r = (Road)model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
		    Blockade b = getTargetBlockade(r, -1);
		    try {
		    	sendMove(time, pathtoclean, b.getX(), b.getY());
			} catch (Exception e) {
				sendMove(time, pathtoclean);
				// TODO: handle exception
			}
		    
		    //System.out.println("INDO PARA UMA RUA BLOQUEADA CONHECIDA");
			ocupado = 1;
			return;
		}
	}
} 

//senão obter nova rota para explorar/limpar
else{
	ocupado = 1;
	Map<EntityID, Set<EntityID>> mapa  = new HashMap<>();
	if(Setor == 6){
		mapa = sectoring.MapPrincipal;
		mapa = search.getGraph();
		
	}else if(Setor == 5){ 
		mapa = sectoring.MapPrincipal;
		mapa = search.getGraph();
	}
	else if(Setor == 4){ 
		mapa = sectoring.MapSetor4;
		mapa = search.getGraph();
	}
	else if(Setor == 3){ 
		mapa = sectoring.MapSetor3;
		mapa = search.getGraph();
	}
	else if(Setor == 2){ 
		mapa = sectoring.MapSetor2;
		mapa = search.getGraph();
	}
	else if(Setor == 1){ 
		mapa = sectoring.MapSetor1;
		mapa = search.getGraph();
	}else{
		mapa = sectoring.MapPrincipal;
		mapa = search.getGraph();
	}
	WalkingInSector walking = new WalkingInSector(model);
	node = walking.GetExplorationNode(time, me().getPosition(model).getID(), mapa, exploration.GetExplorationNodes(), 0);
	// Caso tenha explorado todos nodes
	if (node == null) {
		//System.out.println("EXPLORAÇÃO COMPLETA");
		node = exploration.GetNewExplorationNode(time, 0);
		if(Setor == 5){
			Setor = sectoring.getSetorPertencente(me().getX(), me()
					.getY()) ;}
		if(Setor == 5){
			Setor = 6;
			}
		setSector();
	}
	//System.out.println("EXPLORANDO");
	pathtoclean = routing.Explorar(me().getPosition(), Setor,Bloqueios, node.getID());
    Road r = (Road)model.getEntity(pathtoclean.get(pathtoclean.size() - 1));
    Blockade b = getTargetBlockade(r, -1);
    sendMove(time, pathtoclean, b.getX(), b.getY());
	return;
}
	
}
//FIM DA EXPLORAÇÃO ------------------------------------------------------------------------------------#


	private void ClearAllRoads(int time) {
		Area location = (Area) location();
		//System.out.println("Ainda estou ocupado: "+pathtoclean.toString());
		 // Am I near a blockade?
       Blockade target = getTargetBlockade(time);
       if (target != null) {
       	//if (isGoal(target.getPosition(), pathtoclean)|| getTargetBlockade(location, distance) != null) {
           Logger.info("Clearing blockade " + target);
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
           sendClear(time, (int)(me().getX() + v.getX()), (int)(me().getY() + v.getY()));
           return;
       	//}
       }
       
      
       // Plan a path to a blocked area
       List<EntityID> path = search.breadthFirstSearch(me().getPosition(), getBlockedRoads());
       if (path != null) {
       	//System.out.println("Movendo para bloqueio");
           Logger.info("Moving to target");
           Road r = (Road)model.getEntity(path.get(path.size() - 1));
           Blockade b = getTargetBlockade(r, -1);
           sendMove(time, path, b.getX(), b.getY());
           Logger.debug("Path: " + path);
           Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
           return;
       }
       Logger.debug("Couldn't plan a path to a blocked road");
       Logger.info("Moving randomly");
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
		if (nodesconhecidos2.contains(e) && getBlockedRoads().contains(e)) {
			return true;
		}
		return false;
		// nodesconhecidos2.contains(e);
	}

	/**
	 * retorna todos os vizinhos de um buildings que sejam roads supõe-se que um
	 * vizinho de um building que seja road é uma porta, então este é adicionado
	 * a lista e retornado
	 * 
	 * @param e
	 *            EntityID
	 * @param world
	 *            WorldModel
	 * @return Doors List
	 */
	private List<EntityID> haveDoors(EntityID e, StandardWorldModel world) {
		List<EntityID> doors = new ArrayList<EntityID>();
		StandardEntity entity = world.getEntity(e);
		// se o no do parametro é road
		if (entity.getStandardURN().equals(StandardEntityURN.ROAD)) {
			Road r = (Road) entity;
			// procura portas entre cada vizinho
			for (EntityID n : r.getNeighbours()) {
				StandardEntity _entity = world.getEntity(n);
				// se o vizinho é uma building
				if (_entity.getStandardURN().equals(StandardEntityURN.BUILDING)) {

					// adiciona o nó do parâmetro e o retorna, pois é uma porta
					// doors.add(n);
					doors.add(r.getID());
					// return doors;
					// senão, se o vizinho é road
				} else if (_entity.getStandardURN().equals(
						StandardEntityURN.ROAD)) {
					Road r2 = (Road) _entity;
					for (EntityID n2 : r2.getNeighbours()) {
						StandardEntity _entity2 = world.getEntity(n2);
						if (_entity2.getStandardURN().equals(
								StandardEntityURN.BUILDING)) {
							// doors.add(n2);
							doors.add(r2.getID());
						}
					}
				}
			}
		}
		return doors;
	}

	public void ProcurarPolicias(int time, StandardEntity PosicaoAtual) {
		Collection<StandardEntity> all = model.getObjectsInRange(PosicaoAtual,
				maxDistance);
		// Monta a string de problemas
		// Para tudo que estiver no raio de visão
		for (StandardEntity se : all) {
			// verifica se existe outro policial
			if (se.getStandardURN().equals(StandardEntityURN.POLICE_FORCE)) {
				Human h = (Human) se;
				// Senão for eu
				if (h.getID().getValue() != me().getID().getValue()) {
					// ////System.out.println("Eu: "+me().getID().toString()+"Existe outro Policial aqui"+" ele: "+h.getID().getValue());
					// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					// A ideia é enviar a tarefa objetivo identicos
					List<AbstractMessage> m = new ArrayList<AbstractMessage>();
					if (node != null) {
						m.add(new AbstractMessage(String.valueOf(MSGType.UNBLOCK_ME.ordinal()), node.getID().toString(), me().getID().toString()));
						////System.out.println("Informando Objetivo voz");
						sendMessage(MSGType.UNBLOCK_ME, false, time, m);
					}
				}
			}

		}
	}

	// Todos começam com a via principal
	private void setSector() {

		int aux = random.nextInt(100);
		// Se está na via principal
		// 50% de chance de permanecer na via principal
		// 30% de mudar para lane secundaria
		// 20% de mudar para o setor atual
		if (objetivoSetor == 6) {

			if (aux >= 0 && aux < 30) {
				Setor = 5;
				return;
			} else if (aux >= 30 && aux < 50) {
				Setor = sectoring.getSetorPertencente(me().getX(), me().getY());
				return;
			}

		}

		// Se está na via secundaria
		// 10% de chance de permanecer na lane secundaria
		// 40% de mudar para o setor onde está
		// 15% de mudar para outro setor 2
		// 15% de mudar para outro setor 3
		// 15% de mudar para outro setor 4
		// 05% de mudar para outro lane principal

		if (objetivoSetor == 5) {
			if (aux >= 0 && aux < 10) {
				Setor = 5;
				return;
			} else if (aux >= 10 && aux < 50) {
				Setor = sectoring.getSetorPertencente(me().getX(), me().getY());
				return;
			} else if (aux >= 50 && aux < 55) {
				Setor = 6;
				return;
			} else {
				List<Integer> lista = new ArrayList<Integer>();
				lista.add(1);
				lista.add(2);
				lista.add(3);
				lista.add(4);
				lista.remove(sectoring.getSetorPertencente(me().getX(), me()
						.getY()));
				Setor = lista.get(random.nextInt(lista.size()));
				return;
			}

		}

		if (objetivoSetor != 6 && objetivoSetor != 5) {
			if (aux >= 0 && aux < 15) {
				Setor = 6;
				return;
			} else if (aux >= 15 && aux < 30) {
				Setor = 5;
				return;
			} else if (aux >= 30 && aux < 45) {
				Setor = 4;
				return;
			} else if (aux >= 45 && aux < 60) {
				Setor = 3;
				return;
			} else if (aux >= 60 && aux < 75) {
				Setor = 2;
				return;
			} else if (aux >= 75 && aux < 90) {
				Setor = 1;
				return;
			} else if (aux >= 90 && aux < 100) {
				Setor = sectoring.getSetorPertencente(me().getX(), me().getY());
				return;
			}
		}

	}

}
