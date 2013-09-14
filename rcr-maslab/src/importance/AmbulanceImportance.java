package importance;

import java.util.Map;
import java.util.Set;

import agent.MASLABAmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import util.MASLABSectoring;
import util.Setores;

public class AmbulanceImportance extends AbstractImportance {
	
	public AmbulanceImportance(MASLABAmbulanceTeam caller) {
		super(caller);
	}

	@Override
	public void calculateImportances(boolean isPreProcessing) {
		if (isPreProcessing){ //performs calculation with static information
			
			for (int sectorIndex : Setores.SECTORS){
				Map<EntityID, Set<EntityID>> sector = caller.getSectoringInfo().getMapSetor(sectorIndex);
				
				int numbuildings = 0;
				for (EntityID id : sector.keySet()){
					if(caller.getWorldModel().getEntity(id) instanceof Building){
						numbuildings++;
					}
				}
				
				sectorImportances.put(sectorIndex, numbuildings);
			}
		} 
		else { //TODO: performs calculation with dynamic info.
			
		}
	}

}
