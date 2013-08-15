package util;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	private static final int NORTH_EAST = 1;
	private static final int SOUTH_EAST = 2;
	private static final int SOUTH_WEST = 3;
	private static final int NORTH_WEST = 4;
	private static final int UNDEFINED_SECTOR = 0;

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

	private Polygon SetorNordeste = new Polygon();
	private Polygon SetorSudeste = new Polygon();
	private Polygon SetorSudoeste = new Polygon();
	private Polygon SetorNoroeste = new Polygon();

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

		System.out.println(model.getEntity(idNorte));
		System.out.println(model.getEntity(idSul));
		System.out.println(model.getEntity(idLeste));
		System.out.println(model.getEntity(idOeste));
		
		Avenue_NtoS = search.breadthFirstSearch(idNorte,
				new ArrayList<EntityID>(), idSul, false);
		Avenue_LtoO = search.breadthFirstSearch(idLeste,
				new ArrayList<EntityID>(), idOeste, false);
		System.out.println(Avenue_NtoS);
		System.out.println(Avenue_LtoO);

		CarregaGrafoPrincipal();
		CarregaGrafosSetores();
		debug();
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
		Set<EntityID> v = new HashSet<EntityID>();
		Set<EntityID> v2 = new HashSet<EntityID>();

		Avenue_NtoS.add(0, idNorte);
		for (EntityID next : Avenue_NtoS) {
			v = g.get(next);
			v2 = new HashSet<EntityID>();

			for (EntityID t : v) {
				if (Avenue_NtoS.contains(t) || Avenue_LtoO.contains(t))
					v2.add(t);
			}
			MapPrincipal.put(next, v2);
		}

		Avenue_LtoO.add(0, idLeste);
		for (EntityID next : Avenue_LtoO) {
			if (!MapPrincipal.containsKey(next)) {
				v = g.get(next);
				v2 = new HashSet<EntityID>();
				for (EntityID t : v) {
					if (Avenue_LtoO.contains(t) || Avenue_NtoS.contains(t))
						v2.add(t);
				}
				MapPrincipal.put(next, v2);
			}
		}
	}

	/*
	 * 
	 * S1: Nordeste; S2: Sudeste; S3: Sudoeste; S4: Noroeste.
	 */
	private void CarregaGrafosSetores() {
		MASLABBFSearch MapP = new MASLABBFSearch(MapPrincipal);

		Map<EntityID, Set<EntityID>> searchP = MapP.getGraph();

		List<EntityID> roadIDss = new ArrayList<EntityID>();
		List<EntityID> buildingIDs = new ArrayList<EntityID>();
		List<EntityID> refugeIDs = new ArrayList<EntityID>();
		List<EntityID> hydrantIDs = new ArrayList<EntityID>();
		List<EntityID> p = new ArrayList<EntityID>();
		List<EntityID> aux = new ArrayList<EntityID>();

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

		DemarcarRegioes(1);
		DemarcarRegioes(2);
		DemarcarRegioes(3);
		DemarcarRegioes(4);

		for (EntityID next : roadIDss) {
			Road r = (Road) model.getEntity(next);
			addGrafoSetor(next, getSetorPertencente(r.getX(), r.getY()));
		}
		for (EntityID next : buildingIDs) {
			Building r = (Building) model.getEntity(next);
			addGrafoSetor(next, getSetorPertencente(r.getX(), r.getY()));
		}
		for (EntityID next : refugeIDs) {
			Refuge r = (Refuge) model.getEntity(next);
			addGrafoSetor(next, getSetorPertencente(r.getX(), r.getY()));
		}
		for (EntityID next : hydrantIDs) {
			Hydrant r = (Hydrant) model.getEntity(next);
			addGrafoSetor(next, getSetorPertencente(r.getX(), r.getY()));
		}

		// Adiciona a parte correspondente da via principal
		p = GetDivisionLane(1);
		aux = new ArrayList<EntityID>(MapSetor1.keySet());
		for (EntityID e : p) {
			if (!aux.contains(e))
				MapSetor1.put(e, searchP.get(e));
		}

		p = GetDivisionLane(2);
		aux = new ArrayList<EntityID>(MapSetor2.keySet());
		for (EntityID e : p) {
			if (!aux.contains(e))
				MapSetor2.put(e, searchP.get(e));
		}

		p = GetDivisionLane(3);
		aux = new ArrayList<EntityID>(MapSetor3.keySet());
		for (EntityID e : p) {
			if (!aux.contains(e))
				MapSetor3.put(e, searchP.get(e));
		}

		p = GetDivisionLane(4);
		aux = new ArrayList<EntityID>(MapSetor4.keySet());
		for (EntityID e : p) {
			if (!aux.contains(e))
				MapSetor4.put(e, searchP.get(e));
		}
	}

	private void addGrafoSetor(EntityID e, int setor) {
		Map<EntityID, Set<EntityID>> g = search.getGraph();
		if (setor == 1) {
			MapSetor1.put(e, g.get(e));
		} else if (setor == 2) {
			MapSetor2.put(e, g.get(e));
		} else if (setor == 3) {
			MapSetor3.put(e, g.get(e));
		} else if (setor == 4) {
			MapSetor4.put(e, g.get(e));
		}
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

		int[] xs = SetorNordeste.xpoints;
		int[] ys = SetorNordeste.ypoints;
		System.out.println("Nordeste");
		for (int i = 0; i < xs.length; i++) {
			System.out.println(xs[i] + " " + ys[i] + " - " + i);
		}

		xs = SetorSudeste.xpoints;
		ys = SetorSudeste.ypoints;
		System.out.println("Sudeste");
		for (int i = 0; i < xs.length; i++) {
			System.out.println(xs[i] + " " + ys[i] + " - " + i);
		}

		xs = SetorSudoeste.xpoints;
		ys = SetorSudoeste.ypoints;
		System.out.println("Sudoeste");
		for (int i = 0; i < xs.length; i++) {
			System.out.println(xs[i] + " " + ys[i] + " - " + i);
		}

		xs = SetorNoroeste.xpoints;
		ys = SetorNoroeste.ypoints;
		System.out.println("Noroeste");
		for (int i = 0; i < xs.length; i++) {
			System.out.println(xs[i] + " " + ys[i] + " - " + i);
		}
	}

	private int getSetorPertencente(double X, double Y) {
		if (SetorNordeste.contains(X, Y)) {
			return NORTH_EAST;
		} else if (SetorSudeste.contains(X, Y)) {
			return SOUTH_EAST;
		} else if (SetorSudoeste.contains(X, Y)) {
			return SOUTH_WEST;
		} else if (SetorNoroeste.contains(X, Y)) {
			return NORTH_WEST;
		} else
			return UNDEFINED_SECTOR;
	}

	private void DemarcarRegioes(int sector) {

		List<EntityID> division = new ArrayList<EntityID>();
		List<Integer> xs = new ArrayList<Integer>();
		List<Integer> ys = new ArrayList<Integer>();

		if (sector == 1) {
			System.out.println("Nordeste");
			division = GetDivisionLane(1);
			xs.add((int) coordinate_MaxX);
			ys.add((int) coordinate_MaxY);
			Road r2 = null; // Road aux
			int auxpoint = 0;
			for (EntityID next : division) {
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if (auxpoint == 0) {
					xs.add((int) r.getX());
					ys.add((int) coordinate_MaxY);
					auxpoint = 1;
				}
				xs.add((int) r.getX());
				ys.add((int) r.getY());
			}
			xs.add((int) coordinate_MaxX);
			ys.add((int) r2.getY());

			SetorNordeste = new Polygon(toIntArray(xs), toIntArray(ys),
					xs.size());
			// SetorNordeste.addPoint((int)coordinate_MaxX,
			// (int)coordinate_MaxY);

		} else

		// Gera a Região S2: Sudeste;
		if (sector == 2) {
			division = GetDivisionLane(2);
			xs.add((int) coordinate_MaxX);
			ys.add((int) coordinate_MinY);

			Road r2 = null; // Road aux
			int auxpoint = 0;
			for (EntityID next : division) {
				System.out.println(next.getValue());
				System.out.println(model.getEntity(next).getClass());
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if (auxpoint == 0) {

					xs.add((int) coordinate_MaxX);
					ys.add((int) r.getY());

					auxpoint = 1;
				}
				xs.add((int) r.getX());
				ys.add((int) r.getY());
			}
			xs.add((int) r2.getX());
			ys.add((int) coordinate_MinY);
			SetorSudeste = new Polygon(toIntArray(xs), toIntArray(ys),
					xs.size());
			// SetorSudeste.addPoint((int)coordinate_MaxX,
			// (int)coordinate_MinY);

		} else

		// Gera a Região S3: Sudoeste;
		if (sector == 3) {
			division = GetDivisionLane(3);
			int auxpoint = 0;
			xs.add((int) coordinate_MinX);
			ys.add((int) coordinate_MinY);
			Road r2 = null; // Road aux
			for (EntityID next : division) {
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if (auxpoint == 0) {
					xs.add((int) r.getX());
					ys.add((int) coordinate_MinY);
					SetorSudoeste.addPoint((int) r.getX(),
							(int) coordinate_MinY);
					auxpoint = 1;
				}
				xs.add((int) r.getX());
				ys.add((int) r.getY());
			}
			xs.add((int) coordinate_MinX);
			ys.add((int) r2.getY());
			SetorSudoeste = new Polygon(toIntArray(xs), toIntArray(ys),
					xs.size());
			// SetorSudoeste.addPoint((int)coordinate_MinX,
			// (int)coordinate_MinY);

		} else

		// Gera a Região S4: Noroeste;
		if (sector == 4) {
			division = GetDivisionLane(4);
			xs.add((int) coordinate_MinX);
			ys.add((int) coordinate_MaxY);
			Road r2 = null; // Road aux
			int auxpoint = 0;
			for (EntityID next : division) {
				Road r = (Road) model.getEntity(next);
				r2 = r;
				if (auxpoint == 0) {
					xs.add((int) coordinate_MinX);
					ys.add((int) r.getY());
					auxpoint = 1;
				}
				xs.add((int) r.getX());
				ys.add((int) r.getY());
			}
			xs.add((int) r2.getX());
			ys.add((int) coordinate_MaxY);
			SetorNoroeste = new Polygon(toIntArray(xs), toIntArray(ys),
					xs.size());
			// SetorNoroeste.addPoint((int)coordinate_MinX,
			// (int)coordinate_MaxY);
		}
		/*
		 * int[] xss = SetorNordeste.xpoints; int[] yss = SetorNordeste.ypoints;
		 * System.out.println("Nordeste"); for(int i = 0; i < xss.length; i++){
		 * System.out.println(xss[i] + " " + yss[i] + " - " + i); }
		 * 
		 * xss = SetorSudeste.xpoints; yss = SetorSudeste.ypoints;
		 * System.out.println("Sudeste"); for(int i = 0; i < xss.length; i++){
		 * System.out.println(xss[i] + " " + yss[i] + " - " + i); }
		 * 
		 * xss = SetorSudoeste.xpoints; yss = SetorSudoeste.ypoints;
		 * System.out.println("Sudoeste"); for(int i = 0; i < xss.length; i++){
		 * System.out.println(xss[i] + " " + yss[i] + " - " + i); }
		 * 
		 * xss = SetorNoroeste.xpoints; yss = SetorNoroeste.ypoints;
		 * System.out.println("Noroeste"); for(int i = 0; i < xss.length; i++){
		 * System.out.println(xss[i] + " " + yss[i] + " - " + i); }
		 */

	}

	/*
	 * Gera os limites dos setores (Busca em Largura na via principal)
	 */
	private List<EntityID> GetDivisionLane(int sector) {

		System.out.println(MapPrincipal.keySet());
		MASLABBFSearch bfSearch = new MASLABBFSearch(MapPrincipal);

		if (sector == 1) {
			return bfSearch.breadthFirstSearch(idNorte, idLeste);
		}
		if (sector == 2) {
			return bfSearch.breadthFirstSearch(idLeste, idSul);
		}
		if (sector == 3) {
			return bfSearch.breadthFirstSearch(idSul, idOeste);
		}
		if (sector == 4) {
			return bfSearch.breadthFirstSearch(idOeste, idNorte);
		} else {
			return null;
		}
	}

	int[] toIntArray(List<Integer> list) {
		int[] ret = new int[list.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = list.get(i);
		return ret;
	}
}
