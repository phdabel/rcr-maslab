package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rescuecore2.worldmodel.EntityID;

public final class MASLABRouting {
	
	/*
	 * Identifica os setores:
	 * S1: Nordeste;
	 * S2: Sudeste;
	 * S3: Sudoeste;
	 * S4: Noroeste.
	 */
	public enum Setores{
		S1, S2, S3, S4;
	}
	
	/*
	 * Identifica os tipos das vias:
	 * Principal: Vias que delimitam os setores (Norte-Sul e Leste-Oeste);
	 * Secundario: Vias que ligam edifícios importantes e a via principal;
	 * Outros: Demais vias dos setores;
	 */
	public enum Tipos{
		Principal, Secundario, Outros;
	}
	
	/*
	 * Recebe o ID do local onde está (rua ou edifício) e calcula o caminho até o Refúgio/Hidrante mais próximo
	 */
	public List<EntityID> Abastecer(EntityID Origin) {
		
		return new ArrayList<EntityID>();
	}

	/*
	 * Recebe o ID do local onde está (rua ou edifício) e calcula o caminho até o edifício mais próximo.
	 * OBS.: Não entrará no edifício.
	 */
	public List<EntityID> Combater(EntityID Origem, EntityID Destino) {
		
		return new ArrayList<EntityID>();
	}

	/*
	 * Recebe o ID do local onde está (rua ou edifício) e o setor que deseja explorar
	 * OBS.: Não entrará em nenhum edifício.
	 */
	public List<EntityID> Explorar(EntityID Origin, Setores Setor) {
		
		return new ArrayList<EntityID>();
	}
	
	/*
	 * Recebe o ID do local onde está (rua ou edifício), o setor que deseja explorar e onde realizará a limpeza.
	 * Caso o Tipo seja Principal, limpará as vias principais do setor;
	 * Caso o Tipo seja Secundario, limpará os caminhos dos edifícios importantes até a via pincipal mais próxima.
	 * Caso o Tipo seja Outros, limpará qualquer caminho dentro do setor (não usárá as vias principais/secundárias).
	 */
	public List<EntityID> Limpar(EntityID Origin, Setores Setor, Tipos Tipo) {
		
		return new ArrayList<EntityID>();
	}	

	/*
	 * Recebe o ID do local onde está (rua ou edifício) e calcula a rota até o refúgio mais próximo
	 */
	public List<EntityID> Resgatar(EntityID Origin) {
		
		return new ArrayList<EntityID>();
	}
	
	/*
	 * Recebe o ID do local onde está (rua ou edifício) e o Destino (edifício que contenha um agente)
	 * OBS.: Entrará no edifício para resgatar o agente soterrado.
	 */
	public List<EntityID> Resgatar(EntityID Origin, EntityID Destino) {
		
		return new ArrayList<EntityID>();
	}
}
