package Exploration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/*Exploração armazena o mapeamento conhecido em um Hash da forma:
 *  	- StandardEntity ID : {String Problem, int TimeStep, Tempo de Vida Restante do Civil, Gravidade do Incendio}
 * O problema é definido da seguinte forma: abc { a- civis, b- fogo, c- bloqueio}, onde:
 * 		- 0 : ausencia de problema
 * 		- 1 : problema conhecido
 * Por exemplo:
 * 		- 001 : bloqueio
 * 		- 011 : fogo e bloqueio
 *      - 000 : nada
 * */
public class Exploration {

	@SuppressWarnings("unused")
	private StandardWorldModel model;
	static int TamanhoLista = 10;
	@SuppressWarnings("rawtypes")
	public HashMap<StandardEntity, List> Exploracao = new HashMap<StandardEntity, List>();

	public Exploration(StandardWorldModel world) {
		model = world;
		// TODO Auto-generated constructor stub
	}

	/**
	 * Armazena nova informação do mapa
	 * 
	 * @param int time- Step {Quando o problema foi vizualizado}
	 * @param StandardEntity
	 *            ID - Local do incidente
	 * @param String
	 *            Problem - Problema visualizado
	 * @param int remainingLifeTime - tempo restante de vida do civil
	 * @param int gravidadeIncendio - Situação do incendio
	 */
	public Boolean InsertNewInformation(int time, StandardEntity ID,
			String Problem, int remainingLifeTime, int gravidadeIncendio) {
		Boolean retorno = false;

		// Caso o problema seja conhecido será atualizado o conhecimento
		if (Exploracao.containsKey(ID)) {

			// Verifica se contem um problema e se o problema sofreu alterações
			List aux = Exploracao.get(ID);
			if (!aux.get(0).equals(Problem)) {
				// Se foi identificado um novo problema, inclui na lista de
				// mensagens a serem enviadas
				retorno = true;
			}
			// Senão, só atualiza as informações mas não envia mensagem
			UpdateInformation(time, ID, Problem, remainingLifeTime,
					gravidadeIncendio);

			return retorno;

			// Caso Contrario é inserio um novo conhecimento
		} else {
			// Se for inserida uma nova informação, sempre envia mensagem
			Exploracao.put(ID, Arrays.asList(Problem, time, remainingLifeTime,
					gravidadeIncendio));
			return true;
		}

	}

	/**
	 * Atualiza informações conhecidas do mapa
	 * 
	 * @param int timeAtual- Idade da informação
	 * @param StandardEntity
	 *            ID - Localização
	 * @param String
	 *            Problem- Problema
	 * @param int remainingLifeTime - tempo restante de vida do civil
	 * @param int gravidadeIncendio - Situação do incendio
	 */
	public void UpdateInformation(int timeAtual, StandardEntity ID,
			String Problem, int remainingLifeTime, int gravidadeIncendio) {
		int timeAntigo = Integer.parseInt(Exploracao.get(ID).get(0).toString());
		// Atualiza caso o tempo da informação seja mais importante
		if (timeAtual > timeAntigo) {
			Exploracao.remove(ID);
			Exploracao.put(ID, Arrays.asList(Problem, timeAtual,
					remainingLifeTime, gravidadeIncendio));
		} else {
			// Não atualiza
		}

	}

	/**
	 * Recebe um hash de exploração e atualiza suas informações
	 * 
	 * @param HashMap
	 *            <StandardEntity, List> mensagem - Atualiza informações de
	 *            exploração
	 */
	public void UpdateExploracao(HashMap<StandardEntity, List> mensagem) {
		Set<StandardEntity> chaves = mensagem.keySet();

		for (StandardEntity chave : chaves) {
			// (time, ID, Problem, remainingLifeTime,gravidadeIncendio);
			InsertNewInformation(
					Integer.parseInt(mensagem.get(chave).get(0).toString()), // time
					chave, // ID
					mensagem.get(chave).get(2).toString(), // Problem
					Integer.parseInt(mensagem.get(chave).get(3).toString()), // remainingLifeTime
					Integer.parseInt(mensagem.get(chave).get(4).toString())); // gravidadeIncendio
		}

	}

	/**
	 * Estabelece uma probabilidade de exploracao com base na diferenca
	 * temporal: 100*(1-(0.999^(TAtual - Tvisitado))) {normaliza os valores num
	 * intervalo de 0 à 100} Assim sorteia (roleta) um node para exploracao;
	 * 
	 * @param int timeAtual - Tempo atual da simulação
	 * @param agente
	 *            = 0:Policial, 1: bombeiro, 2: ambulancia
	 * @return StandardEntity node - node à visitar
	 */
	public StandardEntity GetNewExplorationNode(int timeAtual, int agente) {
		double limite = 0;
		// double auxlimear;
		HashMap<StandardEntity, Double> Roleta = new HashMap<StandardEntity, Double>();
		Set<StandardEntity> chaves = Exploracao.keySet();

		int auxTime = 1000000;
		int timeagent = 0;
		StandardEntity value = null;
		if (agente == 0) {
			while (value == null
					|| value.getStandardURN()
							.equals(StandardEntityURN.BUILDING)) {

				for (StandardEntity chave : chaves) {
					// Calculo do tempo: 100*( 1 - ( 0.999 ^ ( Tempo Atual -
					// Tempo de
					// Visita)))
					timeagent = Integer.parseInt(Exploracao.get(chave)
							.get(1).toString());
					// System.out.println("Timeagent: "+timeagent +
					// " timeAtual: "+ timeAtual);
					double time = (double) ((double) timeagent / (double) timeAtual);
					// System.out.println("Time:"+time);
					// int time = timeAtual -
					// Integer.parseInt(Exploracao.get(chave).get(0).toString());
					// auxlimear = 100 * (1 - (Math.pow(0.9999, time)));
					// Gera uma Roleta
					limite = limite + time;
					Roleta.put(chave, limite);

					// Guarda o valor com mais tempo sem acessar
					if (auxTime >= timeagent) {
						auxTime = timeagent;
						value = chave;
					}

				}

				/* Sorteio de um limear para exploção */
				Random r = new Random();
				double numerodasorte = (limite) * r.nextDouble();
				// double numerodasorte = (int) (rand.nextDouble()
				// nextDouble(0.0,100.) * limite);
				/* Seleção do node */
				Set<StandardEntity> indices = Roleta.keySet();
				// System.out.println("Roleta: "+Roleta.toString());
				for (StandardEntity indice : indices) {
					if (numerodasorte <= Roleta.get(indice).doubleValue()) {

						return indice;
					}
				}
			}
		}
		// entrar em edificios
		else{
			for (StandardEntity chave : chaves) {
				// Calculo do tempo: 100*( 1 - ( 0.999 ^ ( Tempo Atual -
				// Tempo de
				// Visita)))
				timeagent = Integer.parseInt(Exploracao.get(chave)
						.get(1).toString());
				// System.out.println("Timeagent: "+timeagent +
				// " timeAtual: "+ timeAtual);
				double time = (double) ((double) timeagent / (double) timeAtual);
				// System.out.println("Time:"+time);
				// int time = timeAtual -
				// Integer.parseInt(Exploracao.get(chave).get(0).toString());
				// auxlimear = 100 * (1 - (Math.pow(0.9999, time)));
				// Gera uma Roleta
				limite = limite + time;
				Roleta.put(chave, limite);

				// Guarda o valor com mais tempo sem acessar
				if (auxTime >= timeagent) {
					auxTime = timeagent;
					value = chave;
				}

			}

			/* Sorteio de um limear para exploção */
			Random r = new Random();
			double numerodasorte = (limite) * r.nextDouble();
			// double numerodasorte = (int) (rand.nextDouble()
			// nextDouble(0.0,100.) * limite);
			/* Seleção do node */
			Set<StandardEntity> indices = Roleta.keySet();
			// System.out.println("Roleta: "+Roleta.toString());
			for (StandardEntity indice : indices) {
				if (numerodasorte <= Roleta.get(indice).doubleValue()) {

					return indice;
				}
			}
		}
		
		return value;

	}

	/**
	 * Obtém a lista de edificios em chamas identificados
	 * 
	 * @return List<StandardEntity> Edificios - Lista de Prédios em chamas
	 *         conhecidos
	 */
	public List<StandardEntity> GetBurningBuilds() {
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> Predios = new ArrayList<>();

		for (StandardEntity chave : chaves) {
			if (Exploracao.get(chave).get(0).toString().substring(1, 2).equals("1")) {
				Predios.add(chave);
			}
		}
		return Predios;
	}

	/**
	 * Obtém a lista de civis feridos identificados
	 * 
	 * @return List<StandardEntity> Civis - Lista de civis identificado em
	 *         estado de perigo
	 */
	public List<StandardEntity> GetInjuredCivilians( ) {
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> civis = new ArrayList<>();

		for (StandardEntity chave : chaves) {
			if (Exploracao.get(chave).get(0).toString().substring(0, 1)
					.equals("1")) {
				civis.add(chave);
			}
		}
		return civis;
	}

	/**
	 * Obtém a lista de Bloqueios identificados
	 * 
	 * @return List<StandardEntity> Bloqueio - Lista de bloqueios identificados
	 */
	public List<StandardEntity> GetBlockRoads( ) {
		Set<StandardEntity> chaves = Exploracao.keySet();
		List<StandardEntity> bloqueios = new ArrayList<>();

		for (StandardEntity chave : chaves) {
			
			if (Exploracao.get(chave).get(0).toString().substring(2, 3).equals("1")) {
				//System.out.println("Bloqueio: "+ExploracaoLocal.get(chave));
				bloqueios.add(chave);
			}
		}
		return bloqueios;
	}

	/**
	 * Limpa as informações da exploração do agente
	 * 
	 * @param List
	 *            <StandardEntity> informacoes - Lista de problemas a ser limpo
	 * @param int problema - 0- civis, 1- incendio, 2- bloqueio
	 */
	public void clear(List<StandardEntity> informacoes, int problema) {
		// Civis
		if (problema == 0) {
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(2).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(0)
						.toString());
				Exploracao.remove(chave);
				String Problem = "0" + aux.substring(1, 2)
						+ aux.substring(2, 3);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Incendios
		if (problema == 1) {
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(2).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(0)
						.toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0, 1) + "0"
						+ aux.substring(2, 3);
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}
		// Bloqueios
		if (problema == 2) {
			for (StandardEntity chave : informacoes) {
				String aux = Exploracao.get(chave).get(2).toString();
				int time = Integer.parseInt(Exploracao.get(chave).get(0)
						.toString());
				Exploracao.remove(chave);
				String Problem = aux.substring(0, 1) + aux.substring(1, 2)
						+ "0";
				Exploracao.put(chave, Arrays.asList(Problem, time));
			}
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 *
	 * Obtém uma lista das ultimas ações dos agentes
	 *
	 * @param int Passos - Define o numero das ultimas ações
	 * @return List<StandardEntity> UltimosNodesVisitados - Retorna uma lista
	 * dos ultimos lugares visitados pelo agente
	 */
	public List<StandardEntity> GetLastAction(int Passos) {

		List mapKeys = new ArrayList<StandardEntity>(Exploracao.keySet());
		List mapValues = new ArrayList(Exploracao.get(0));

		Collections.sort(mapValues);
		Collections.sort(mapKeys);

		LinkedHashMap sortedMap = new LinkedHashMap();

		Iterator valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Object val = valueIt.next();
			Iterator keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Object key = keyIt.next();
				String comp1 = Exploracao.get(key).toString();
				String comp2 = val.toString();

				if (comp1.equals(comp2)) {
					Exploracao.remove(key);
					mapKeys.remove(key);
					sortedMap.put((String) key, (Double) val);
					break;
				}

			}

		}

		List<StandardEntity> UltimasAcoes = new ArrayList<StandardEntity>();
		int j = 0;
		for (StandardEntity chave : (List<StandardEntity>) sortedMap) {
			if (j >= Passos) {
				return UltimasAcoes;
			} else {
				UltimasAcoes.add(chave);
			}
			j++;
		}

		return (List<StandardEntity>) sortedMap;
	}

	@SuppressWarnings({ "unchecked" })
	/**
	 *
	 * @return List<StandardEntity> Nodes já Explorados
	 */
	public List<StandardEntity> GetExplorationNodes(
			 ) {
		List<StandardEntity> Explorationnodes = new ArrayList<>();

		Explorationnodes = new ArrayList<StandardEntity>(
				Exploracao.keySet());

		return Explorationnodes;

	}

}
