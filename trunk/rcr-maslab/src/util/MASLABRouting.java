package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public final class MASLABRouting {
	
	private Map<EntityID, EntityID> Setor1;
	private Map<EntityID, EntityID> Setor2;
	private Map<EntityID, EntityID> Setor3;
	private Map<EntityID, EntityID> Setor4;
	private Map<EntityID, EntityID> Principais;
    private List<EntityID> refugeIDs;
    private List<EntityID> waterIDs;
    private List<EntityID> buildingIDs;
    protected BFSearch search;
    
    /**
     * @param s1 Grafo do setor 1
     * @param s2 Grafo do setor 2
     * @param s3 Grafo do setor 3
     * @param s4 Grafo do setor 4
     * @param p Grafo das Vias Principais
     * @param r Lista com os refúgios
     * @param w Lista com os refúgios e hidrantes
     * @param b Lista com as construções
     */
	public MASLABRouting(Map<EntityID, EntityID> s1, Map<EntityID, EntityID> s2, Map<EntityID, EntityID> s3, Map<EntityID, EntityID> s4,
			Map<EntityID, EntityID> p, List<EntityID> r, List<EntityID> w, List<EntityID> b, StandardWorldModel world){
		Setor1 = s1;
		Setor2 = s2;
		Setor3 = s3;
		Setor4 = s4;
		Principais = p;
		refugeIDs = r;
		waterIDs = w;
		buildingIDs = b;
		search = new BFSearch(world);
	}
	
	/**
	 * Identifica os setores:
	 * S1: Nordeste;
	 * S2: Sudeste;
	 * S3: Sudoeste;
	 * S4: Noroeste.
	 */
	public enum Setores{
		S1, S2, S3, S4;
	}
	
	/**
	 * Identifica os tipos das vias:
	 * Principal: Vias que delimitam os setores (Norte-Sul e Leste-Oeste);
	 * Secundario: Vias que ligam edifícios importantes e a via principal;
	 * Outros: Demais vias dos setores;
	 */
	public enum Tipos{
		Principal, Secundario, Outros;
	}
	
	/**
	 * Calcula o caminho até o Refúgio/Hidrante mais próximo
	 * @param Origem EntityID da origem do agente 
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Abastecer(EntityID Origem, List<EntityID> Bloqueios) {
		List<EntityID> path = search.breadthFirstSearch(Origem, Bloqueios, Principais.values());
		List<EntityID> pathaux = search.breadthFirstSearch(path.get(path.size()-1), Bloqueios, Principais.values());
		path.remove(path.size() -1);
		path.addAll(pathaux);
		return path;
	}

	/**
 	 * Calcula o caminho até a entrada do edifício.
	 * OBS.: Não entrará no edifício.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Destino EntityID do destino do agente (pode ser rua ou edifício)
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Combater(EntityID Origem, EntityID Destino, List<EntityID> Bloqueios) {
		
		return new ArrayList<EntityID>();
	}

	/**
	 * Define a rota de exploração aleatória - DESCREVER FUNÇÃO DO ALGORITMO
	 * OBS.: Não entrará em nenhum edifício.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Setor Setor que deseja ser explorado
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Explorar(EntityID Origem, Setores Setor, List<EntityID> Bloqueios) {
		
		return new ArrayList<EntityID>();
	}
	
	/**
	 * Define o caminho a ser percorrido e limpo pelo agente
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Setor Setor que deseja ser limpo
	 * @param Tipo Caso o Tipo seja Principal, limpará as vias principais do setor; Caso o Tipo seja Secundario, limpará os caminhos dos edifícios importantes até a via pincipal mais próxima; Caso o Tipo seja Outros, limpará qualquer caminho dentro do setor (não usárá as vias principais/secundárias).
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Limpar(EntityID Origem, Setores Setor, Tipos Tipo, List<EntityID> Bloqueios) {
		
		return new ArrayList<EntityID>();
	}	

	/**
	 * Calcula a rota até o refúgio mais próximo
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Resgatar(EntityID Origem, List<EntityID> Bloqueios) {
		
		return new ArrayList<EntityID>();
	}
	
	/**
	 * Calcula a rota até onde o agente está soterrado
	 * OBS.: Entrará no edifício para resgatar o agente soterrado.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Destino EntityID de onde o agente está soterrado
	 * @param Bloqueios Lsita de bloqueios a serem desviados no roteamento 
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Resgatar(EntityID Origem, EntityID Destino, List<EntityID> Bloqueios) {
		
		return new ArrayList<EntityID>();
	}
}
