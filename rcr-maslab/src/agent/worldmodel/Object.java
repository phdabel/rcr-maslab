package agent.worldmodel;

import java.io.Serializable;

public enum Object implements Serializable {
	
	BUILDING_FIRE("Building on Fire"),
	BLOCKADE("Blockade"),
	RESCUE("Civillian Rescue");
	
	String name;
	
	private Object(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
