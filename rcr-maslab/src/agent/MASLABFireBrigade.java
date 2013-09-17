package agent;

import agent.interfaces.IFireBrigade;
import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.FireBrigade;
import util.Channel;
import util.DistanceSorter;
import util.MASLABPreProcessamento;
import util.MASLABSectoring;
import util.Setores;

/**
 * A sample fire brigade agent.
 */
public class MASLABFireBrigade extends MASLABAbstractAgent<FireBrigade>
		implements IFireBrigade {

	/**
	 * 
	 * Variaveis Sample Agent
	 * 
	 */

	private static final String MAX_WATER_KEY = "fire.tank.maximum";
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	private static final String GAS_STATION_RANGE = "ignition.gas_station.explosion.range";
	
	private int maxWater;
	private int maxDistance;
	private int maxPower;
	private int gasStationRange;
	
	/**
	 * 
	 * Variaveis definidas por nós
	 * 
	 */
	
	private enum Estados {
		Abastecendo, BuscandoRefugio, Explorando, Combatendo;
	}
	private Estados estado;
	private EntityID destino = null;
	
	private List<EntityID> path = new LinkedList<EntityID>();
	
	private int setorAtual;
	
	/*
	 * 
	 * Métodos Standard Agent
	 */

	public MASLABFireBrigade(int pp){
		super(pp);
		setorAtual = (1 + rand.nextInt(4));
		System.out.println(setorAtual);
	}
	
	@Override
	public String toString() {
		return "MASLAB fire brigade";
	}

	@Override
	protected void postConnect() {
		
		
		/*
		 Bombeiros
			- Apagarão os prédios mais relevantes e ignorarão os demais (para controlar incêndio):
			Usar a Rede Neural pra ver qual incêndio vale a pena apagar.
			
			getIncendio(Lista com todos os incêndios conhecidos):
				Retorna o incêndio que deve ser combatido;
			
			
			
			Comunicação Específica
				Cada vez que um incêndio for descoberto, informa via rádio:
					MSGType.Type.getOrdinal MSG_SEPARATOR ID MSG_SEPARATOR TempoAtual MSG_SEPARATOR TempoEstimado
					OBS.: Fazer na classe de bombeiros e depois que estiver OK, passar para abstractAgent.
				
			Comunicação Geral
				Solicitar desbloqueio
					MSGType.Type.getOrdinal MSG_SEPARATOR ID
				
				Solicitar resgate
					MSGType.Type.getOrdinal MSG_SEPARATOR ID
				
			
			Como decidir se ignora a atividade atual e apaga o incêndio visto agora????
						
		

			
			CAMINHAR???
			
			- Haverá agentes patrulhando a área e não necessariamente combatendo um incêndio????
			Lider do setor:
				Definir quem vai patrulhar o setor;
				
			Coordenação entre as duplas:
			  - No pre-processamento os agentes serão alocados em pares e onde se encontrar;
			  - O agente de menor ID irá definir para onde irão (comunicação por voz quando se encontrarem);
			  
			  
		 */
		
		
		
		
		
		
		
		super.postConnect();
		model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,
				StandardEntityURN.HYDRANT, StandardEntityURN.GAS_STATION);
		maxWater = config.getIntValue(MAX_WATER_KEY);
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
		maxPower = config.getIntValue(MAX_POWER_KEY);
		//gasStationRange = config.getIntValue(GAS_STATION_RANGE);
		
		Logger.info("Sample fire brigade connected: max extinguish distance = "
				+ maxDistance + ", max power = " + maxPower + ", max tank = "
				+ maxWater);
		
		if(PreProcessamento > 0){
			//Cria um novo objeto da classe de setorizacao e setoriza
	        sectoring = new MASLABSectoring(model);
	        sectoring.Setorizar();
	        
	        //Cria um novo objeto da classe de pre-processamento e gera os arquivos
			MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(sectoring);
			PreProcess.GerarArquivos();
			
		//Carrega os arquivos já processados
		}else{
			//Esse procedimento é genérico para todos os agentes, por isso está na classe MASLABAbstractAgent
		}
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, Channel.FIRE_BRIGADE.ordinal());
		}

		FireBrigade me = me();
		
		//TODO - Receber Mensagens
		
		
		
		
		//TODO - Perceber o ambiente
		//Percenbendo onde está
		if(destino != null){
			if(me.getPosition().getValue() == destino.getValue()){
				destino = null;
			}
		}
		//Percenebendo incendios
		Collection<EntityID> all = getBurningBuildings();
		//Perceber os bloqueios no caminho para desviar
		//Bloqueios = PERCEBERBLOQUEIOS();
		
		
		
		//TODO - Enviar Mensagens
		
		
		//Avaliando estados
		
		//Estou no refúgio abastecendo
		if (me.isWaterDefined() && me.getWater() < maxWater
				&& location() instanceof Refuge) {
			estado = Estados.Abastecendo;
		//Estou sem água
		}else if (me.isWaterDefined() && me.getWater() == 0) {
			estado = Estados.BuscandoRefugio;
		//Vi algum incendio
		}else if(all.size() > 0){
			estado = Estados.Combatendo;
		//Tô no vácuo...
		}else{
			estado = Estados.Explorando;
		}
		
		//Abastecendo, só espera até concluir
		if(estado.equals(Estados.Abastecendo)){
			sendRest(time);
			return;

		//Sem água, indo até o refúgio
		}else if (estado.equals(Estados.BuscandoRefugio)){
			//Tenta encontrar um caminho até o refúgio
			path = routing.Abastecer(me.getPosition(), Bloqueios);

			//Se encontrar o refúgio, move-se até ele
			if(path != null){
				sendMove(time, path);
				return;
			/*
			 * Caso não tenha encontrado o caminho,
			 * tenta explorar e traçar a rota novamente no próximo passo de tempo
			 */
			} else {
				path = routing.Explorar(me().getPosition(), setorAtual, Bloqueios);
				sendMove(time, path);
				return;
			}
			
		//Combatendo - Não "afrouxemo" nem nos "lançante" pois "semo" loco de dá com um pau
		} else if (estado.equals(Estados.Combatendo)){
			
			//Verifica se algum prédio pode ser combatido
			for (EntityID next : all) {
				if (model.getDistance(getID(), next) <= maxDistance) {
					sendExtinguish(time, next, maxPower);
					return;
				}
			}
			
			//Caso nenhum prédio possa ser combatido, move-se até um prédio
			for (EntityID next : all) {
				path = planPathToFire(next);
				if (path != null) {
					sendMove(time, path);
					return;
				}
			}
		
		//Explorando
		}else{
			//Guarda o destino para não ficar locão...
			if(destino == null){
				path = routing.Explorar(me().getPosition(), setorAtual, Bloqueios);
				destino = path.get(path.size()-1); 
			}else{
				path = routing.Explorar(me.getPosition(), setorAtual, Bloqueios, destino);
			}
			sendMove(time, path);
			return;
		}
	}

	/**
	 * 
	 * Métodos Sample Agent
	 * 
	 */
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}

	private Collection<EntityID> getBurningBuildings() {
		Collection<StandardEntity> e = model
				.getEntitiesOfType(StandardEntityURN.BUILDING);
		List<Building> result = new ArrayList<Building>();
		for (StandardEntity next : e) {
			if (next instanceof Building) {
				Building b = (Building) next;
				if (b.isOnFire()) {
					result.add(b);
				}
			}
		}
		// Sort by distance
		Collections.sort(result, new DistanceSorter(location(), model));
		return objectsToIDs(result);
	}

	private List<EntityID> planPathToFire(EntityID target) {
		// Try to get to anything within maxDistance of the target
		Collection<StandardEntity> targets = model.getObjectsInRange(target,
				maxDistance);
		if (targets.isEmpty()) {
			return null;
		}
		//TODO - Definir o alvo do combate
		return routing.Combater(me().getPosition(), new ArrayList<EntityID>(objectsToIDs(targets)).get(0), Bloqueios);
	}
	
	/*
	 * 
	 * Métodos Definidos por nós
	 */
	
	/*
	 * 
	 * Métodos Acessores se necessário
	 */
	
	
	/**
	 * Returns the explosion range of the gas station
	 * @return
	 */
	public int getGasStationRange(){
		return gasStationRange;
	}
}
