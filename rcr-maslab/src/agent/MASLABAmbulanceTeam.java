package agent;

import agent.interfaces.IAmbulanceTeam;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import util.Channel;
import util.DistanceSorter;
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
	

	/*
	 * 
	 * Métodos Standard Agent
	 */
	public MASLABAmbulanceTeam(int pp){
		super(pp);
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
		
		//buriedness damage factor is the mean + standard variation of the gaussian distribution used to increase damage by buriedness 
		buriedness_dmg_factor = config.getIntValue(MEAN_BURIEDNESS_DMG_KEY) + config.getIntValue(STD_BURIEDNESS_DMG_KEY);
		
		//fire damage is the one directly given in config
		fire_damage = config.getIntValue(FIRE_DMG_KEY);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, Channel.AMBULANCE.ordinal());
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		updateUnexploredBuildings(changed);
		// Am I transporting a civilian to a refuge?
		if (someoneOnBoard()) {
			// Am I at a refuge?
			if (location() instanceof Refuge) {
				// Unload!
				Logger.info("Unloading");
				sendUnload(time);
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
		for (Human next : getTargets()) {
			if (next.getPosition().equals(location().getID())) {
				// Targets in the same place might need rescueing or loading
				if ((next instanceof Civilian) && next.getBuriedness() == 0
						&& !(location() instanceof Refuge)) {
					// Load
					Logger.info("Loading " + next);
					sendLoad(time, next.getID());
					return;
				}
				if (next.getBuriedness() > 0) {
					// Rescue
					Logger.info("Rescueing " + next);
					sendRescue(time, next.getID());
					return;
				}
			} else {
				// Try to move to the target
				List<EntityID> path = routing.Resgatar(me().getPosition(), next.getPosition(), Bloqueios); 
				if (path != null) {
					Logger.info("Moving to target");
					sendMove(time, path);
					return;
				}
			}
		}
		// Nothing to do
		List<EntityID> path  = routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios);
//		List<EntityID> path = search.breadthFirstSearch(me().getPosition(),unexploredBuildings);
		if (path != null) {
			Logger.info("Searching buildings");
			sendMove(time, path);
			return;
		}
		Logger.info("Moving randomly");
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
	 * Retorna a expectativa de vida do agente, levando em conta o tempo que a ambulância leva até resgatá-lo.
	 * Ev=(time_until_victim + buriedness)*(fire_damage + buriedness_dmg_factor*buriedness + damage)
	 * time_until_victim = tempo de deslocamento até a vítima (estimativa de 1 timestep por EntityID)
	 * buriedness = o nível de soterramento da vítima
	 * fire_damage = o quanto de damage o fogo causa na vítima por timestep
	 * buriedness_dmg_factor = estimativa de quanto damage o buriedness vai causar na vítima por timestep
	 * damage = damage atual da vítima 
	 * @param Human victim 
	 * @return double
	 */
	protected double estimatedLifeTime(Human victim) {
		int time_until_victim = routing.Resgatar(me().getPosition(), victim.getPosition(), Bloqueios, Setores.UNDEFINED_SECTOR).size();
		float buriedness_dmg = buriedness_dmg_factor * victim.getBuriedness();
		
		double ev = (time_until_victim + victim.getBuriedness()) * (fire_damage + buriedness_dmg + victim.getDamage());
		return ev;
	}
	
	/**
	 * Escolhe a vítima com maior estimativa de vida
	 */
	protected Human chooseVictimToRescue() {
		//TODO: atualizar getTargets() para retornar algum humano na base de conhecimento do agente, ao inves do atualmente implementado do sample agent
		
		Human choosen = null;
		double choosen_ev = 0;
		for (Human victim : getTargets()) { 
			double ev = estimatedLifeTime(victim);
			if (choosen == null || ev > choosen_ev) {
				choosen = victim;
				choosen_ev = ev;
			}
		}
		return choosen;
	}
	
	
	
	/*
	 * 
	 * Métodos Acessores se necessário
	 */
}
