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
	private int temObjetivo = 0; // define se o policial ja tem uma tarefa
	private int radioControl = 0; // define se existe algum pedido de socorro
	private List<EntityID> pathtoclean = new ArrayList<EntityID>();;
	private int maxView;
	private StandardEntity node; // node objetivo
	private int Setor = 6;
	private int objetivoSetor = 5; // Variavel auxiliar que define o objetivo do
									// agente
	private String MSG_SEPARATOR = "-";
	private String MSG_FIM = ",";
	private List<EntityID> ObrigacoesSoterramento = new ArrayList<EntityID>();; // lista
																				// de
																				// chamado
																				// do
																				// setor
	private int ControlResgate = 0; // status de resgate
	private List<EntityID> ComunicarIncendios = new ArrayList<EntityID>();
	private int controletempoParado=0;
	private int controletempoParadoTotal=0;
	private EntityID PosicaoPassada;
	private List<EntityID> MensageActivites = new ArrayList<EntityID>();
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

	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
		}

		Area location = (Area) location();
		search = new MASLABBFSearch(model);
		// Atualiza o ambiente ao redor do agente
		PerceberAmbiente(time, me().getPosition(model));

		if(time==1){
			PosicaoPassada = me().getPosition();
		}
		
		
		// lista de chamado

		// Comunicar incendios observados
		// sendMessage(BURNING_BUILDING,true,time,mensagem)
		// ComunicarIncendios

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
			// //System.out.println("Informando incendio");
			sendMessage(MSGType.BURNING_BUILDING, true, time, m);
		}
		// Verifica se existe policiais ao redor
		ProcurarPolicias(time, me().getPosition(model));

		// Receber Mensagens
		List<String> msgs = heardMessage(heard);
		// //System.out.println("mensagens 00: "+msgs);
		// Separa todas as mensagens recebidas pois podem vir agrupadas de um
		// único agente
		List<String> mensagens = new ArrayList<String>();
		for (String s : msgs) {
			String x[] = s.split(MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(MSG_FIM)));
		}

		// //System.out.println("Verificando canais de comunicação.....");
		// //System.out.println("Mensagem: "+ mensagens.toString());
		// Armazena as informações recebidas

		for (String s : mensagens) {
			// Separa as partes da mensagem
			List<String> msg = Arrays.asList(s.split(MSG_SEPARATOR));
			// //System.out.println("Existe um pedido de resgate.................................:"+
			// s.toString());
			// Tamanho da mensagem de prédios em chamas
			// TODO - Ver uma forma eficiente de tratar as mensagens pois pode
			// ser que recebamos um texto
			Boolean bAdd = true;
			BurningBuilding alvoAux = null;
			// Caso seja uma mensagem por radio
			if (msg.size() == 2) {
				// //System.out.println("Pedido de resgate em: "+
				// msg.get(1).toString());
				// Se for um infeliz bloqueado adicionar a lista
				// MensageActivites
				if (Integer.parseInt(msg.get(0)) == MSGType.UNBLOCK_ME
						.ordinal()) {
					// //System.out.println("Adicionando nova tarefa");

					if (!MensageActivites.contains(new EntityID(Integer
							.parseInt(msg.get(1))))) {
						MensageActivites.add(new EntityID(Integer.parseInt(msg
								.get(1))));
						radioControl = 1;
					}
					
					// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					// A ideia é gerar uma lista <MensageActivites> de
					// Locais onde estão os bloqueados
				}
			}
			
			// Caso seja uma mensagem por voz
			if (msg.size() == 3) {
				//System.out.println("Verificando mensagem por voz");
				if (Integer.parseInt(msg.get(0)) == MSGType.UNBLOCK_ME
						.ordinal()) {
					// verificar se o objetivo é o mesmo
					if (node.equals(new EntityID(Integer.parseInt(msg.get(1))))) {
						// Menor id permanece com a tarefa
						if (Integer.parseInt(me().getID().toString()) > Integer
								.parseInt(new EntityID(Integer.parseInt(msg
										.get(2))).toString())) {
							//System.out.println("Policial com mesmo objetivo ...");
							try {
								MensageActivites.remove(new EntityID(Integer
										.parseInt(msg.get(1))));
								ObrigacoesSoterramento.remove(new EntityID(
										Integer.parseInt(msg.get(1))));
								//System.out.println("Removendo Atividade");
							} catch (Exception e) {
								// TODO: handle exception
							}
						}

					}

					// new EntityID(Integer.parseInt(msg.get(1)));

				}

			}

		}

		// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		// caso seja uma mensagem por voz de outro policial
		// verificar checarInteresse.CheckAnotherActions(me().getID(),
		// h.getID(), pathtoclean, exploracao.GetLastAction(10,
		// exploration.Exploracao), OutrosObjetivos)
		// tomar outra ação de acordo com o return e o ID (menor)

     	//System.out.println("Estou: "+ me().getPosition() +" estive:"+ PosicaoPassada);
		if(time>1){
			
			// verifica se estou na mesma posição
			if(( me().getPosition().equals(PosicaoPassada))){
				controletempoParadoTotal++;
				// verifica se eh uma rua
				if (model.getEntity(me().getPosition()).getStandardURN().equals(StandardEntityURN.ROAD)){
					// verifica se tem bloqueio 
					if (((Road) model.getEntity(me().getPosition())).isBlockadesDefined() && !((Road) model.getEntity(me().getPosition())).getBlockades().isEmpty() ){
						controletempoParado = 0;
						// Aqui tem um bug  --- caso o agente esteja em uma lane com bloqueio e o bloqueio esta longe
						// ele ficará aqui para sempre --- boa sorte o/
						// a ideia seria ele mover um pouco em direção ao bloqueio e alcança-lo
					}else{
						controletempoParado ++;
						//System.out.println("Estou parado numa rua");
					}
				}else{
					controletempoParado ++;
				}
			}else{
				controletempoParadoTotal = 0;
			}
			PosicaoPassada = me().getPosition();
				//if( == false)
			if(controletempoParadoTotal > 10){
				node = exploration.GetNewExplorationNode(time, 0);
				pathtoclean = routing.Explorar(me().getPosition(), Setor, Bloqueios);
				sendMove(time, pathtoclean);
				temObjetivo = 1;
				radioControl = 0;
				ControlResgate = 0;
				MensageActivites = new ArrayList<EntityID>();
				return;
			}
			
			if(controletempoParado>=3){
				temObjetivo = 0;
				radioControl = 0;
				ControlResgate = 0;
				MensageActivites = new ArrayList<EntityID>();
				//System.out.println("Objetivo: "+ node.getID().toString());
				//System.out.println("Caminho: "+ pathtoclean.toString());
				pathtoclean = routing.Explorar(me().getPosition(),Setor, Bloqueios, node.getID());
				//System.out.println("Novo Caminho: "+ pathtoclean.toString());
				sendMove(time, pathtoclean);

				return;
				//controletempoParado = 0;
				//Setor = sectoring.getSetorPertencente(me().getX(), me().getY()) ;
			}
		}
		
		// Verifica se existe um objetivo especifico
		if (temObjetivo == 1) {
			//System.out.println("Tenho um objetivo:" + node);
			// Caso exista um chamado de radio
			if (radioControl == 1) {
				//System.out.println("é um atendendimento de radio");
				// //System.out.println("Existe um pedido de socorro verificando....");
				// Verifica se realmente existe mensagem
				if (!MensageActivites.isEmpty()) {
					for (EntityID mensage : MensageActivites) {
						//System.out.println("Pedido de socorro: "+ mensage.getValue());
						// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????
						// a ideia verificar se a atividade pertence ao setor
						// que eu me encontro....
						// caso afirmativo inserir essa obrigação
						if (routing.getRoadIDs(Setor).contains(mensage)) {
							//System.out.println("Obrigação:"+ mensage.getValue());
							// ////System.out.println("é meu dever sevir a sociedade e atirar no fogo");
							// adiciona uma tarefa na lista de soterrados
							if (!ObrigacoesSoterramento.contains(mensage)) {
								ObrigacoesSoterramento.add(mensage);
							}
						}

					}
				}

				// caso não esteja realizando nenhum resgate
				if (ControlResgate == 0) {
					//System.out.println("verificando obrigações");
					// //System.out.println("Não tenho nenhum dever no momento verificando pendencias....");
					// verifica se possui alguma pendencia
					if (!ObrigacoesSoterramento.isEmpty()) {
						// //System.out.println("to devendo horas... merda");
						List<EntityID> caminho0 = routing.Explorar(me()
								.getPosition(), Setor, Bloqueios,
								ObrigacoesSoterramento.get(0));
						List<EntityID> caminho1;
						node = model.getEntity(ObrigacoesSoterramento.get(0));
						// //System.out.println("Opção01: "+ObrigacoesSoterramento.get(0));
						// busca a ação de resgate mais proximo
						for (EntityID soterrados : ObrigacoesSoterramento) {

							caminho1 = routing.Explorar(me().getPosition(),
									Setor, Bloqueios, soterrados);
							// //System.out.println("Opção: "+soterrados);
							// busca o menor caminho
							if (caminho1.size() <= caminho0.size()) {
								node = model.getEntity(soterrados);

							}

						}
						// atualiza obrigações
						ControlResgate = 1;
						// gera novo caminho
						pathtoclean = routing.Explorar(me().getPosition(),
								Setor, Bloqueios, node.getID());
						// //System.out.println(me().getID()+" nova atividade:" +
						// node.getID());
						sendMove(time, pathtoclean);
						return;

					}

					//radioControl = 0;

				} else {

					// Continua o resgate do soterrado
					// Am I near a blockade?
					Blockade target = getTargetBlockade(time);
					if (target != null) {
						// Verifica se o bloqueio se encontra sob a rota de
						// limpeza ou onde eu estou
						location = (Area) location();
						if (isGoal(target.getPosition(), pathtoclean)
								|| getTargetBlockade((location), distance) != null) {
							// //System.out.println("Tem um bloqueio aqui");
							// Caso afirmativo limpar
							Logger.info("Clearing blockade " + target);
							sendSpeak(time, 1,
									("Clearing " + target).getBytes());
							// sendClear(time, target.getX(), target.getY());
							List<Line2D> lines = GeometryTools2D.pointsToLines(
									GeometryTools2D.vertexArrayToPoints(target
											.getApexes()), true);
							double best = Double.MAX_VALUE;
							Point2D bestPoint = null;
							Point2D origin = new Point2D(me().getX(), me()
									.getY());
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
							Vector2D v = bestPoint.minus(new Point2D(me()
									.getX(), me().getY()));
							v = v.normalised().scale(1000000);
							sendClear(time, (int) (me().getX() + v.getX()),
									(int) (me().getY() + v.getY()));
							return;

						}
						// //System.out.println("Tem um bloqueio mais n é meu dever...");
					}

					// Verifica se cheguei ao meu objetivo
					if (me().getPosition().equals(node.getID())) {
						//System.out.println(me().getID().toString()+ " Estou em meu objetivo:" + node.getID());
						StandardEntity local = model.getEntity(me()
								.getPosition());
						try {
							// Verifica se estou em uma road
							if (((Road) local).getStandardURN().equals(
									StandardEntityURN.ROAD)) {
								// Verifica se n existe bloqueio
								if (!((Road) model.getEntity(me().getPosition())).isBlockadesDefined() && ((Road) model.getEntity(me().getPosition())).getBlockades().isEmpty() ) {
									// Remove a tarefa da lista e busca nova
									// tarefa
									// //System.out.println(me().getID()+" Tarefa concluida:"
									// + node.getID());
									ObrigacoesSoterramento.remove(me()
											.getPosition());
									ControlResgate = 0;
									//radioControl = 0;
								}

								else {
									// Quando ele cai numa porta ele da merda
									ObrigacoesSoterramento.remove(me()
											.getPosition());
									ControlResgate = 0;
									//radioControl = 0;
									// //System.out.println(me().getID()+" Tarefa concluida o/:"
									// + node.getID());
								}
							}
						} catch (Exception e) {
							// Remove a tarefa da lista e busca nova tarefa
							ObrigacoesSoterramento.remove(me().getPosition());
							ControlResgate = 0;
							//radioControl = 0;
						}
					}
					// Continua se movendo
					sendMove(time, routing.Explorar(me().getPosition(), Setor,
							Bloqueios, node.getID()));
					return;

				}
				// //System.out.println("Obrigações: ...."+
				// ObrigacoesSoterramento.toString());
				// Caso não tenha mais obrigações inicia exploração
				if (ObrigacoesSoterramento.isEmpty()) {
					radioControl = 0;
					temObjetivo = 0;
					// //System.out.println(" não estou devendo horas o/");
				}
			}
			// Continuar tarefa priori de alocação
			else {
				// Am I near a blockade?
				Blockade target = getTargetBlockade(time);
				if (target != null) {
					// Verifica se o bloqueio se encontra sob a rota de limpeza
					// ou onde eu estou
					if (isGoal(target.getPosition(), pathtoclean)
							|| getTargetBlockade(location, distance) != null) {
						// //System.out.println("Existe um bloqueio onde estou *.*");
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

					// //System.out.println("Verificando portas");
					// caso exista uma porta onde estou e caso eu esteja no rumo
					// do objetivo
					if ((haveDoors(me().getPosition(), model) != null)) {

						List<EntityID> doors = haveDoors(me().getPosition(),
								model);
						// verifica a lista de portas da rua
						for (EntityID dor : doors) {
							// caso esteja bloqueada
							StandardEntity auxdor = model.getEntity(dor);
							if (((Road) auxdor).isBlockadesDefined() && !((Road) auxdor).getBlockades().isEmpty()) {
								// //System.out.println("Porta bloqueada: " +
								// dor);
								pathtoclean = routing.Explorar(me()
										.getPosition(), Setor, Bloqueios, dor);
								sendMove(time, pathtoclean);
								node = auxdor;
								//System.out.println("Novo Objetivo: Porta identificada ("+ node.getID() + ")");
								temObjetivo = 1;
							}
						}
					}

				}
				// ////System.out.println("Me: "+me().getPosition()+
				// " Objetivo: "+
				// node.getID())
				if (me().getPosition().equals(node.getID())) {
					StandardEntity local = model.getEntity(me().getPosition());
					// Verifica se estou em uma road
					// //System.out.println("Alcancei o meu objetivo");
					// Tem uma merda aqui --- quando entra num edificio
					try {

						if (((Road) local).getStandardURN().equals(
								StandardEntityURN.ROAD)) {

							// //System.out.println("é uma Rua o/");
							// Verifica se n existe bloqueio
							if (!((Road) model.getEntity(me().getPosition())).isBlockadesDefined() && ((Road) model.getEntity(me().getPosition())).getBlockades().isEmpty()) {
								//System.out.println("não Existe mais nada para limpar aqui o/");
								temObjetivo = 0;
							}

							else {
								// Quando ele cai numa porta ele da merda
								// ////System.out.println("Merda ......");
								temObjetivo = 0;
							}
						}
					} catch (Exception e) {
						temObjetivo = 0;
					}
				}
				// Continua se movendo
				sendMove(time, routing.Explorar(me().getPosition(), Setor,
						Bloqueios, node.getID()));
				return;
			}

		} else {

			//System.out.println("Iniciando Exploração");
			// Verifica se existe uma rua explorada e que esteja bloqueada
			//System.out.println("Nenhum objetivo .... Verificando Nodes Explorados");
			if (exploration.GetBlockRoads() != null) {
				// checar se essa rua esta sob a rota de responsabilidade do
				// agente
				for (StandardEntity road : exploration.GetBlockRoads()) {
					if (model.getEntity(road.getID()) != null) {
						// isGoal(road.getID(), pathtoclean)){
						node = road;
						// //System.out.println("Resultado exploração:"+
						// node.getID());
						pathtoclean = routing.Explorar(me().getPosition(),
								Setor, Bloqueios, node.getID());
						sendMove(time, pathtoclean);
						temObjetivo = 1;
						return;
					}
				}
			}

			//System.out.println("Iniciando nova exploração...");

			// senão obter nova rota para explorar/limpar
			WalkingInSector walking = new WalkingInSector(model);
			Map<EntityID, Set<EntityID>> mapa = sectoring.MapSetor2;
			mapa = search.getGraph();
			// ////System.out.println("Objetivo: "+mapa.values());

			node = walking.GetExplorationNode(time, me().getPosition(model)
					.getID(), mapa, exploration.GetExplorationNodes(), 0);

			if (node == null) {
				//System.out.println("Roleta....");
				node = exploration.GetNewExplorationNode(time, 0);
				if(Setor == 5){
					Setor = sectoring.getSetorPertencente(me().getX(), me()
							.getY()) ;}
				if(Setor == 5){
					Setor = 6;}
				setSector();
			}
			// ////System.out.println("Posicao Atual: "+
			// me().getPosition()+" Objetivo: "+ node.getID());

			pathtoclean = routing.Explorar(me().getPosition(), Setor,
					Bloqueios, node.getID());

			// Mover(me().getID(),Setor, node.getID());
			// ////System.out.println(" indo por : "+pathtoclean);

			// gotoDestino(me().getID(), node.getID(), Bloqueios, false, 0);
			// routing.Explorar();

			// ////System.out.println("Iniciando exploração: " +
			// node.getID().toString());
			temObjetivo = 1;
			sendMove(time, pathtoclean);
			// //System.out.println("Novo Objetivo: " + node);
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
					// //System.out.println("Eu: "+me().getID().toString()+"Existe outro Policial aqui"+" ele: "+h.getID().getValue());
					// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????
					// A ideia é enviar a tarefa objetivo identicos
					List<AbstractMessage> m = new ArrayList<AbstractMessage>();
					if (node != null) {
						m.add(new AbstractMessage(String.valueOf(MSGType.UNBLOCK_ME.ordinal()), node.getID().toString(), me().getID().toString()));
						//System.out.println("Informando Objetivo voz");
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
