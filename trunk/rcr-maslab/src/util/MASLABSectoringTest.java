package util;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/* LEGENDA MAPA:         
 *   
 *               idNorte    
 *            ______*______
 *           | 4-NO | 1-NE |
 *  idOeste *|-------------|* idLeste 
 *           | 3-So | 2-SE |
 *           ---------------
 *                  *
 *                 idSul
 *    
 *     idNorte = MelhorDistancia(CentroX,MaxY)
 *     idSul   = MelhorDistancia(CentroX,MinY)
 *     idLeste = MelhorDistancia(CentroY,MaxX)
 *     idOeste = MelhorDistancia(CentroY,MinX)
 *               
 */
public class MASLABSectoringTest {

	private static double coordinate_MaxX = 0;
	private static double coordinate_MaxY = 0;
	private static double coordinate_MinX = 0;
	private static double coordinate_MinY = 0;
	private static double coordinate_CenterX = 0;
	private static double coordinate_CenterY = 0;

	private List<EntityID> Avenue_NtoS;
	private List<EntityID> Avenue_LtoO;

	Map<EntityID, Set<EntityID>> MapSetor1;
	Map<EntityID, Set<EntityID>> MapSetor2;
	Map<EntityID, Set<EntityID>> MapSetor3;
	Map<EntityID, Set<EntityID>> MapSetor4;
	Map<EntityID, Set<EntityID>> MapPrincipal;

	private static EntityID idNorte;
	private static EntityID idSul;
	private static EntityID idLeste;
	private static EntityID idOeste;

	private StandardWorldModel model;

	MASLABBFSearch search;

	protected List<Road> roadIDs;

	public MASLABSectoringTest(StandardWorldModel world) {
		model = world;

		MapSetor1 = new HashMap<EntityID, Set<EntityID>>();
		MapSetor2 = new HashMap<EntityID, Set<EntityID>>();
		MapSetor3 = new HashMap<EntityID, Set<EntityID>>();
		MapSetor4 = new HashMap<EntityID, Set<EntityID>>();
		MapPrincipal = new HashMap<EntityID, Set<EntityID>>();

		/* Obtem especificação geral do mapa */
		coordinate_MaxX = model.getBounds().getMaxX();
		coordinate_MaxY = model.getBounds().getMaxY();
		coordinate_MinX = model.getBounds().getMinX();
		coordinate_MinY = model.getBounds().getMinY();
		coordinate_CenterX = model.getBounds().getCenterX();
		coordinate_CenterY = model.getBounds().getCenterY();

		roadIDs = new ArrayList<Road>();

		for (StandardEntity next : model) {
			if (next instanceof Road) {
				roadIDs.add((Road) next);
			}
		}

		search = new MASLABBFSearch(model);

		/* Obtem os nodes primordiais */

		idNorte = EntMaisProximo(coordinate_CenterX, coordinate_MaxY);
		idSul = EntMaisProximo(coordinate_CenterX, coordinate_MinY);
		idLeste = EntMaisProximo(coordinate_MaxX, coordinate_CenterY);
		idOeste = EntMaisProximo(coordinate_MinX, coordinate_CenterY);

		Avenue_NtoS = search.breadthFirstSearch(idNorte,
				new ArrayList<EntityID>(), idSul);
		Avenue_LtoO = search.breadthFirstSearch(idLeste,
				new ArrayList<EntityID>(), idOeste);

		CarregaGrafoPrincipal();

		CarregaGrafosSetores();

	}

	/**
	 * 
	 * @param Px
	 *            Coordenada X (double)
	 * @param Py
	 *            Coordenada Y (double)
	 * @return EntityID contendo o nó mais proximo dos pontos passados
	 */
	private EntityID EntMaisProximo(double Px, double Py) {
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
	 * @param x1
	 *            Px0
	 * @param y1
	 *            Py0
	 * @param x2
	 *            Px1
	 * @param y2
	 *            Py1
	 * @return Distancia Euclidiana dos pontos
	 */
	private double DistanciaEuclidiana(double x1, double y1, double x2,
			double y2) {
		return Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)));
	}

	/**
	 * Obtem o grafo das vias principais
	 * 
	 * @return Grafo das vias principais
	 */
	private void CarregaGrafoPrincipal() {
		Map<EntityID, Set<EntityID>> g = search.getGraph();

		for (EntityID next : Avenue_NtoS) {
			MapPrincipal.put(next, g.get(next));
		}
		for (EntityID next : Avenue_LtoO) {
			if (!MapPrincipal.containsKey(next))
				MapPrincipal.put(next, g.get(next));
		}
	}

	/*
	 * 
	 * S1: Nordeste; S2: Sudeste; S3: Sudoeste; S4: Noroeste.
	 */
	private void CarregaGrafosSetores() {

		List<EntityID> roadIDss = new ArrayList<EntityID>();
		List<EntityID> buildingIDs = new ArrayList<EntityID>();
		List<EntityID> refugeIDs = new ArrayList<EntityID>();
		List<EntityID> hydrantIDs = new ArrayList<EntityID>();
		for (StandardEntity next : model) {
			if (next instanceof Road) {
				roadIDss.add(next.getID());
			}
			if (next instanceof Building) {
				buildingIDs.add(next.getID());
			}
			if (next instanceof Refuge) {
				refugeIDs.add(next.getID());
			}
			if (next instanceof Hydrant) {
				hydrantIDs.add(next.getID());
			}
		}

		List<EntityID> Setor1 = new ArrayList<EntityID>();
		List<EntityID> Setor2 = new ArrayList<EntityID>();
		List<EntityID> Setor3 = new ArrayList<EntityID>();
		List<EntityID> Setor4 = new ArrayList<EntityID>();

		List<EntityID> Principal = new ArrayList<EntityID>(
				MapPrincipal.keySet());
		System.out.println(Principal.toString());
		List<EntityID> validador = new ArrayList<EntityID>(
				MapPrincipal.keySet());

		for (EntityID p : Principal) {
			List<EntityID> Nodos = new ArrayList<EntityID>(search.getGraph()
					.get(p));
			Road comparador = (Road) model.getEntity(p);

			for (EntityID next : Nodos) {

				if (roadIDss.contains(next)) {
					Road r = (Road) model.getEntity(next);
					// Se o vizinho estiver a esquerda e acima (e não fizer
					// parte da via principal) pertence ao setor 4 então
					if (r.getX() <= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor4.contains(next))
							Setor4.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e acima (e não fizer
						// parte da via principal) pertence ao setor 1 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor1.contains(next))
							Setor1.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a esquerda e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() <= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor3.contains(next))
							Setor3.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor2.contains(next))
							Setor2.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
					}
				} else if (buildingIDs.contains(next)) {
					Building r = (Building) model.getEntity(next);
					// Se o vizinho estiver a esquerda e acima (e não fizer
					// parte da via principal) pertence ao setor 4 então
					if (r.getX() <= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor4.contains(next))
							Setor4.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e acima (e não fizer
						// parte da via principal) pertence ao setor 1 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor1.contains(next))
							Setor1.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a esquerda e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() <= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor3.contains(next))
							Setor3.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor2.contains(next))
							Setor2.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
					}
				} else if (hydrantIDs.contains(next)) {
					Hydrant r = (Hydrant) model.getEntity(next);
					// Se o vizinho estiver a esquerda e acima (e não fizer
					// parte da via principal) pertence ao setor 4 então
					if (r.getX() <= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor4.contains(next))
							Setor4.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e acima (e não fizer
						// parte da via principal) pertence ao setor 1 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor1.contains(next))
							Setor1.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a esquerda e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() <= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor3.contains(next))
							Setor3.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor2.contains(next))
							Setor2.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
					}
				} else if (refugeIDs.contains(next)) {
					Refuge r = (Refuge) model.getEntity(next);
					// Se o vizinho estiver a esquerda e acima (e não fizer
					// parte da via principal) pertence ao setor 4 então
					if (r.getX() <= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor4.contains(next))
							Setor4.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e acima (e não fizer
						// parte da via principal) pertence ao setor 1 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() >= comparador.getY()) {
						if (!Setor1.contains(next))
							Setor1.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a esquerda e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() <= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor3.contains(next))
							Setor3.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
						// Se o vizinho estiver a direita e abaixo (e não fizer
						// parte da via principal) pertence ao setor 3 então
					} else if (r.getX() >= comparador.getX()
							&& r.getY() <= comparador.getY()) {
						if (!Setor2.contains(next))
							Setor2.addAll(search.CompleteBreadthFirstSearch(
									r.getID(), Principal));
					}
				}

			}
		}

		// Depois de obter os nodos de cada lado, monta os grafos
		MapSetor1 = getGrafo(Setor1);
		MapSetor2 = getGrafo(Setor2);
		MapSetor3 = getGrafo(Setor3);
		MapSetor4 = getGrafo(Setor4);

		System.out.println(MapSetor1.keySet().toString());
		System.out.println(MapSetor2.keySet().toString());
		System.out.println(MapSetor3.keySet().toString());
		System.out.println(MapSetor4.keySet().toString());
	}

	private Map<EntityID, Set<EntityID>> getGrafo(List<EntityID> nodos) {
		Map<EntityID, Set<EntityID>> g = search.getGraph();
		Map<EntityID, Set<EntityID>> aux = new HashMap<EntityID, Set<EntityID>>();

		for (EntityID next : nodos) {
			aux.put(next, g.get(next));
		}
		return aux;
	}

	private void debug() {
		Integer a[] = { 36495, 36202, 36205, 36373, 36207, 36183, 35966, 35963,
				35960, 35958, 35954, 35951, 35948, 35946, 35942, 35939, 35936,
				35932, 35930, 4731, 2382, 7359, 4524, 34584, 6450, 33986,
				33987, 33976, 34513, 5793, 34462, 9051, 4389, 6018, 34565,
				2000, 6378, 34562, 9375, 5883, 3345, 6720, 1976, 2535, 7206,
				892, 3471, 8790, 5775, 1504, 4146, 34753 };
		Integer b[] = { 30883, 30884, 30859, 30860, 8610, 4668, 7458, 3183,
				445, 3840, 7809, 2922, 4695, 7098, 32572, 32571, 2051, 8250,
				31201, 34241, 34240, 4461, 6567, 1596, 3678, 5127, 2643, 35344,
				35345, 2841, 34222, 34223, 34224, 3021, 32767, 32765, 34279,
				8727, 2000, 34565, 6018, 4389, 9051, 34462, 5793, 34513, 33976,
				33987, 2508, 7215, 25835, 34654, 33968, 8925, 5766, 4128, 4173,
				352, 4155, 33357, 4956, 33169, 33179, 33190, 33191, 33234,
				33282, 33431, 33447 };

		List<Integer> Lista1 = Arrays.asList(a);
		List<Integer> Lista2 = Arrays.asList(b);

		List<Integer> ListaMapa = Arrays.asList(4128, 6567, 4146, 4389, 4155,
				9051, 34654, 33282, 32571, 8790, 32572, 7098, 4668, 2508, 3471,
				7359, 892, 4956, 1596, 6378, 7809, 4695, 4461, 33357, 3021,
				4173, 2535, 25835, 5127, 34584, 8727, 36495, 34565, 352, 34562,
				4731, 8250, 33447, 33169, 33179, 35932, 31201, 33987, 33986,
				35930, 36183, 6450, 2051, 4524, 9375, 34241, 34240, 35958,
				2000, 35954, 445, 3678, 35966, 36205, 36207, 35963, 35345,
				35344, 36202, 35960, 35942, 3183, 33431, 35939, 1504, 33191,
				35936, 33190, 8925, 5883, 34513, 35951, 35948, 34753, 34279,
				35946, 2841, 5766, 33234, 2382, 2922, 5775, 7458, 1976, 6720,
				6018, 7215, 30884, 3345, 7206, 30883, 36373, 2643, 5793, 34224,
				3840, 34462, 33976, 30860, 30859, 34223, 34222, 32767, 8610,
				32765, 33968);

		// Verifica se todos da listaavenidas estão na listamapa

		for (Integer v : Lista1) {
			if (!ListaMapa.contains(v)) {
				System.out.println(v + " não está na lista do mapa");
			}
		}
		for (Integer v : Lista2) {
			if (!ListaMapa.contains(v)) {
				System.out.println(v + " não está na lista do mapa");
			}
		}

		for (Integer v : ListaMapa) {
			if (!Lista1.contains(v) && !Lista2.contains(v)) {
				System.out.println(v + " não está na lista da avenida");
			}
		}

	}

	
	private boolean isPointInPolygon(double X, double Y, int sector) {
		
		List<EntityID> division =  new LinkedList<EntityID>();
		double xpoints[] = {100,100,200,200}; //Square      
		double ypoints[] = {100,200,200,100};
		
		Polygon poly = new Polygon();
		int auxpoint = 0;
		// Gera a Região S1: Nordeste;
		if (sector == 1) {
			division = GetDivisionLane(1);
			poly.addPoint((int)coordinate_MaxX, (int)coordinate_MaxY);
			Road r2 = null; // Road aux
			for(EntityID next: division){
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if(auxpoint == 0){
					poly.addPoint((int)r.getX(),(int) coordinate_MaxY);
					auxpoint =1;					
				}
				poly.addPoint((int)r.getX(),(int) r.getY());				
			}
			poly.addPoint((int) coordinate_MaxX, (int)r2.getY());
			poly.addPoint((int)coordinate_MaxX, (int)coordinate_MaxY);

		} else

		// Gera a Região S2: Sudeste;
		if (sector == 2) {
			
			division = GetDivisionLane(2);
			poly.addPoint((int)coordinate_MaxX, (int)coordinate_MinY);
			Road r2 = null; // Road aux
			for(EntityID next: division){
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if(auxpoint == 0){
					poly.addPoint((int)r.getX(),(int) coordinate_MaxY);
					auxpoint =1;					
				}
				poly.addPoint((int)r.getX(),(int) r.getY());				
			}
			poly.addPoint((int) coordinate_MaxX, (int)r2.getY());
			poly.addPoint((int)coordinate_MaxX, (int)coordinate_MinY);
			
		} else

		// Gera a Região S3: Sudoeste;
		if (sector == 3) {
			

		} else

		// Gera a Região S4: Noroeste;
		if (sector == 4) {
			
		}
		else return false;
			

		if(poly.contains(X,Y))
			return true;
		else
			return false;
		
		
	}

	
	/*
	 *  Gera os limites dos setores (Busca em Largura)
	 */
	private List<EntityID> GetDivisionLane(int sector) {
		
		EntityID start = null;
		EntityID next = null;
		Collection<EntityID> goals  = new LinkedList<EntityID>();
		List<EntityID> open = new LinkedList<EntityID>();
	    Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
	    boolean found = false;
		
	    if(sector == 1){
			start = idNorte;
			goals.add(idLeste);
		}
		if(sector == 2){
			start = idLeste;
			goals.add(idSul);
		}
		if(sector == 3){
			start = idSul;
			goals.add(idOeste);
		}
		if(sector == 4){
			start = idOeste;
			goals.add(idNorte);
		}
			 
	    open.add(start);
	     
	    ancestors.put(start, start);
	    do {
	           next = open.remove(0);
	           if (isGoal(next, goals)) {
	               found = true;
	               break;
	            }
		            Collection<EntityID> neighbours = MapPrincipal.get(next);
		            if (neighbours.isEmpty()) {
		                continue;
		            }
		            for (EntityID neighbour : neighbours) {
		                if (isGoal(neighbour, goals)) {
		                    ancestors.put(neighbour, next);
		                    next = neighbour;
		                    found = true;
		                    break;
		                }
		                else {
		                    if (!ancestors.containsKey(neighbour)) {
		                        open.add(neighbour);
		                        ancestors.put(neighbour, next);
		                    }
		                }
		            }
	        } while (!found && !open.isEmpty());
	        
	    	if (!found) {
	            return null;
	        }
	        
	        EntityID current = next;
	        List<EntityID> path = new LinkedList<EntityID>();
	        do {
	            path.add(0, current);
	            current = ancestors.get(current);
	            if (current == null) {
	                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
	            }
	        } while (current != start);
	
		return path;
	}
	
	  private boolean isGoal(EntityID e, Collection<EntityID> test) {
	        return test.contains(e);
	    }

}
