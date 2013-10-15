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
 * A sample ambulance team agent.
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
	
	private float buriedness_dmg_factor;
	private int fire_damage;
	
	private HashMap<EntityID, MemoryEntry> buriedness_memory;
	
	private int current_time;
	
	private Human current_target;

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
	}
	
	@Override
	public String toString() {
		return "MASLAB ambulance team";
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
		
		//System.out.println(config.getValue("kernel.agents.think-time"));
		//System.out.println(config.getValue("kernel.timesteps"));
		
		//buriedness damage factor is the mean + standard variation of the gaussian distribution used to increase damage by buriedness
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
		//fire damage is the one directly given in config
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
		//System.out.println("POST-CONNECTED");
		
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		
		current_time = time;
		
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			sendSubscribe(time, Channel.AMBULANCE.ordinal());
		}
		/*
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		*/
		//cria msg vazia
		List<AbstractMessage> msgs = new ArrayList<AbstractMessage>();
		
		//percorre lista de entidades percebidas procurando humanos soterrados
		//for (EntityID id : changed.getChangedEntities()){
			
			//processa apenas humanos
		for(Human h : getTargets()){//	if (model.getEntity(id) instanceof Human) {
				//Human h = (Human) model.getEntity(id);
				
				//processa apenas humanos com buriedness ou machucados
				if (h.getBuriedness() > 0 || h.getDamage() > 0) {
					
					//adiciona o humano machucado 'a memoria
					int estimatedDeathTime = estimatedDeathTime(h);
					buriedness_memory.put(h.getID(), new MemoryEntry(h.getPosition(), estimatedDeathTime));
					
					//adiciona o humano machucado 'a mensagem
					AbstractMessage msg = new AbstractMessage(
						String.valueOf(MSGType.BURIED_HUMAN.ordinal()),
						String.valueOf(h.getID()),
						String.valueOf(h.getPosition()),
						String.valueOf(estimatedDeathTime)
					);
					msgs.add(msg);
					System.out.println(me().getID()+": added message " + msg);
				}
				//else
					//System.out.println(me() + ": hdmg="+h.getDamage());
			//}
		}
		
		if(msgs.size() > 0){
			sendMessage(MSGType.BURIED_HUMAN, true, time, msgs);
			System.out.println(me().getID()+": sent message"); 
		}
		
		//Receber Mensagens
		List<String> received = heardMessage(heard);

		//Separa todas as mensagens recebidas pois podem vir agrupadas de um único agente
		List<String> mensagens = new ArrayList<String>();
		for(String s: received){
			String x[] = s.split(AbstractMessage.MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(AbstractMessage.MSG_FIM)));
		}
		
		//Armazena as informações recebidas
		for(String s: mensagens){
			//Separa as partes da mensagem
			List<String> msg = Arrays.asList(s.split(AbstractMessage.MSG_SEPARATOR));
			
			//Tamanho da mensagem de prédios em chamas
			try{
				//Se for um humano soterrado...
				if(Integer.parseInt(msg.get(0)) == MSGType.BURIED_HUMAN.ordinal()){
					
					EntityID human_id = new EntityID(Integer.parseInt(msg.get(1)));
					EntityID human_position = new EntityID(Integer.parseInt(msg.get(2)));
					int estimatedDeathTime = Integer.parseInt(msg.get(3));
					
					buriedness_memory.put(
						human_id, new MemoryEntry(human_position, estimatedDeathTime)
					);
				}
			} catch(Exception e) {
				System.out.println("Erro ao decodificar mensagem: " + msg);
			}
			
		}
		
		System.out.println(me().getID() + ": " + buriedness_memory);
		System.out.println(me().getID() + ": " + msgs);
		
		updateUnexploredBuildings(changed);
		// Am I transporting a civilian to a refuge?
		if (someoneOnBoard()) {
			// Am I at a refuge?
			if (location() instanceof Refuge) {
				// Unload!
				Logger.info("Unloading");
				sendUnload(time);
				
				//tira a vítima da memória 
				current_target = null;
				buriedness_memory.remove(current_target.getID());
				
				return;
			} else {
				// Move to a refuge
				List<EntityID> path = routing.Resgatar(me().getPosition(), Bloqueios); 
				if (path != null) {
					Logger.info("Moving to refuge");
					sendMove(time, path);
					return;
				}
				// What do I do now? Might as well carry on and see if we can
				// dig someone else out.
				Logger.debug("Failed to plan path to refuge");
			}
		}
		// Go through targets (sorted by distance) and check for things we can
		// do
		if (current_target == null) {
			current_target = chooseVictimToRescue();
		}
		if (current_target.getPosition().equals(location().getID())) {
			// Targets in the same place might need rescueing or loading
			if ((current_target instanceof Civilian) && current_target.getBuriedness() == 0
					&& !(location() instanceof Refuge)) {
				// Load
				System.out.println(me().getID() + ": Loading " + current_target);
				sendLoad(time, current_target.getID());
				return;
			}
			if (current_target.getBuriedness() > 0) {
				// Rescue
				System.out.println(me().getID() + ":Rescueing " + current_target);
				sendRescue(time, current_target.getID());
				return;
			}
		} else {
			// Try to move to the target
			List<EntityID> path = routing.Resgatar(me().getPosition(), current_target.getPosition(), Bloqueios); 
			if (path != null) {
				System.out.println(me().getID() + ":Moving to target");
				sendMove(time, path);
				return;
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
		// Nothing to do
		List<EntityID> path  = routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios);
//		List<EntityID> path = search.breadthFirstSearch(me().getPosition(),unexploredBuildings);
		if (path != null) {
			System.out.println(me().getID() + ":Searching buildings");
			sendMove(time, path);
			return;
		}
		System.out.println(me().getID() + ":Moving randomly");
		sendMove(time, routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios));
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
	protected Human chooseVictimToRescue() {
		
		Human chosen = null;
		double chosen_hp = 0;
		for (EntityID v : buriedness_memory.keySet()) { 
			Human victim = (Human)model.getEntity(v);
			double hp = estimatedHPWhenRescued(victim, buriedness_memory.get(v));
			if (chosen == null || hp > chosen_hp) {
				chosen = victim;
				chosen_hp = hp;
			}
		}
		return chosen;
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
	
	private class MemoryEntry {
		public EntityID position;
		public int expectedDeathTime;
		
		public MemoryEntry(EntityID position, int expectedDeathTime) {
			this.position = position;
			this.expectedDeathTime = expectedDeathTime;
		}
		
		public String toString(){
			return "[(edt,pos)=("+expectedDeathTime+","+position+")]"; 
		}
	}
	
	protected float getVictimBury() {
		//estima o dano relativo ao soterramento
		float bury = 0.9f;
		return bury;
	}
	
	protected float getVictimFire(EntityID victim_position) {
		//estima o dano relativo ao fogo
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
