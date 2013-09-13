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
import rescuecore2.standard.entities.Road;
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
//		MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(sectoring);
//		PreProcess.GerarArquivos();
		MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(model);
		PreProcess.CarregarArquivo();
		//sectoring = PreProcess.getMASLABSectoring();
	}

	
	
	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Sunbscribe to channel 1
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
		}
		for (Command next : heard) {
			Logger.debug("Heard " + next);
		}
		
		int teste = rand.nextInt(100);
		int x=0, y=0;
		StandardEntity se = model.getEntity(me().getPosition()); 
		if(se instanceof Road){
			Road r = (Road)se;
			x = r.getX();
			y = r.getY();
		}else if(se instanceof Building){
			Building b = (Building)se;
			x = b.getX();
			y = b.getY();
		}

		int s1 = sectoring.getSetorPertencente(x,y);
		
		List<EntityID> path = new ArrayList<EntityID>();
		System.out.println(me().getID().getValue() + " Posicao: " + me().getPosition().getValue());
		
		if(teste <= 50){

			System.out.println(me().getID().getValue() + " Explorar no mesmo setor: " + s1);
			path = routing.Explorar(me().getPosition(), s1 , new ArrayList<EntityID>());
			
		}else if(teste <= 100){
			int s2 = rand.nextInt(4); 
			while(s2 == 0 || s2 == 1){
				s2 = rand.nextInt(4);
			}
			
			System.out.println(me().getID().getValue() + " Explorar em outro setor. De: " + s1 + " para " + s2);
			path = routing.Explorar(me().getPosition(), s2 , new ArrayList<EntityID>());

		}/*else if(teste <= 60){
			System.out.println(me().getID().getValue() + "Refugio a partir de: " + me().getPosition().getValue());
			path = routing.Resgatar(me().getPosition(), new ArrayList<EntityID>(), s1);

		}else if(teste <= 80){
			EntityID r = buildingIDs.get(rand.nextInt(buildingIDs.size() - 1));
			System.out.println(me().getID().getValue() + "Resgatar a partir de: " + me().getPosition().getValue() + " ate " + r.getValue());
			Building b = (Building)model.getEntity(r);
			int s2 = sectoring.getSetorPertencente(b.getX(), b.getY());
			path = routing.Resgatar(me().getPosition(), r, new ArrayList<EntityID>(), s1, s2);
			
		}*/
		
		System.out.println(me().getID().getValue() + "Caminho: " + path);
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
