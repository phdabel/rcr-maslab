package agent.worldmodel;

import rescuecore2.worldmodel.EntityID;

public class AgentState {
	
	private String state;
	private EntityID id;
	private int x,y;
	
	public AgentState(String s){
		this.setState(s);
		this.setId(null);
	}
	
	public AgentState(String s, int x, int y)
	{
		this.setState(s);
		this.setX(x);
		this.setY(y);
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

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

}
