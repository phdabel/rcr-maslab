package agent.worldmodel;

import rescuecore2.worldmodel.EntityID;

public class AgentState {
	
	private String state;
	private EntityID id;
	
	public AgentState(String s){
		this.setState(s);
		this.setId(null);
	}
	
	public AgentState(String s, EntityID target)
	{
		this.setState(s);
		this.setId(target);
	}
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public EntityID getId() {
		return id;
	}
	public void setId(EntityID id) {
		this.id = id;
	}

}
