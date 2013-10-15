package agent;

import agent.interfaces.IAmbulanceTeam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import model.AbstractMessage;
import model.BurningBuilding;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import util.Channel;
import util.DistanceSorter;
import util.MSGType;
import util.Setores;

/**
 * Agente ambulancia do time EPICENTER
 */
public class MASLABAmbulanceTeam extends MASLABAbstractAgent<AmbulanceTeam>
		implements IAmbulanceTeam {

	/**
	 * 
	 * Variaveis Sample Agent
	 * 
	 */
	private Collection<EntityID> unexploredBuildings;

	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */
	
	public final static String MEAN_BURIEDNESS_DMG_KEY = "misc.damage.mean";
	public final static String STD_BURIEDNESS_DMG_KEY = "misc.damage.sd";
	public final static String FIRE_DMG_KEY = "misc.damage.fire";
	
	//exibir prints?
	private final static boolean DEBUG = false;
	
	//fatores de dano devido a buriedness e fogo
	private float buriedness_dmg_factor;
	private int fire_damage;
	
	//memoria do agente, mapeia ID para um objeto q armazena os dados do humano
	private HashMap<EntityID, MemoryEntry> buriedness_memory;
	
	//timestep atual
	private int current_time;
	
	//alvo atual
	private EntityID current_target;

	//armazena a ultima posicao deste agente
	EntityID lastPosition;
	
	//indicador de 'estou bloqueado por quantos timesteps'?
	int stuckFor;
	

	/*
	 * 
	 * Métodos Standard Agent
	 */
	public MASLABAmbulanceTeam(int pp){
		super(pp);
		
		//coloca valores default nos parametros de calculo de fogo
		//caso nao seja possivel ler no misc-new.cfg
		buriedness_dmg_factor = 1.1f;
		fire_damage = 10;
		
		buriedness_memory = new HashMap<EntityID, MemoryEntry>();
		current_target = null; 
		
		stuckFor = 0;
		
		lastPosition = null;
	}
	
	@Override
	public String toString() {
		return "MASLAB ambulance team, id="+me().getID();
	}

	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */
	/*
	 * 
	 * Métodos Standard Agent
	 */
	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.CIVILIAN,
				StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,
				StandardEntityURN.HYDRANT, StandardEntityURN.GAS_STATION,
				StandardEntityURN.BUILDING);
		unexploredBuildings = new HashSet<EntityID>(buildingIDs);
		
		//buriedness damage factor e' a media + desv. padrao da distribuicao gaussiana usada pra aumentar dmg por soterramento
		try {
			buriedness_dmg_factor = config.getIntValue(MEAN_BURIEDNESS_DMG_KEY) + config.getIntValue(STD_BURIEDNESS_DMG_KEY);
		}
		catch (NoSuchConfigOptionException e) {
			System.out.println(
				"WARNING: key " + MEAN_BURIEDNESS_DMG_KEY + " or " + STD_BURIEDNESS_DMG_KEY +
				" not found in config files. Using default value of " + buriedness_dmg_factor + 
				" for buriedness_dmg_factor."
			);
		}
		//dano por fogo e' dado diretamente no config
		try {
			fire_damage = config.getIntValue(FIRE_DMG_KEY);
		}
		catch (NoSuchConfigOptionException e) {
			System.out.println(
				"WARNING: key " + FIRE_DMG_KEY + 
				" not found in config files. Using default value of " + fire_damage + 
				" for fire_damage."
			);
		}
		if (DEBUG) System.out.println("Ambulance post-connected.");
		
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		
		current_time = time;
		
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, Channel.AMBULANCE.ordinal());
		}
		
		//quando stuckFor e' maior q 1, ambulancia entra no canal da policia
		if (stuckFor > 1){
			//precisa voltar pro canal da ambulancia
			sendSubscribe(time, Channel.AMBULANCE.ordinal());
			if(DEBUG) System.out.println(me().getID() +": voltei pro canal da ambulancia");
			
		}
		
		//testa se esta no mesmo lugar do timestep passado
		if(DEBUG) System.out.println(me().getID() + ": last,cur,stuck = " + lastPosition+","+me().getPosition()+","+stuckFor);
		if (me().getPosition().equals(lastPosition)){
			stuckFor++;
		}
		else {
			stuckFor = 0;
		}
		
		//atualiza o marcador de posicao anterior
		lastPosition = me().getPosition();
		
		/*
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		*/
		//cria msg vazia
		List<AbstractMessage> msgs = new ArrayList<AbstractMessage>();
		
		//percorre lista de humanos, procurando os soterrados e machcados
		for(Human h : getTargets()){
				
				//processa apenas humanos com buriedness ou machucados
				if (h.getBuriedness() > 0 || h.getDamage() > 0) {
					
					//adiciona o humano machucado 'a memoria
					int estimatedDeathTime = estimatedDeathTime(h);
					buriedness_memory.put(h.getID(), new MemoryEntry(h.getPosition(), estimatedDeathTime, h.getBuriedness()));
					
					//adiciona o humano machucado 'a mensagem
					AbstractMessage msg = new AbstractMessage(
						String.valueOf(MSGType.BURIED_HUMAN.ordinal()),
						String.valueOf(h.getID()),
						String.valueOf(h.getPosition()),
						String.valueOf(estimatedDeathTime),
						String.valueOf(h.getBuriedness())
					);
					msgs.add(msg);
					if(DEBUG) System.out.println(me().getID()+": added message " + msg);
				}
		}
		
		//envia mensagens
		if(msgs.size() > 0){
			sendMessage(MSGType.BURIED_HUMAN, true, time, msgs);
			if (DEBUG) System.out.println(me().getID()+": sent message"); 
		}
		
		//Receber Mensagens
		List<String> received = heardMessage(heard);

		//Separa todas as mensagens recebidas pois podem vir agrupadas de um único agente
		List<String> mensagens = new ArrayList<String>();
		for(String s: received){
			//String x[] = s.split(AbstractMessage.MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(AbstractMessage.MSG_FIM)));
		}
		
		//Armazena as informações recebidas
		for(String s: mensagens){
			//Separa as partes da mensagem
			List<String> msg = Arrays.asList(s.split(AbstractMessage.MSG_SEPARATOR));
			
			try{
				//Se for um humano soterrado...
				if(Integer.parseInt(msg.get(0)) == MSGType.BURIED_HUMAN.ordinal()){
					
					//obtem os atributos dele...
					EntityID human_id = new EntityID(Integer.parseInt(msg.get(1)));
					EntityID human_position = new EntityID(Integer.parseInt(msg.get(2)));
					int estimatedDeathTime = Integer.parseInt(msg.get(3));
					int buriedness = Integer.parseInt(msg.get(4));
					
					if(DEBUG) System.out.println(me().getID()+": received msg!" + human_id);
					
					//... e o adiciona 'a memoria
					buriedness_memory.put(
						human_id, new MemoryEntry(human_position, estimatedDeathTime, buriedness)
					);
				}
				
				//se for um pedido de socorro de agente...
				if(Integer.parseInt(msg.get(0)) == MSGType.SAVE_ME.ordinal()){
					
					//TODO tratar pedido de socorro de agente
					System.out.println("Alguem pediu socorro, mas nao implementei o tratamento desta msg...");
				}
			} 
			catch(Exception e) {
				System.out.println("Erro ao decodificar mensagem: " + msg);
			}
			
		}
		if(DEBUG) {
			System.out.println(me().getID() + ": " + buriedness_memory);
			System.out.println(me().getID() + ": " + msgs);
		}
		
		updateUnexploredBuildings(changed);
		
		// Am I transporting a civilian to a refuge?
		if (someoneOnBoard()) {
			// Am I at a refuge?
			if (location() instanceof Refuge) {
				// Unload!
				if(DEBUG) System.out.println("Unloading");
				sendUnload(time);
				
				//tira a vítima da memória 
				current_target = null;
				buriedness_memory.remove(current_target);
				
				if (DEBUG) System.out.println(me().getID()+": delivered human to refuge!");
				
				return;
			} else {
				// Move to a refuge
				List<EntityID> path = routing.Resgatar(me().getPosition(), Bloqueios); 
				if (path != null) {
					if(DEBUG) System.out.println(me().getID()+": Moving to refuge");
					
					//pede ajuda para policial se nao consegue se mover
					checkStuck(time);
					
					sendMove(time, path);
					return;
				}
				// What do I do now? Might as well carry on and see if we can
				// dig someone else out.
				System.out.println(me().getID()+": Failed to plan path to refuge");
			}
		}
		
		//se nao esta engajado em salvar alguem, procura alguem na memoria 
		if (current_target == null) {
			current_target = chooseVictimToRescue();
		}
		else {//se esta engajado em salvar alguem...
			
			//se estou no mesmo lugar que a vitima estaria...
			if (buriedness_memory.get(current_target).position.equals(location().getID())) {
				
				//obtem o human com os dados da vitima (teoricamente esta dentro da visao, dai posso obte-lo)
				Human h_target = (Human)model.getEntity(current_target);
				
				//checa se a vitima foi removida de sua posicao esperada, e a remove da memoria em caso afirmativo
				if (!h_target.getPosition().equals(buriedness_memory.get(current_target).position)) {
					current_target = null;
					buriedness_memory.remove(current_target);
				}
				else {
					//se o alvo foi desenterrado, carrega-o para levar p/ refugio
					if ((h_target instanceof Human) && h_target.getBuriedness() == 0
							&& !(location() instanceof Refuge)) {
						// Load
						if(DEBUG) System.out.println(me().getID() + ": Loading " + current_target);
						sendLoad(time, current_target);
						return;
					}
					//se alvo ainda esta enterrado, desenterra-o
					if (h_target.getBuriedness() > 0) {
						// Rescue
						if(DEBUG) System.out.println(me().getID() + ": Rescueing " + current_target);
						sendRescue(time, current_target);
						return;
					}
				}
			} else {
				//se nao estiver na mesma posicao da vitima, planeja caminho ate ela
				List<EntityID> path = routing.Resgatar(me().getPosition(), buriedness_memory.get(current_target).position, Bloqueios); 
				if (path != null) {
					if(DEBUG) System.out.println(me().getID() + ":Moving to target");
					//pede ajuda para policial se nao consegue se mover
					checkStuck(time);
					sendMove(time, path);
					return;
				}
			}
		}
		
		/*for (Human next : getTargets()) {
			if (next.getPosition().equals(location().getID())) {
				// Targets in the same place might need rescueing or loading
				if ((next instanceof Civilian) && next.getBuriedness() == 0
						&& !(location() instanceof Refuge)) {
					// Load
					System.out.println(me().getID() + ": Loading " + next);
					sendLoad(time, next.getID());
					return;
				}
				if (next.getBuriedness() > 0) {
					// Rescue
					System.out.println(me().getID() + ":Rescueing " + next);
					sendRescue(time, next.getID());
					return;
				}
			} else {
				// Try to move to the target
				List<EntityID> path = routing.Resgatar(me().getPosition(), next.getPosition(), Bloqueios); 
				if (path != null) {
					System.out.println(me().getID() + ":Moving to target");
					sendMove(time, path);
					return;
				}
			}
		}*/
		
		//nao tenho vitima pra salvar, vou explorar o mapa
		List<EntityID> path  = routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios);
		if (path != null) {
			if(DEBUG) System.out.println(me().getID() + ":Searching buildings");
			sendMove(time, path);
			return;
		}
		if (DEBUG) System.out.println(me().getID() + ":Moving randomly");
		sendMove(time, routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios));
	}

	/**
	 * Checa se o agente esta parado ha mais de dois timestep 
	 * e pede ajuda para policial se precisar
	 * @param time
	 */
	private void checkStuck(int time) {
		
		if(stuckFor > 2) {
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
			List<AbstractMessage> sos = new ArrayList<AbstractMessage>();
			sos.add(new AbstractMessage(
				String.valueOf(MSGType.UNBLOCK_ME.ordinal()), 
				String.valueOf(me().getPosition().getValue()))
			);
			sendMessage(MSGType.UNBLOCK_ME, true, time, sos);
			//trocarCanal = true;
			if(DEBUG) System.out.println(me().getID()+ ": SOS! " + sos);
		}
		else {
			if(DEBUG) System.out.println(me().getID() + ": not stuck...");
		}
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}

	/**
	 * 
	 * Métodos Sample Agent
	 * 
	 */
	
	/**
	 * Retorna se esta carregando alguem
	 * @return
	 */
	private boolean someoneOnBoard() {
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if (((Human) next).getPosition().equals(getID())) {
				Logger.debug(next + " is on board");
				return true;
			}
		}
		return false;
	}

	/**
	 * Retorna uma lista de humanos
	 * (creio que dentro do raio de visao <- confirmar isso)
	 * @return
	 */
	private List<Human> getTargets() {
		List<Human> targets = new ArrayList<Human>();
		for (StandardEntity next : model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM)) {
			Human h = (Human) next;
			if (h == me()) {
				continue;
			}
			if (h.isHPDefined() && h.isBuriednessDefined()
					&& h.isDamageDefined() && h.isPositionDefined()
					&& h.getHP() > 0
					&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
				targets.add(h);
			}
		}
		Collections.sort(targets, new DistanceSorter(location(), model));
		return targets;
	}

	/**
	 * Atualiza a lista de buildings q nao foram explorados,
	 * removendo aqueles que percebi neste timestep
	 * @param changed
	 */
	private void updateUnexploredBuildings(ChangeSet changed) {
		for (EntityID next : changed.getChangedEntities()) {
			unexploredBuildings.remove(next);
		}
	}

	/*
	 * 
	 * Métodos Definidos por nós
	 */
	
	/**
	 * Calcula uma estimativa de que timestep a referida vítima vai morrer.
	 * @param current_time - o timestep atual
	 * @param victim - a vítima em questão
	 * @return tempo estimado
	 */
	protected int estimatedDeathTime(Human victim) {
		int edt = 0;
		
		float hp = victim.getHP();
		float damage = victim.getDamage();
		
		while (hp > 0) {
			hp -=  damage;
			damage += getVictimBury() + getVictimFire(victim.getPosition());
			edt++;
		}
		return current_time + edt;
	}
	
	/**
	 * Escolhe qual vítima salvar. A vítima é escolhida a partir da memória da ambulância.
	 * O critério de escolha é pela vítima com maior HP esperado.
	 * @return
	 */
	protected EntityID chooseVictimToRescue() {
		if(DEBUG) System.out.println(me().getID() + ": escolhendo vitima...");

		EntityID chosen = null;
		double chosen_ets = 0;
		for (EntityID v : buriedness_memory.keySet()) { 

			int ets = estimatedTimeSaved(buriedness_memory.get(v));
			if(DEBUG) System.out.println(""+v+" - ets:" + ets);
			if (chosen == null || ets > chosen_ets) {
				chosen = v;
				chosen_ets = ets;
			}
		}
		if(DEBUG) System.out.println(me().getID() + ": escolhi " + chosen);
		return chosen;
	}
	
	/**
	 * Estima o tempo até resgatar a vítima (se deslocar até ela e desenterrá-la)
	 * @param mem
	 * @return
	 */
	protected int estimatedTimeSaved(MemoryEntry mem) {
		int time_until_rescue = routing.Resgatar(me().getPosition(), mem.position, Bloqueios, Setores.UNDEFINED_SECTOR).size();
		time_until_rescue += mem.buriedness;
		
		return mem.expectedDeathTime - (current_time + time_until_rescue);
	}
	
	/**
	 * Estima o HP que uma dada vítima terá após ser salva. 
	 * Salvar uma vítima consiste em se deslocar até ela e desenterrá-la.
	 * @param current_time
	 * @param mem
	 * @return hp esperado
	 */
	protected double estimatedHPWhenRescued(Human victim, MemoryEntry mem) {
		//calcula o tempo até resgatar a vítima (se deslocar até ela e desenterrá-la)
		int time_until_rescue = routing.Resgatar(me().getPosition(), mem.position, Bloqueios, Setores.UNDEFINED_SECTOR).size();
		time_until_rescue += victim.getBuriedness();
		
		//se o tempo até resgatar a vítima excede a expectativa de seu tempo de morte, 
		//então retorna zero (pois a vítima não será salva a tempo)  
		if (current_time + time_until_rescue > mem.expectedDeathTime) {
			return 0;
		}
		
		double hp = victim.getHP();
		float damage = victim.getDamage();
		
		//estima o hp que a vítima terá quando for resgatada
		while (time_until_rescue-- > 0) {
			hp -=  damage;
			damage += getVictimBury() + getVictimFire(mem.position);
		}
		
		return hp;
	}
	
	/**
	 * Item da memoria da ambulancia.
	 * Contem a posicao, timestep esperado da morte e buriedness da vitima
	 * 
	 * @author Gabriel
	 *
	 */
	private class MemoryEntry {
		public EntityID position;
		public int expectedDeathTime;
		public int buriedness;
		
		public MemoryEntry(EntityID position, int expectedDeathTime, int buriedness) {
			this.position = position;
			this.expectedDeathTime = expectedDeathTime;
			this.buriedness = buriedness;
		}
		
		public String toString(){
			return "[(edt,pos,bur)=("+expectedDeathTime+","+position+","+buriedness+")]"; 
		}
	}
	
	/**
	 * Estima o dano relativo ao soterramento
	 * @return
	 */
	protected float getVictimBury() {
		float bury = 0.9f;
		return bury;
	}
	
	/**
	 * Estima o dano relativo ao fogo
	 * @param victim_position
	 * @return
	 */
	protected float getVictimFire(EntityID victim_position) {
		float fire = 0;
		if (model.getEntity(victim_position) instanceof Building) {
			Building b = (Building)model.getEntity(victim_position);
			if (b.isOnFire()) {
				switch (b.getFieryness()) {
					case 1:
						fire = 10;
					case 2:
						fire = 100;
					default:
						fire = 1000;
				}
			}
		}
		return fire;
	}
	
	/*
	 * 
	 * Métodos Acessores se necessário
	 */
}
