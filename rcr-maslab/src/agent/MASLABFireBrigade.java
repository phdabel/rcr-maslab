package agent;

import agent.interfaces.IFireBrigade;
import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import model.BurningBuilding;
import model.BurningMensagem;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.FireBrigade;
import util.Channel;
import util.MASLABPreProcessamento;
import util.MASLABSectoring;
import util.MSGType;

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
	private EntityID alvo = null;
	
	private List<EntityID> path = new LinkedList<EntityID>();
	
	private int setorAtual;
	
    private String MSG_SEPARATOR = "-";
    private String MSG_FIM = ",";
    
    private List<BurningBuilding> Alvos = new LinkedList<BurningBuilding>();
    private List<BurningBuilding> AlvosComunicar = new LinkedList<BurningBuilding>();
	
	/*
	 * 
	 * Métodos Standard Agent
	 */

	public MASLABFireBrigade(int pp){
		super(pp);
		setorAtual = (1 + rand.nextInt(4));
		BurningMensagem m = new BurningMensagem();
		MSG_SEPARATOR = m.getMSG_SEPARATOR();
		MSG_FIM = m.getMSG_FIM();
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
		
		//Receber Mensagens
		List<String> msgs = heardMessage(heard);
		
		//Separa todas as mensagens recebidas pois podem vir agrupadas de um único agente
		List<String> mensagens = new ArrayList<String>();
		for(String s: msgs){
			String x[] = s.split(MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(MSG_FIM)));
		}
		
		//Armazena as informações recebidas
		for(String s: mensagens){
			//Separa as partes da mensagem
			List<String> msg = Arrays.asList(s.split(MSG_SEPARATOR));
			
			//Tamanho da mensagem de prédios em chamas
			//TODO - Ver uma forma eficiente de tratar as mensagens pois pode ser que recebamos um texto
			try{
				if(msg.size() == 4){
					System.out.println("ID: " + me.getID().getValue() + " Recebido: " + s);
					//Se for um prédio em chamas
					if(Integer.parseInt(msg.get(0)) == MSGType.BURNING_BUILDING.ordinal()){
						BurningBuilding alvoAux = new BurningBuilding(Integer.parseInt(msg.get(1)), Integer.parseInt(msg.get(2)), Integer.parseInt(msg.get(3))); 
						if(!Alvos.contains(alvoAux)){
							Alvos.add(alvoAux);
						}
					}
				}
			}catch(Exception e){
				System.out.println(s);
			}
			
		}
		
		String aux = "";
		for(BurningBuilding b: Alvos){
			aux += b.getID() + " - ";
		}
		
		//TODO - Perceber o ambiente
		//Percenbendo onde está
		if(destino != null){
			if(me.getPosition().getValue() == destino.getValue()){
				destino = null;
			}
		}

		/*
		 * Atualizar lista de incêndios:
		 * - Adicionar novos incêndios
		 * - Remover incêndios que já foram combatidos
		 */
		AtualizarIncendios(time);
		
		aux = "";
		for(BurningBuilding b: Alvos){
			aux += b.getID() + " - ";
		}
		System.out.println("TODOS MEUS ALVOS ID: " + me.getID().getValue() + " Alvos: " + aux);
		
		
		alvo = getIncendio(time, Alvos);
		if(alvo != null)
			System.out.println("ID: " + me.getID().getValue() + " Alvo: " + alvo.getValue());
		
		//Perceber os bloqueios no caminho para desviar
		//Bloqueios = PERCEBERBLOQUEIOS();
		
		
		//TODO - Enviar Mensagens
		List<BurningMensagem> m = new ArrayList<BurningMensagem>();
		for(BurningBuilding bb: AlvosComunicar){
			m.add(new BurningMensagem(String.valueOf(MSGType.BURNING_BUILDING.ordinal()), String.valueOf(bb.getID()), String.valueOf(bb.getTempoAtual()), String.valueOf(bb.getTempoEstimado())));
		}
		if(time > 4)
			AlvosComunicar.clear();
		if(m.size()>0){
			sendMessage(MSGType.BURNING_BUILDING, true, time, m);
		}
		
		//Avaliando estados
		
		//Estou no refúgio abastecendo
		if (me.isWaterDefined() && me.getWater() < maxWater
				&& (location() instanceof Refuge 
						|| location() instanceof Hydrant)) {
			estado = Estados.Abastecendo;
		//Estou sem água
		}else if (me.isWaterDefined() && me.getWater() == 0) {
			estado = Estados.BuscandoRefugio;
		//Vi algum incendio
		}else if(alvo != null){
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
			//Caso já esteja indo para um refúgio/hidrante, continua a rota e não recalcula
			if(refugeIDs.contains(destino) || hydrantIDs.contains(destino)){
				List<EntityID> p = new ArrayList<EntityID>(path);
				for(EntityID e: p){
					if(e.getValue() == me.getPosition().getValue()){
						break;
					}else{
						path.remove(e);
					}
				}
				
			}else{
				//Tenta encontrar um caminho até o refúgio
				path = routing.Abastecer(me.getPosition(), Bloqueios);
				//Armazena o destino para não ficar em loop
				destino = path.get(path.size()-1);
			}
			
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
				destino = path.get(path.size()-1);
				sendMove(time, path);
				return;
			}
			
		//Combatendo - Não "afrouxemo" nem nos "lançante" pois "semo" loco de dá com um pau
		} else if (estado.equals(Estados.Combatendo)){
			
			//Verifica se algum prédio pode ser combatido
			if (model.getDistance(getID(), alvo) <= maxDistance) {
				sendExtinguish(time, alvo, maxPower);
				return;
			}
			
			//Caso nenhum prédio possa ser combatido, move-se até um prédio
			//Se o destino já é o alvo então só atualiza o path
			if(alvo.getValue() == destino.getValue()){
				System.out.println("JÀ ESTOU CAMINHANDO");
				List<EntityID> p = new ArrayList<EntityID>(path);
				for(EntityID e: p){
					if(e.getValue() == me.getPosition().getValue()){
						break;
					}else{
						path.remove(e);
					}
				}
			//Caso contrário calcula o caminho ate o alvo
			}else{
				//Calcula o caminho
				path = routing.Combater(me().getPosition(), alvo, Bloqueios);
				//Atualiza o destino
				destino = path.get(path.size()-1);
			}
			if (path != null) {
				sendMove(time, path);
				return;
			}
		
		//Explorando
		}else{
			//Guarda o destino para não ficar locão...
			if(destino == null){
				path = routing.Explorar(me().getPosition(), setorAtual, Bloqueios);
				destino = path.get(path.size()-1); 
			}else{
				path = routing.Explorar(me.getPosition(), setorAtual, Bloqueios, destino);
				destino = path.get(path.size()-1);
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
		return objectsToIDs(result);
	}
	
	/*
	 * 
	 * Métodos Definidos por nós
	 */
	
	private void AtualizarIncendios(int time){
		//Cria uma lista auxiliar para fazer a iteração
		List<BurningBuilding> alvosAuxiliar = new ArrayList<BurningBuilding>(Alvos);

		//Perceber prédios próximos para remover da lista de incendios
		Collection<EntityID> notBurning = getNOTBurningBuildings();
		
		//Remove os prédios que não estão mais pegando fogo
		for(BurningBuilding a: alvosAuxiliar){
			if(notBurning.contains(new EntityID(a.getID()))){
				Alvos.remove(a);
			}
		}
		
		//Percenebendo incendios para atualizar a lista de incendios
		Collection<EntityID> burning = getBurningBuildings();
		
		//Adiciona prédios pegando fogo
		boolean inserir = true;
		for(EntityID e: burning){
			inserir = true;
			for(BurningBuilding a: Alvos){
				if(burning.contains(new EntityID(a.getID()))){
					inserir = false;
				}
			}
			
			if (inserir){
				Building b = (Building)model.getEntity(e);
				//TODO - Atualizar cálculo
				Alvos.add(new BurningBuilding(e.getValue(), time, (b.getBrokenness() / b.getTemperature())));
				AlvosComunicar.add(new BurningBuilding(e.getValue(), time, (b.getBrokenness() / b.getTemperature())));
			}
		}
		
		
	}
	
	private Collection<EntityID> getNOTBurningBuildings() {
		Collection<StandardEntity> e = model.getObjectsInRange(me().getPosition(), maxDistance);
		List<Building> result = new ArrayList<Building>();
		for (StandardEntity next : e) {
			if (next instanceof Building) {
				Building b = (Building) next;
				if (!b.isOnFire()) {
					result.add(b);
				}
			}
		}
		return objectsToIDs(result);
	}

	private EntityID getIncendio(int time, List<BurningBuilding> all){
		int maiorTempoVida = Integer.MIN_VALUE;
		BurningBuilding bb = null;
		
		for(BurningBuilding b: all){
			
			if(maiorTempoVida < (b.getTempoRestante(time))){
				maiorTempoVida = b.getTempoRestante(time);
				bb = b;
			}
		}
		if(bb != null)
			return new EntityID(bb.getID());
		else
			return null;
	}
	
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
