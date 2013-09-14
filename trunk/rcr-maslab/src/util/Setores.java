package util;

public class Setores {
	
	/* LEGENDA MAPA:         
	 *   
	 *            ______*______
	 *           | 4-NO | 1-NE |
	 *   		*|-------------|*  
	 *           | 3-So | 2-SE |
	 *           ---------------
	 *                  *
	 */
	
	public static final int S1 = 1;
	public static final int S2 = 2;
	public static final int S3 = 3;
	public static final int S4 = 4;
	
	public static final int PRINCIPAL = 5;
	public static final int SECUNDARIAS = 6;
	public static final int UNDEFINED_SECTOR = 0;
	
	//defines names for the sectors (easier to remember)
	public static final int NORTH_EAST = S1;
	public static final int SOUTH_EAST = S2;
	public static final int SOUTH_WEST = S3;
	public static final int NORTH_WEST = S4;
	
	//puts sectors in array to ease traversing
	public static final int[] SECTORS = {NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST};

}
