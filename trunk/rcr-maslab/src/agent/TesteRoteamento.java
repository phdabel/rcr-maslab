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
import util.DistanceSorter;
import util.MASLABPreProcessamento;
import util.MASLABSectoring;
import util.MASLABRouting.Setores;
import util.MASLABSectoring;

/**
 * A sample fire brigade agent.
 */
public class TesteRoteamento extends MASLABAbstractAgent<FireBrigade> implements
		IFireBrigade {

	/**
	 * 
	 * Variaveis Sample Agent
	 * 
	 */

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	private int maxWater;
	private int maxDistance;
	private int maxPower;
	
	private MASLABSectoring sectoring;

	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */

	private List<EntityID> Bloqueios;

	/*
	 * 
	 * Métodos Standard Agent
	 */
	
	public TesteRoteamento(int pp){
		super(pp);
	}
	
	@Override
	public String toString() {
		return "Sample fire brigade";
	}

	@Override
	protected void postConnect() {
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,
				StandardEntityURN.HYDRANT, StandardEntityURN.GAS_STATION);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		Logger.info("Sample fire brigade connected: max extinguish distance = "
				+ maxDistance + ", max power = " + maxPower + ", max tank = "
				+ maxWater);
		Bloqueios = new ArrayList<EntityID>();
		sectoring = new MASLABSectoring(model);
		
		sectoring.Setorizar();
		MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(sectoring);
		PreProcess.GerarArquivos();
		//MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(model);
		//PreProcess.CarregarArquivo();
		//sectoring = PreProcess.getMASLABSectoring();
		
		System.out.println("OK");
	}

	
	
	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {

		debug(time, routing
				.Abastecer(me().getPosition(), Bloqueios, Setores.S1)
				.toString());
		debug(time,
				routing.Combater(me().getPosition(), new EntityID(958),
						Bloqueios, Setores.S1, Setores.S2).toString());
		debug(time, routing.Resgatar(me().getPosition(), Bloqueios, Setores.S1)
				.toString());
		debug(time,
				routing.Resgatar(me().getPosition(), new EntityID(958),
						Bloqueios, Setores.S1).toString());
		
		debug(time,"Importance of sector NE:" + 
				sectoring.getSectorImportance(sectoring.NORTH_EAST, sectoring.AMBULANCE_TEAM)	
		);
		
		debug(time,"Importance of sector NW:" + 
				sectoring.getSectorImportance(sectoring.NORTH_WEST, sectoring.AMBULANCE_TEAM)	
		);
		
		debug(time,"Importance of sector SE:" + 
				sectoring.getSectorImportance(sectoring.SOUTH_EAST, sectoring.AMBULANCE_TEAM)	
		);
		
		debug(time,"Importance of sector SW:" + 
				sectoring.getSectorImportance(sectoring.SOUTH_WEST, sectoring.AMBULANCE_TEAM)	
		);
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
		return search.breadthFirstSearch(me().getPosition(),
				objectsToIDs(targets));
	}
	/*
	 * 
	 * Métodos Definidos por nós
	 */
	/*
	 * 
	 * Métodos Acessores se necessário
	 */
}
