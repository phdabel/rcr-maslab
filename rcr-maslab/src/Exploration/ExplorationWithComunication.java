package Exploration;

import java.util.List;


import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class ExplorationWithComunication {


	@SuppressWarnings("unused")
	/**
	 * 
	 * @param EntityID me - Minha Identificação
	 * @param EntityID Agent - Identifcação do outro Agente
	 * @param List<StandardEntity> MinhasUltimasAcoes - Ultimos N nodes visitados por mim
	 * @param List<StandardEntity> MeusObjetivos -  percurso a ser percorrido por mim
	 * @param List<StandardEntity> OutrasUltimasAcoes - Ultimos N nodes visitados pelo outro agente
	 * @param List<StandardEntity> OutrosObjetivos - percurso desejado pelo outro agente
	 * @return boolean Situação - True : Buscar Outra Ação && False: Manter Ação
	 */
	
	private boolean CheckAnotherActions(EntityID me, EntityID Agent,
			List<StandardEntity> MinhasUltimasAcoes,
			List<StandardEntity> MeusObjetivos,
			List<StandardEntity> OutrasUltimasAcoes,
			List<StandardEntity> OutrosObjetivos) {
		// -----------------------------------------------------
		// Em caso de ações Conflitantes: Agentes com o mesmo Objetivo
		int totaldeacoes = 0;
		int acoesidenticas = 0;
		for (StandardEntity acoesminhas : MeusObjetivos) {
			for (StandardEntity acoesdeoutro : OutrosObjetivos) {
				if (acoesminhas.equals(acoesdeoutro)) {
					acoesidenticas++;
				}
				totaldeacoes++;
			}
		}
		// Verifica se existe 60% de coincidencias
		if (acoesidenticas / totaldeacoes > 0.6) {
			// Verifica quem tem maior prioridade pelo ID
			if (Integer.parseInt(me.toString()) > Integer.parseInt(Agent
					.toString())) {
				return false;
			} else {
				return true;
			}
		}
		// -----------------------------------------------------

		// Em caso de ações Repetidas: se meu Objetivo coincidem com ações
		// passadas do outro agente
		totaldeacoes = 0;
		acoesidenticas = 0;
		for (StandardEntity acoesminhas : MeusObjetivos) {
			for (StandardEntity acoesdeoutro : OutrasUltimasAcoes) {
				if (acoesminhas.equals(acoesdeoutro)) {
					acoesidenticas++;
				}
				totaldeacoes++;
			}
		}
		// Verifica se existe 60% de coincidencias
		if (acoesidenticas / totaldeacoes > 0.6) {
			// Verifica quem tem maior prioridade pelo ID
			if (Integer.parseInt(me.toString()) > Integer.parseInt(Agent
					.toString())) {
				return false;
			} else {
				return true;
			}
		}
		
		// -----------------------------------------------------
		
		// Ações Independentes
		
		return false;
	}

}
