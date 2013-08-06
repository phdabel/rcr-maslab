package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import maps.convert.osm2gml.buildings.row.ThinDuplexRowFiller;
import rescuecore.objects.World;
import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.components.StandardViewer;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

/* LEGENDA MAPA:         
 *   
 *             idNorte    
 *            ____*_____
 *           | NO | NE |
 *  idOeste *|---------|* idLeste 
 *           | So | SE |
 *           -----------
 *                *
 *              idSul
 *    
 *     idNorte = MelhorDistancia(CentroX,MaxY)
 *     idSul   = MelhorDistancia(CentroX,MinY)
 *     idLeste = MelhorDistancia(CentroY,MaxX)
 *     idOeste = MelhorDistancia(CentroY,MinX)
 *               
 */
public abstract class MASLABSectoringTest extends StandardAgent<Road> {

	private static double coordinate_MaxX = 0;
	private static double coordinate_MaxY = 0;
	private static double coordinate_MinX = 0;
	private static double coordinate_MinY = 0;
	private static double coordinate_CenterX = 0;
	private static double coordinate_CenterY = 0;

	private List<EntityID> SectorNO;
	private List<EntityID> SectorNE;
	private List<EntityID> SectorSO;
	private List<EntityID> SectorSE;
	private List<EntityID> Avenue;
	
	private static EntityID idNorte;
	private static EntityID idSul;
	private static EntityID idLeste;
	private static EntityID idOeste;
	

	protected List<Road> roadIDs;

	public MASLABSectoringTest() {
	}

	protected void postConnect() {
		super.postConnect();
		/* Obtem especificação geral do mapa*/
		coordinate_MaxX = this.model.getBounds().getMaxX();
		coordinate_MaxY = this.model.getBounds().getMaxY();
		coordinate_MinX = this.model.getBounds().getMinX();
		coordinate_MinY = this.model.getBounds().getMinY();
		coordinate_CenterX = this.model.getBounds().getCenterX();
		coordinate_CenterY = this.model.getBounds().getCenterY();

		roadIDs = new ArrayList<Road>();

		for (StandardEntity next : model) {
			if (next instanceof Road) {
				roadIDs.add((Road) next);
			}
		}

		/* Obtem os nodes primordiais*/
		
		idNorte = EntMaisProximo(coordinate_CenterX, coordinate_MaxY);
		idSul = EntMaisProximo(coordinate_CenterX, coordinate_MinY);
		idLeste = EntMaisProximo(coordinate_CenterY, coordinate_MaxX);
		idOeste = EntMaisProximo(coordinate_CenterY, coordinate_MinX);
		
	}

	/**
	 * 
	 * @param Px
	 *            Cordenada X (double)
	 * @param Py
	 *            Cordenada Y (double)
	 * @return EntityID contendo o nó mais proximo dos pontos passados
	 */

	public EntityID EntMaisProximo(double Px, double Py) {
		Road bestNode = roadIDs.get(0);
		double melhorDist = DistanciaEuclidiana(Px, Py, bestNode.getX(),
				bestNode.getY());
		double auxdist = 0;
		for (Road next : roadIDs) {
			next.getX();
			auxdist = DistanciaEuclidiana(Px, Py, next.getX(), next.getY());
			if (melhorDist > auxdist) {
				bestNode = next;
				melhorDist = auxdist;
			}
		}
		return bestNode.getID();
	}

	/**
	 * 
	 * @param x1 Px0
	 * @param y1 Py0
	 * @param x2 Px1
	 * @param y2 Py1
	 * @return Distancia Euclidiana dos pontos
	 */
	public double DistanciaEuclidiana(double x1, double y1, double x2, double y2) {
		return Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)));
	}

	/**
	 * 
	 * @return A Avenida Principal do Mapa
	 */
	
	public Map<EntityID, Set<EntityID>> getAvenue() {
		
		BFSearch bfSearch =  new BFSearch(model);
		List<EntityID> Avenue_StoN = bfSearch.breadthFirstSearch(idNorte, idSul);
		List<EntityID> Avenue_LtoO = bfSearch.breadthFirstSearch(idLeste, idOeste);

        Map<EntityID, Set<EntityID>> g = bfSearch.getGraph();
        Map<EntityID, Set<EntityID>> MAPPrincipal = new HashMap<EntityID, Set<EntityID>>();
        
        for(EntityID next: Avenue_StoN){
        	MAPPrincipal.put(next, g.get(next));
        }

        for(EntityID next: Avenue_LtoO){
        	MAPPrincipal.put(next, g.get(next));
        }
        
        return MAPPrincipal;

    }

}
