package agent.worldmodel;

import java.io.Serializable;


public class Task implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private Object object;
	private Integer position;
	private int numberTokens;
	
	public Task(Integer id)
	{
		this.setId(id);
	}
	
	public Task(Integer id, Object object, Integer position)
	{
		this.setId(id);
		this.setObject(object);
		this.setPosition(position);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public int getNumberTokens() {
		return numberTokens;
	}

	public void setNumberTokens(int numberTokens) {
		this.numberTokens = numberTokens;
	}
	

}
