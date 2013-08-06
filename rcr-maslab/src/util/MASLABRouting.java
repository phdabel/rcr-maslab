package util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public final class MASLABRouting {
	
	private Map<EntityID, Set<EntityID>> Setor1;
	private Map<EntityID, Set<EntityID>> Setor2;
	private Map<EntityID, Set<EntityID>> Setor3;
	private Map<EntityID, Set<EntityID>> Setor4;
	private Map<EntityID, Set<EntityID>> Principais;
    private Hashtable<EntityID, List<EntityID>> PontosPrincipais;
    private List<EntityID> refugeIDs;
    private List<EntityID> waterIDs;
    private List<EntityID> buildingIDs;
    private List<EntityID> principalIDs;
    private MASLABBFSearch GlobalSearch;
    private MASLABBFSearch S1search;
    private MASLABBFSearch S2search;
    private MASLABBFSearch S3search;
    private MASLABBFSearch S4search;
    private MASLABBFSearch Psearch;
    
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
	public MASLABRouting(Map<EntityID, Set<EntityID>> s1, Map<EntityID, Set<EntityID>> s2, Map<EntityID, Set<EntityID>> s3, 
			Map<EntityID, Set<EntityID>> s4, Map<EntityID, Set<EntityID>> p, List<EntityID> r, List<EntityID> w, List<EntityID> b, 
			StandardWorldModel world, Hashtable<EntityID, List<EntityID>> pp, List<EntityID> pIDs){
		/*Setor1 = s1;
		Setor2 = s2;
		Setor3 = s3;
		Setor4 = s4;
		Principais = p;*/

		PontosPrincipais = pp;
		refugeIDs = r;
		waterIDs = w;
		buildingIDs = b;
		principalIDs = pIDs;
		GlobalSearch = new MASLABBFSearch(world);
		S1search = new MASLABBFSearch(s1);
		S2search = new MASLABBFSearch(s2);
		S3search = new MASLABBFSearch(s3);
		S4search = new MASLABBFSearch(s4);
		Psearch = new MASLABBFSearch(p);
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
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Abastecer(EntityID Origem, List<EntityID> Bloqueios, Integer... Setores) {
		return gotoRefugios(Origem, Bloqueios, Setores);
	}

	/**
 	 * Calcula o caminho até a entrada do edifício.
	 * OBS.: Não entrará no edifício.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Destino EntityID do destino do agente (pode ser rua ou edifício)
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Combater(EntityID Origem, EntityID Destino, List<EntityID> Bloqueios, Integer... Setores) {
		return gotoDestino(Origem, Destino, Bloqueios, false, Setores);
	}

	/**
	 * Define a rota de exploração aleatória - DESCREVER FUNÇÃO DO ALGORITMO
	 * OBS.: Não entrará em nenhum edifício.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Setor Setor que deseja ser explorado
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Explorar(EntityID Origem, Setores Setor, List<EntityID> Bloqueios, Integer... Setores) {
		
		return new ArrayList<EntityID>();
	}
	
	/**
	 * Define o caminho a ser percorrido e limpo pelo agente
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Setor Setor que deseja ser limpo
	 * @param Tipo Caso o Tipo seja Principal, limpará as vias principais do setor; Caso o Tipo seja Secundario, limpará os caminhos dos edifícios importantes até a via pincipal mais próxima; Caso o Tipo seja Outros, limpará qualquer caminho dentro do setor (não usárá as vias principais/secundárias).
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Limpar(EntityID Origem, Setores Setor, Tipos Tipo, Integer... Setores) {
		
		return new ArrayList<EntityID>();
	}

	/**
	 * Calcula a rota até o refúgio mais próximo
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Resgatar(EntityID Origem, List<EntityID> Bloqueios, Integer... Setores) {
		return gotoRefugios(Origem, Bloqueios, Setores);
	}
	
	/**
	 * Calcula a rota até onde o agente está soterrado
	 * OBS.: Entrará no edifício para resgatar o agente soterrado.
	 * @param Origem EntityID da origem do agente (pode ser rua ou edifício)
	 * @param Destino EntityID de onde o agente está soterrado
	 * @param Bloqueios Lista de bloqueios a serem desviados no roteamento 
	 * @param Setores Setor onde o agente se encontra e (se conhecido) setor onde o agente deseja ir
	 * @return Caminho a ser percorrido ou nulo caso o agente estiver preso
	 */
	public List<EntityID> Resgatar(EntityID Origem, EntityID Destino, List<EntityID> Bloqueios, Integer... Setores) {
		return gotoDestino(Origem, Destino, Bloqueios, true, Setores);
	}
	
	/**
	 * Descobre qual é o grafo a ser utilizado com base no setor atual do agente
	 * @return Algoritmo que contem o grafo a ser utilizado na busca
	 */
	private MASLABBFSearch getSearch(Integer Setor){
		if(Setor == 1){
			return S1search;
		}else if(Setor == 2){
			return S2search;
		}else if(Setor == 3){
			return S3search;
		}else if(Setor == 4){
			return S4search;
		}
		return GlobalSearch;
	}
	
	public List<EntityID> gotoRefugios(EntityID Origem, List<EntityID> Bloqueios, Integer... Setores) {
		//Transforma os parametros recebidos em um Array
		List<Integer> s = Arrays.asList(Setores);
		MASLABBFSearch search;
		
		//Se o setor de origem do agente foi informado, busca somente no local onde ele está
		if(s.size() > 0){
			//Obtem o grafo de busca conforme o setor atual 
			search = getSearch(s.get(0));
		}else{
			//Caso contrário, utiliza todo mapa
			search = GlobalSearch;
		}
		
		//Calcula o caminho mais curto do agente até a via principal
		List<EntityID> path = search.breadthFirstSearch(Origem, Bloqueios, principalIDs);
		
		//Remove a última posição do caminho para não duplicar e armazena para realizar o roteamento
		EntityID paux = path.get(path.size()-1);
		path.remove(path.size()-1);
		
		//Calcula o caminho mais curto da via principal até o ponto da via principal onde tem um refúgio mais perto e adiciona ao path
		path.addAll(Psearch.breadthFirstSearch(paux, Bloqueios, PontosPrincipais.keySet()));
		
		//Adiciona o caminho mais curto do refúgio a via principal no path
		path.addAll(PontosPrincipais.get(path.get(path.size()-1)));
		
		return path;
	}

	public List<EntityID> gotoDestino(EntityID Origem, EntityID Destino, List<EntityID> Bloqueios, Boolean EntrarEdificio, Integer... Setores) {
		//Transforma os parametros recebidos em um Array
		List<Integer> s = Arrays.asList(Setores);
		MASLABBFSearch search;
		
		//Se o setor de origem do agente foi informado, busca somente no local onde ele está
		if(s.size() > 0){
			//Obtem o grafo de busca conforme o setor atual 
			search = getSearch(s.get(0));
		}else{
			//Caso contrário, utiliza todo mapa
			search = GlobalSearch;
		}
		
		//Calcula o caminho mais curto do agente até a via principal
		List<EntityID> path = search.breadthFirstSearch(Origem, Bloqueios, principalIDs);
		
		//Se o setor de destido do agente foi informado, busca somente no local onde ele está
		if(s.size() > 1){
			//Obtem o grafo de busca conforme o setor atual 
			search = getSearch(s.get(1));
		}else{
			//Caso contrário, utiliza todo mapa
			search = GlobalSearch;
		}
		
		//Calcula o caminho mais curto entre o destino e a via principal
		List<EntityID> aux = search.breadthFirstSearch(Destino, Bloqueios, principalIDs);
		
		//Verifica se deve ou não entrar no destino
		if(!EntrarEdificio){
			//Se não deve e o destino (atualmente é a primeira posição do aux) for um edifício, remove ele do path
			if(path.get(path.size()-1).getClass().equals(Building.class)){
				aux.remove(0);
			}
		}
		
		//Reverte a ordem do caminho encontrado pois queremos ir da via para o destino e não ao contrário. 
		Collections.reverse(aux);
		
		//Remove a última posição do caminho para não duplicar e armazena para realizar o roteamento
		EntityID paux = path.get(path.size()-1);
		path.remove(path.size()-1);
		
		//Calcula o caminho mais curto da via principal até o ponto da via principal encontrado anteriormente e adiciona ao path
		path.addAll(Psearch.breadthFirstSearch(paux, Bloqueios, aux.get(0)));
		
		//Remove a primeira possição do aux para não duplicar 
		aux.remove(0);
		
		//Adiciona o caminho final ao path
		path.addAll(aux);
		
		return path;
	}

}
