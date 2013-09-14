package importance;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import agent.MASLABFireBrigade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import util.MASLABSectoring;
import util.Setores;

public class FireBrigadeImportance extends AbstractImportance {

	
	public FireBrigadeImportance(MASLABFireBrigade caller){
		super(caller);
	}
	
	/**
	 * Calculates the importance of a sector for the Fire Brigades
	 */
	@Override
	public void calculateImportances(boolean isPreProcessing) {
		
		StandardWorldModel worldModel = caller.getWorldModel();
		
		if (isPreProcessing){ //performs calculation with static information
			for (int sectorIndex : Setores.SECTORS){
				Map<EntityID, Set<EntityID>> sector = caller.getSectoringInfo().getMapSetor(sectorIndex);
				
				//importance is the total area of the buildings in the sector
				//plus the area on the range of a gas station
				int importance = 0;
				
				for (EntityID id : sector.keySet()){
					if(worldModel.getEntity(id) instanceof Building){
						Building b = (Building) caller.getWorldModel().getEntity(id);
						importance += b.getTotalArea();
						
						int xplosionRange = ((MASLABFireBrigade)caller).getGasStationRange();
						
						//if b is a GasStation, adds the areas of the buildings in its explosion range
						if (b instanceof GasStation){
							for (StandardEntity objInRange : worldModel.getObjectsInRange(id, xplosionRange)){
								if (objInRange instanceof Building){
									importance += ((Building)objInRange).getTotalArea();
								}
							}
						}//if b instanceof GasStation
					}//if getEntity(b) instanceof Building
				}//for id : sector.keySet()
				
				sectorImportances.put(sectorIndex, importance);
			}//for sectorIndex : Sectors
		} 
		else { //TODO: performs calculation with dynamic info.
			
		}

	}

}
