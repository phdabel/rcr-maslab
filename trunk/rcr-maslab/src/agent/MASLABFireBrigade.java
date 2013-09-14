package agent;

import agent.interfaces.IFireBrigade;
import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.FireBrigade;
import util.Channel;
import util.DistanceSorter;
import util.MASLABPreProcessamento;
import util.MASLABSectoring;
import util.Setores;

/**
 * A sample fire brigade agent.
 */
public class MASLABFireBrigade extends MASLABAbstractAgent<FireBrigade>
		implements IFireBrigade {

	/**
	 * 
	 * Variaveis Sample Agent
	 * 
	 */

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	private static final String GAS_STATION_RANGE = "ignition.gas_station.explosion.range";
	
	private int maxWater;
	private int maxDistance;
	private int maxPower;
	private int gasStationRange;
	
	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */

	/*
	 * 
	 * Métodos Standard Agent
	 */

	public MASLABFireBrigade(int pp){
		super(pp);
	}
	
	@Override
	public String toString() {
		return "MASLAB fire brigade";
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,
				StandardEntityURN.HYDRANT, StandardEntityURN.GAS_STATION);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		gasStationRange = config.getIntValue(GAS_STATION_RANGE);
		
		Logger.info("Sample fire brigade connected: max extinguish distance = "
				+ maxDistance + ", max power = " + maxPower + ", max tank = "
				+ maxWater);
		
		if(PreProcessamento > 0){
			//Cria um novo objeto da classe de setorizacao e setoriza
	        sectoring = new MASLABSectoring(model);
	        sectoring.Setorizar();
	        
	        //Cria um novo objeto da classe de pre-processamento e gera os arquivos
			MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(sectoring);
			PreProcess.GerarArquivos();
			
		//Carrega os arquivos já processados
		}else{
			//Esse procedimento é genérico para todos os agentes, por isso está na classe MASLABAbstractAgent
		}
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, Channel.FIRE_BRIGADE.ordinal());
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		FireBrigade me = me();
		// Are we currently filling with water?
		if (me.isWaterDefined() && me.getWater() < maxWater
				&& location() instanceof Refuge) {
			Logger.info("Filling with water at " + location());
			sendRest(time);
			return;
		}
		// Are we out of water?
		if (me.isWaterDefined() && me.getWater() == 0) {
			// Head for a refuge
			List<EntityID> path = routing.Abastecer(me().getPosition(), Bloqueios); 
			if (path != null) {
				Logger.info("Moving to refuge");
				sendMove(time, path);
				return;
			} else {
				Logger.debug("Couldn't plan a path to a refuge.");
				path = routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios);
				Logger.info("Moving randomly");
				sendMove(time, path);
				return;
			}
		}
		// Find all buildings that are on fire
		Collection<EntityID> all = getBurningBuildings();
		// Can we extinguish any right now?
		for (EntityID next : all) {
			if (model.getDistance(getID(), next) <= maxDistance) {
				Logger.info("Extinguishing " + next);
				sendExtinguish(time, next, maxPower);
				sendSpeak(time, 1, ("Extinguishing " + next).getBytes());
				return;
			}
		}
		// Plan a path to a fire
		for (EntityID next : all) {
			List<EntityID> path = planPathToFire(next);
			if (path != null) {
				Logger.info("Moving to target");
				sendMove(time, path);
				return;
			}
		}
		List<EntityID> path = null;
		Logger.debug("Couldn't plan a path to a fire.");
		path = routing.Explorar(me().getPosition(), Setores.UNDEFINED_SECTOR, Bloqueios);
		Logger.info("Moving randomly");
		sendMove(time, path);
	}

	/**
	 * 
	 * Métodos Sample Agent
	 * 
	 */
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	private Collection<EntityID> getBurningBuildings() {
		Collection<StandardEntity> e = model
				.getEntitiesOfType(StandardEntityURN.BUILDING);
		List<Building> result = new ArrayList<Building>();
		for (StandardEntity next : e) {
			if (next instanceof Building) {
				Building b = (Building) next;
				if (b.isOnFire()) {
					result.add(b);
				}
			}
		}
		// Sort by distance
		Collections.sort(result, new DistanceSorter(location(), model));
		return objectsToIDs(result);
	}

	private List<EntityID> planPathToFire(EntityID target) {
		// Try to get to anything within maxDistance of the target
		Collection<StandardEntity> targets = model.getObjectsInRange(target,
				maxDistance);
		if (targets.isEmpty()) {
			return null;
		}
		//TODO - Definir o alvo do combate
		return routing.Combater(me().getPosition(), new ArrayList<EntityID>(objectsToIDs(targets)).get(0), Bloqueios);
	}
	/*
	 * 
	 * Métodos Definidos por nós
	 */
	/*
	 * 
	 * Métodos Acessores se necessário
	 */
	
	
	/**
	 * Returns the explosion range of the gas station
	 * @return
	 */
	public int getGasStationRange(){
		return gasStationRange;
	}
}
