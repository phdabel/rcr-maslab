package importance;

import java.util.Map;
import java.util.Set;

import agent.MASLABPoliceForce;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import util.MASLABSectoring;
import util.Setores;

public class PoliceForceImportance extends AbstractImportance {

	/**
	 * Weights of the entities in the importance calculation
	 */
	public static final int REFUGE_WEIGHT = 3;
	public static final int HYDRANT_WEIGHT = 1;
	public static final int GAS_STATION_WEIGHT = 5;
	
	public PoliceForceImportance(MASLABPoliceForce caller) {
		super(caller);
	}

	@Override
	public void calculateImportances(boolean isPreProcessing) {
		if (isPreProcessing){ //performs calculation with static information
			for (int sectorIndex : Setores.SECTORS){
				Map<EntityID, Set<EntityID>> sector = caller.getSectoringInfo().getMapSetor(sectorIndex);
				
				int importance = 0;
				for (EntityID id : sector.keySet()){
					if(caller.getWorldModel().getEntity(id) instanceof Refuge){
						importance += REFUGE_WEIGHT;
					}
					
					if(caller.getWorldModel().getEntity(id) instanceof Hydrant){
						importance += HYDRANT_WEIGHT;
					}
					
					if (caller.getWorldModel().getEntity(id) instanceof GasStation){
						importance += GAS_STATION_WEIGHT;
					}
				}
				
				sectorImportances.put(sectorIndex, importance);
			}
		} 
		else { //TODO: performs calculation with dynamic info.
			
		}

	}

}
