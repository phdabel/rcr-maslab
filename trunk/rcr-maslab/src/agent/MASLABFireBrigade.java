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
import model.AbstractMessage;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Road;
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
	private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
	private static final String GAS_STATION_RANGE = "ignition.gas_station.explosion.range";
	
	private int maxWater;
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
	
	private Estados estadoAnterior;
	private Estados estado;
	private EntityID destino = null;
	private EntityID alvoAtual = null;
	private EntityID alvoAnterior = null;
	private BurningBuilding alvo = null;
	
	private List<EntityID> path = new LinkedList<EntityID>();
	
	private int setorAtual;
	
    private String MSG_SEPARATOR = "-";
    private String MSG_FIM = ",";
    private Boolean trocarCanal = false;
    
    private List<BurningBuilding> Alvos = new LinkedList<BurningBuilding>();
//    private List<BurningBuilding> AlvosComunicar = new LinkedList<BurningBuilding>();
    
    //uma lista de bombeiros 'mal comportados' em kobe (p/ debug)
    List<Integer> monitored;
	
    private Boolean alterarAlvo = true;
    
    private boolean debug = false;
    
    //armazena a ultima posicao deste agente
  	EntityID lastPosition;
    
    //indicador de 'estou bloqueado por quantos timesteps'?
  	int stuckFor;
    
	/*
	 * 
	 * Métodos Standard Agent
	 */

	public MASLABFireBrigade(int pp){
		super(pp);
		setorAtual = (1 + rand.nextInt(4));
		AbstractMessage m = new AbstractMessage();
		MSG_SEPARATOR = m.getMSG_SEPARATOR();
		MSG_FIM = m.getMSG_FIM();
		stuckFor = 0;
		lastPosition = null;
		
		//prenche lista de bombeiros doidos em kobe
		monitored = new ArrayList<Integer>();
		monitored.add(1827029798);
		monitored.add(2005616081);
		monitored.add(1148420355);
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
		maxPower = config.getIntValue(MAX_POWER_KEY);
		//gasStationRange = config.getIntValue(GAS_STATION_RANGE);
		
		Logger.info("MASLAB fire brigade connected: max extinguish distance = "
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
		//System.out.println("FB on!");
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		
		if (time == config
				.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			// Subscribe to channel 1
			sendSubscribe(time, Channel.FIRE_BRIGADE.ordinal());
		}
		FireBrigade me = me();
		
		debug = false;//time==2 || monitored.contains(me.getID());
		/*
		if (trocarCanal){
			trocarCanal = false;
			sendSubscribe(time, Channel.FIRE_BRIGADE.ordinal());
		}
		*/
		//quando stuckFor e' maior q 2, bombeiro estava no canal da policia
		if (stuckFor > 2){
			//precisa voltar pro canal da ambulancia
			sendSubscribe(time, Channel.FIRE_BRIGADE.ordinal());
			//if(DEBUG) //System.out.println(me().getID() +": voltei pro canal do bombz");
			
		}
		
		if(debug){
			//System.out.println(me().getID() + " stuckFor= " + stuckFor);
		}
		
		//testa se esta no mesmo lugar do timestep passado
		if (me().getPosition().equals(lastPosition)){
			stuckFor++;
		}
		else {
			stuckFor = 0;
		}
		
		//atualiza o marcador de posicao anterior
		lastPosition = me().getPosition();
		
		debug = (time == 2 || monitored.contains(me.getID()) );
			
		//Receber Mensagens
		List<String> msgs = heardMessage(heard);

		//Separa todas as mensagens recebidas pois podem vir agrupadas de um único agente
		List<String> mensagens = new ArrayList<String>();
		for(String s: msgs){
			//String x[] = s.split(MSG_FIM);
			mensagens.addAll(Arrays.asList(s.split(MSG_FIM)));
		}
		
		//Armazena as informações recebidas
		for(String s: mensagens){
			//Separa as partes da mensagem
			List<String> msg = Arrays.asList(s.split(MSG_SEPARATOR));
			
			//Tamanho da mensagem de prédios em chamas
			//TODO - Ver uma forma eficiente de tratar as mensagens pois pode ser que recebamos um texto
			Boolean bAdd = true;
			BurningBuilding alvoAux = null;
			try{
				if(msg.size() == 4){
					//Se for um prédio em chamas
					if(Integer.parseInt(msg.get(0)) == MSGType.BURNING_BUILDING.ordinal()){
						bAdd = true;
						for(BurningBuilding bb : Alvos){
							if(bb.getID() == Integer.parseInt(msg.get(1))){
								bAdd = false;
								bb.AtualizarImportancia(Integer.parseInt(msg.get(3)));
							}
						}
						alvoAux = null;
						if (bAdd){
							////System.out.println(me().getID() + " Adicionando nova burning building ");
							alvoAux = new BurningBuilding(Integer.parseInt(msg.get(1)), Integer.parseInt(msg.get(2)), Integer.parseInt(msg.get(3)));
						}
						
						if(alvoAux != null){
							Alvos.add(alvoAux);
						}
					}
				}
			}catch(Exception e){
				////System.out.println(s);
			}
			
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
		
		String aux = "";
		for(BurningBuilding b: Alvos){
			aux += b.getID() + " - ";
		}
		
		if(debug){
			////System.out.println("TODOS MEUS ALVOS ANTES ID: " + me.getID().getValue() + " Alvos: " + aux);
		}
		AtualizarIncendios(time);
		
		aux = "";
		for(BurningBuilding b: Alvos){
			aux += b.getID() + " - ";
		}
		
		if(debug){
			////System.out.println("TODOS MEUS ALVOS DEPOIS ID: " + me.getID().getValue() + " Alvos: " + aux);
		}
		JuntarAlvos();
		
		alvoAnterior = alvoAtual;
		alvo = getIncendio(time, Alvos);
		//if(debug) System.out.println(me.getID()+" - alvo: " + alvo);
		if(alvo != null){
			//System.out.println(me().getID().getValue() + " Importancia: " + alvo.getImportancia());
			if(alvo.getImportancia() < 0){
				alvo = null;
				alvoAtual = null;
			}
		}

		//if(alvoAtual != null && debug)
			//System.out.println("ID: " + me.getID().getValue() + " Alvo: " + alvoAtual.getValue());
		
		//Perceber os bloqueios no caminho para desviar
		//Bloqueios = PERCEBERBLOQUEIOS();
		
		//TODO - Enviar Mensagens
		List<AbstractMessage> m = new ArrayList<AbstractMessage>();
		for(BurningBuilding bb: IncendiosComunicar){
			m.add(new AbstractMessage(String.valueOf(MSGType.BURNING_BUILDING.ordinal()), String.valueOf(bb.getID()), String.valueOf(bb.getTempoAtual()), String.valueOf(bb.getImportancia())));
		}
		if(time > 4)
			IncendiosComunicar.clear();
		if(m.size()>0){
			sendMessage(MSGType.BURNING_BUILDING, true, time, m);
		} 
		/*else {
			StandardEntity se = model.getEntity(me().getPosition());
			if(se instanceof Road){
				Road r = (Road)se;
				if(r.isBlockadesDefined() && r.getBlockades().size() > 0){
					sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
					m.add(new AbstractMessage(String.valueOf(MSGType.UNBLOCK_ME.ordinal()), String.valueOf(r.getID().getValue())));
					sendMessage(MSGType.UNBLOCK_ME, true, time, m);
					trocarCanal = true;
				}
			}
		}*/
		
		//System.out.println(me().getID() + " TIMESTEP : " + time);
		
		/*if(debug && time == 2){
			for(EntityID e : refugeIDs){
				System.out.println(me().getID() + " Refúgios : " + e.getValue());
			}
		}*/
		
		//Avaliando estados
		estadoAnterior = estado;
		//if (debug) System.out.println(me.getID() + ": alvoAtual="+alvoAtual);
		
		//se alvo atual ja foi apagado, torna-o null (evita entrar no estado Combatendo desnecessariamente)
		Building target = (Building) model.getEntity(alvoAtual);
		if (target != null && !target.isOnFire()) {
			//if(debug) System.out.println(me.getID()+": alvo apagado " + alvoAtual);
			alvoAtual = null;
		}
		
		//Estou no refúgio abastecendo
		if (me.isWaterDefined() && me.getWater() < maxWater
				&& (location() instanceof Refuge)) {
			estado = Estados.Abastecendo;
		//Estou sem água
		}else if (me.isWaterDefined() && me.getWater() == 0) {
			estado = Estados.BuscandoRefugio;
		//Vi algum incendio
		}else if(alvoAtual != null){
			estado = Estados.Combatendo;
		//Tô no vácuo...
		}else{
			if(me.isWaterDefined() && me.getWater() < .5 * maxWater){ //ajustado para nao buscar refugio se tiver mais q 50% de agua
				estado = Estados.BuscandoRefugio;
			}else{
				estado = Estados.Explorando;
			}
		}
		
		if(debug){
			//System.out.println(me().getID() + " Estado: " + estado.toString() + " Qtde Agua: " + me.getWater());
		}
		
		//Abastecendo, só espera até concluir
		if(estado.equals(Estados.Abastecendo)){
			sendRest(time);
			return;

		//Sem água, indo até o refúgio
		}else if (estado.equals(Estados.BuscandoRefugio)){
			//Caso já esteja indo para um refúgio/hidrante, continua a rota e não recalcula
			
			alvoAtual = null;
			
			/*if(debug){
				System.out.println(me().getID() + " refugeIDs.contains(destino): " + refugeIDs.contains(destino));
				if(destino != null)
					System.out.println(me().getID() + " destino: " + destino.getValue());
			}*/
			
			if(refugeIDs.contains(destino) && destino != null && path.contains(me().getPosition())){
				if(debug){
					//System.out.println(me().getID() + " Caminho Atual: " + path.toString());
				}
				List<EntityID> p = new ArrayList<EntityID>(path);
				for(EntityID e: p){
					if(e.getValue() == me.getPosition().getValue()){
						break;
					}else{
						path.remove(e);
					}
				}
				if(debug){
					//System.out.println(me().getID() + " Caminho Depois de Processado: " + path.toString());
				}
				
			}else{
				//Tenta encontrar um caminho até o refúgio
				path = routing.Abastecer(me.getPosition(), Bloqueios);
				if(debug){
					//System.out.println(me().getID() + " routing.Abastecer: " + path.toString());
				}
				//Armazena o destino para não ficar em loop
				destino = path.get(path.size()-1);
			}

			
			//Se encontrar o refúgio, move-se até ele
			if(path != null){
				checkStuck(time);
				sendMove(time, path);
				//System.out.println(me().getID() + " indo p/ refugio!" + path);
				return;
			/*
			 * Caso não tenha encontrado o caminho,
			 * tenta explorar e traçar a rota novamente no próximo passo de tempo
			 */
			} else {
				path = routing.Explorar(me().getPosition(), setorAtual, Bloqueios);
				destino = path.get(path.size()-1);
				//if(debug) System.out.println(me().getID() + " nao achei refugio, explorando:" + path);
				checkStuck(time);
				sendMove(time, path);
				return;
			}
			
		//Combatendo - Não "afrouxemo" nem nos "lançante" pois "semo" loco de dá com um pau
		} else if (estado.equals(Estados.Combatendo)){
			
			/*if(debug){
				if(alvoAtual != null){
					System.out.println(me().getID() + " Alvo: " + alvoAtual.getValue());
					System.out.println(me().getID() + " model.getDistance(getID(), alvoAtual): " + model.getDistance(getID(), alvoAtual));
				}
			}*/
			
			//Verifica se algum prédio pode ser combatido
			if (model.getDistance(getID(), alvoAtual) <= maxView) {
				sendExtinguish(time, alvoAtual, maxPower);
				return;
			}
			
			//Caso nenhum prédio possa ser combatido, move-se até um prédio
			//Se o destino já é o alvo então só atualiza o path
//			if(destino != null && debug)
				//System.out.println("Destino: " + destino.getValue() + " Alvo: " + alvoAtual.getValue());

			/*if(debug){
				if(alvoAtual != null){
					System.out.println(me().getID() + " Alvo: " + alvoAtual.getValue());
				}
			}*/
			
			if(alvoAtual != null && alvoAnterior != null && alvoAtual.getValue() == alvoAnterior.getValue() && estadoAnterior == estado && path.contains(me().getPosition())){
				if(debug){
					//System.out.println(me().getID() + " Path Atual: " + path.toString());
				}
				List<EntityID> p = new ArrayList<EntityID>(path);
				for(EntityID e: p){
					if(e.getValue() == me.getPosition().getValue()){
						break;
					}else{
						path.remove(e);
					}
				}
				if(debug){
					//System.out.println(me().getID() + " Path Depois: " + path.toString());
				}

			//Caso contrário calcula o caminho ate o alvo
			}else{
				Building a = (Building)model.getEntity(alvoAtual);
				
				//Calcula o caminho
				path = routing.Combater(me().getPosition(), alvoAtual, Bloqueios, sectoring.getSetorPertencente(me().getX(), me().getY()), sectoring.getSetorPertencente(a.getX(), a.getY()));
				//Atualiza o destino
				destino = path.get(path.size()-1);
				if(debug){
					//System.out.println(me().getID() + " Path Combatendo: " + path.toString() + " P. Atual: " + me().getPosition() + " Alvo Atual: " + alvoAtual.getValue());
				}
			}

			//Se está vendo o prédio e não consegue combater, fica na volta pra olhar o fogo se espalhando
			if(path.size() == 1 && me.getPosition().getValue() == path.get(0).getValue() && model.getDistance(getID(), alvoAtual) > maxDistance){
				Collection<StandardEntity> e = model.getObjectsInRange(me().getPosition(), maxDistance);
				for(StandardEntity se:e){
					if(se instanceof Road){
						path = routing.Combater(me().getPosition(), se.getID(), new ArrayList<EntityID>());
					}
				}
			}
			
			if (path != null) {
				checkStuck(time);
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
			checkStuck(time);
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

		if(debug){
			//System.out.println("TODOS MEUS ALVOS ANTES DENTRO ID: " + me().getID().getValue() + " Alvos: " + Alvos.size());
		}
		//Remove os prédios que não estão mais pegando fogo
		for(BurningBuilding a: alvosAuxiliar){
			for(Integer id : a.getIDsCorrespondentes()){
				if(debug){
					//System.out.println(me().getID() + " ID Principal: " + a.getID() + " IDCorrespondente: " + id);
				}

				if(notBurning.contains(id)){
					a.RemoverIDCorrespondente(id);
				}
			}
			if(a.getIDsCorrespondentes().size() == 0){
				Alvos.remove(a);
			}
		}

		//if(debug)
			//System.out.println("TODOS MEUS ALVOS DEPOIS DENTRO ID: " + me().getID().getValue() + " Alvos: " + Alvos.size());

		//Percenebendo incendios para atualizar a lista de incendios
		Collection<EntityID> burning = getBurningBuildings();
//		if(debug){
//			for(EntityID e: burning){
//				//System.out.println(me().getID().getValue() + " " + e.getValue());
//			}
//		}
		//Adiciona prédios pegando fogo
		for(EntityID e: burning){
			Building b = (Building)model.getEntity(e);

			BurningBuilding bb = getBurningBuildingsFromList(b, time);
			if(debug && bb != null){
				//System.out.println(me().getID().getValue() + " " + bb.getID() + " Lista " + bb.getIDsCorrespondentes().toString());
			}

			if(bb == null){
				//Se estiver vendo a building principal, apenas adiciona na lista de IDs
				Collection<StandardEntity> v = model.getObjectsInRange(me().getPosition(), maxDistance);
				if(debug){
					//System.out.println(me().getID().getValue() + " no campo de visão: " + v.toString());
				}

				if(v instanceof Building){
					bb = getBurningBuildingsFromList((Building)v, time);
					if(debug && bb != null){
						//System.out.println(me().getID().getValue() + " building principal: " + bb.getID() + " Lista " + bb.getIDsCorrespondentes().toString());
					}
				}
				
				if(bb != null){
					bb.addID(e.getValue());
					if(debug){
						//System.out.println(me().getID().getValue() + " adicionando um ID na lista ");
					}
					return;
				}
			}
			
			if(bb == null){
				if(debug){
					//System.out.println(me().getID().getValue() + " não encontrou nenhuma building principal no raio de visão ");
				}

				BurningBuilding aux = new BurningBuilding(e.getValue(), time, 0);
				aux.AtualizarImportancia(CalcularImportancia(aux, me().getPosition()));
				Alvos.add(aux);
				IncendiosComunicar.add(aux);
			}else{
				if(debug){
					//System.out.println(me().getID().getValue() + " atualizando a importancia ");
				}
				bb.AtualizarImportancia(CalcularImportancia(bb, me().getPosition()));
			}
		}
	}
	
	private BurningBuilding getBurningBuildingsFromList(Building b, int time){
		for(BurningBuilding bb : Alvos){
			List<Integer> IDs = bb.getIDsCorrespondentes();
			for(Integer i: IDs){
				if(b.getID().getValue() == i){
					bb.AtualizarImportancia(CalcularImportancia(bb, me().getPosition()));
					return bb;
				}
			}
		}
		return null;
	}
	
	private Collection<EntityID> getNOTBurningBuildings() {
		Collection<StandardEntity> e = model.getObjectsInRange(me().getPosition(), maxDistance);
		List<Building> result = new ArrayList<Building>();
		for (StandardEntity next : e) {
			if (next instanceof Building) {
				Building b = (Building) next;
				if(b.isTemperatureDefined()){
					if (!b.isOnFire()) {
						result.add(b);
					}
				}
			}
		}
		return objectsToIDs(result);
	}

	private BurningBuilding getIncendio(int time, List<BurningBuilding> all){
		//boolean debug = (me().getID().getValue() == 1827029798 || me().getID().getValue() == 2005616081);
				
		//if(debug) System.out.println(me().getID()+": getincendio...");
		int maiorImportancia = Integer.MIN_VALUE;
		BurningBuilding bb = null;
		Collection<StandardEntity> e = model.getObjectsInRange(me().getPosition(), maxDistance);

		alterarAlvo = true;
	/*	if (alvo != null){
			for(StandardEntity se : e){
				if(se instanceof Building){
					if(alvo.getID() == se.getID().getValue()){
						alterarAlvo = true;
					}
				}
			}
		}else{
			alterarAlvo = true;
		}*/
		if (alvoAtual != null) {
			alterarAlvo = false; //TODO: testar se o bombeiro nao fica doidao
		}
		if(debug){
			//System.out.println(me().getID().getValue() + " alterarAlvo? " + alterarAlvo);
		}
		
		//if(alterarAlvo && alvo == null){
		if (alvo == null) {
			if(debug){
				//System.out.println(me().getID()+": alvo == null");
				//System.out.println(me().getID().getValue() + " Sem alvo,  Alterar Alvo: " + alterarAlvo);
			}

			for(BurningBuilding b: all){
				Building building = (Building)model.getEntity(new EntityID(b.getID()));
				if(maiorImportancia < (b.getImportancia()) && building.getTotalArea() < 1000 ){
					maiorImportancia = b.getImportancia();
					bb = b;
					alvo = bb;
					alvoAtual = new EntityID(bb.getID());
					//if (debug) System.out.println(me().getID()+": escolhi alvoAtual="+alvoAtual);
				}
			}
			

			if(debug && alvoAtual != null){
				//System.out.println(me().getID().getValue() + " AlvoAtual: " + alvoAtual.getValue());
			}

		}else if(alterarAlvo && alvo != null){
			if(debug){
				//System.out.println(me().getID() + ": alterarAlvo && alvo != null // " + alterarAlvo + " Alvo: " + alvo.getID());
			}

			List<Integer> lstBB = alvo.getIDsCorrespondentes();
			if(debug){
				//System.out.println(me().getID().getValue() + " Alterar Alvo: " + alterarAlvo + " Alvo: " + alvo.getID() + " Lista: " + lstBB	);
			}
			
			for(Integer i: lstBB){
				Building b = (Building)model.getEntity(new EntityID(i));
				int menorTemperatura = Integer.MAX_VALUE;
				int menorFieryness = Integer.MAX_VALUE;
				int menorArea = Integer.MAX_VALUE;
				
				if(menorArea > b.getTotalArea()&& b.isOnFire()){
					
					if(b.isTemperatureDefined() && b.isFierynessDefined()){
						if(menorFieryness > b.getFieryness()){
							alvoAtual = b.getID();
							menorTemperatura = b.getTemperature();
						}else if(menorFieryness == b.getFieryness() && b.isOnFire()){
							if(menorTemperatura > b.getTemperature() && b.getTemperature() > 0){
								alvoAtual = b.getID();
							}
						}
						
					}
				}
			}
			//if(debug) System.out.println(me().getID()+": saindo de alterarAlvo && alvo != null // alvo=" + alvoAtual + " ");
		}
		
		if(!alterarAlvo){
			bb = alvo;
		}
		return bb;
	}
	
	private void JuntarAlvos(){
		Collection<StandardEntity> e = model.getObjectsInRange(me().getPosition(), maxDistance);
		BurningBuilding principal = null;
		List<BurningBuilding> lstBB = new ArrayList<BurningBuilding>(Alvos);
		for(StandardEntity se : e){
			for(BurningBuilding bb : lstBB){
				if(se.getID().getValue() == bb.getID()){
					if(principal == null){
						principal = bb;
					}else{
						for(Integer id: bb.getIDsCorrespondentes()){
							principal.addID(id);
						}
						Alvos.remove(bb);
					}
				}
			}
		}
	}
	
	
	/**
	 * Checa se o agente esta parado ha mais de dois timestep 
	 * e pede ajuda para policial se precisar
	 * @param time
	 */
	private void checkStuck(int time) {
		//boolean debug = monitored.contains(me().getID());
		
		if(stuckFor > 2) {
			sendSubscribe(time, Channel.POLICE_FORCE.ordinal());
			List<AbstractMessage> sos = new ArrayList<AbstractMessage>();
			sos.add(new AbstractMessage(
				String.valueOf(MSGType.UNBLOCK_ME.ordinal()), 
				String.valueOf(me().getPosition().getValue()))
			);
			sendMessage(MSGType.UNBLOCK_ME, true, time, sos);
			//trocarCanal = true;
			//if(debug) System.out.println(me().getID()+ ": SOS! " + sos);
		}
		else {
			//if(debug) System.out.println(me().getID() + ": not stuck...");
		}
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
