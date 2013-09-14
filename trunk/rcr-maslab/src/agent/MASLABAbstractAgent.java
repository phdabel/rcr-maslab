package agent;

import agent.interfaces.IAbstractAgent;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import rescuecore2.standard.messages.AKSay;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.AKTell;
import util.Channel;
import util.MASLABBFSearch;
import util.MASLABPreProcessamento;
import util.MASLABRouting;
import util.MASLABSectoring;
import util.MSGType;

/**
 * Abstract base class for sample agents.
 *
 * @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class MASLABAbstractAgent<E extends StandardEntity> extends StandardAgent<E> implements IAbstractAgent {

    /**
     * Variaveis Sample Agent
     */
    private static final int RANDOM_WALK_LENGTH = 50;
    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class
            .getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class
            .getName();
    /**
     * The search algorithm.
     */
    protected MASLABBFSearch search;
    /**
     * Whether to use AKSpeak messages or not.
     */
    protected boolean useSpeak;
    /**
     * Cache of building IDs.
     */
    protected List<EntityID> buildingIDs;
    /**
     * Cache of road IDs.
     */
    protected List<EntityID> roadIDs;
    /**
     * Cache of refuge IDs.
     */
    protected List<EntityID> refugeIDs;
    private Map<EntityID, Set<EntityID>> neighbours;
    /**
     *
     * Variaveis definidas por nós
     *
     */
    /**
     * The routing algorithms.
     */
    protected MASLABRouting routing;
    /**
     * Classe de setorização, responsável por pre-processar e carregar os arquivos.
     */
	protected MASLABSectoring sectoring;
	
	
    /**
     * Cache of Hydrant IDs.
     */
    protected List<EntityID> hydrantIDs;
    protected List<EntityID> roadIDsSetor1;
    protected List<EntityID> roadIDsSetor2;
    protected List<EntityID> roadIDsSetor3;
    protected List<EntityID> roadIDsSetor4;
    protected List<EntityID> roadIDsPrincipal;
    protected List<EntityID> Bloqueios;
    protected final String MSG_SEPARATOR = "-";
	protected int PreProcessamento = 0;
	Random rand = new Random();

    /**
     *
     * Métodos Standard Agent
     *
     */
    /**
     * Construct an AbstractSampleAgent.
     */
    protected MASLABAbstractAgent(int pp) {
    	PreProcessamento = pp;
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        hydrantIDs = new ArrayList<EntityID>();
        Bloqueios = new ArrayList<EntityID>();
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
            if (next instanceof Hydrant) {
                hydrantIDs.add(next.getID());
            }
        }
        
        // Criação de uma lista com hidrantes e refúgios para os bombeiros
        List<EntityID> waterIDs = new ArrayList<EntityID>();
        waterIDs.addAll(refugeIDs);
        waterIDs.addAll(hydrantIDs);
        sectoring = new MASLABSectoring(model);
		
		//Realiza o pre-processamento
		if(PreProcessamento > 0){
			//Esse procedimento está descrito em cada agente pois pode ser iremos realizar diferentes processamentos em cada agente
			
		//Carrega os arquivos já processados
		}else{
			//Cria um objeto da classe de pre-processamento e carrega os arquivos já processados
			MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(model);
			PreProcess.CarregarArquivo();
			
			//Carrega o objeto de setorizacao com as informacoes do arquivo, sem necessadade de setorizar novamente
			sectoring = PreProcess.getMASLABSectoring();

			//Carrega um list dos roads de cada setor para fazer o random walk
			roadIDsSetor1 = new ArrayList<EntityID>(sectoring.getMapSetor(1).keySet()); 
			roadIDsSetor2 = new ArrayList<EntityID>(sectoring.getMapSetor(2).keySet()); 
			roadIDsSetor3 = new ArrayList<EntityID>(sectoring.getMapSetor(3).keySet()); 
			roadIDsSetor4 = new ArrayList<EntityID>(sectoring.getMapSetor(4).keySet()); 
			roadIDsPrincipal = new ArrayList<EntityID>(sectoring.getMapSetor(5).keySet()); 
			
		}
		
		routing = new MASLABRouting(sectoring.getMapSetor(1),
				sectoring.getMapSetor(2),
				sectoring.getMapSetor(3),
				sectoring.getMapSetor(4),
				sectoring.getMapSetor(5),
				roadIDs,
				waterIDs,
				buildingIDs,
				model,
				sectoring.getMapSetorSecundarias(),
				sectoring);
		
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(
                SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: "
                + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    /*
     * 
     * Métodos Definidos por nós
     * 
     */
    public void debug(int time, String str) {
        System.out.println(time + " - " + me().getID() + " - " + str);
    }

    /**
     * Envia uma mensagem
     *
     * @param type - Tipo da mensagem (ver tipos disponíveis)
     * @param radio - indica o meio de comunicação. true = radio e false = voz.
     * @param time - timestep atual.
     * @param params - parametros que compõem a mensagem. Variam de acordo com o
     * type da mensagem.
     */
    @Override
    public void sendMessage(MSGType type, boolean radio, int time, String... params) {

        //inicializa variaveis
        String msg = "";
        Channel channel = null;

        //monta a mensagem em um string
        for (int i = 0; i < params.length; i++) {
            if (i < params.length - 1) {
                msg += params[i] + MSG_SEPARATOR;
            } else {
                msg += params[i];
            }
        }
        //compacta a mensagem IMPLEMENTAR! - huffman ou zip?

        //monta a mensagem de acordo com o tipo e define o canal
        switch (type) {
            //Ex: Informar bloqueio
            case UNLOCK_MAIN_STREET: {
                channel = Channel.POLICE_FORCE;
                break;
            }
            case Type_2: {
                break;
            }
            case Type_3: {
                break;
            }
            case Type_4: {
                break;
            }
        }
        //envia de acordo com o meio (voz, radio)
        if (radio) {
            sendSpeak(time, channel.ordinal(), msg.getBytes());
        } else {
            sendSay(time, msg.getBytes());
        }
    }

    /**
     * Faz o processamento das mensagens recebidas
     *
     * @param messages - Lista de mensagens recebidas do Kernel
     */
    @Override
    public List<String> heardMessage(Collection<Command> messages) {
        List<String> list = new ArrayList<>();
        for (Command next : messages) {
            if ((next instanceof AKSpeak) && (byteToString(((AKSpeak) next).getContent()).length() > 0)) {
                list.add(byteToString(((AKSpeak) next).getContent()));
                //mensagem de rádio
            } else if ((next instanceof AKSay) && (byteToString(((AKSay) next).getContent()).length() > 0)) {
                list.add(byteToString(((AKSay) next).getContent()));
                //mensagem de voz
            } else if ((next instanceof AKTell) && (byteToString(((AKTell) next).getContent()).length() > 0)) {
                //mensagem de voz também
                list.add(byteToString(((AKTell) next).getContent()));
            }
        }
        return list;
    }

    private String byteToString(byte[] msg) {
        try {
            return new String(msg, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }
    /*
     * 
     * Métodos Acessores se necessário
     * 
     */
    
    /**
     * @return StandardWorldModel
     */
    public StandardWorldModel getWorldModel(){
    	return model;
    }
    
    /**
     * Returns the object that stores the sectors information
     * @return
     */
    public MASLABSectoring getSectoringInfo(){
    	return sectoring;
    }
    
    public Config getConfig(){
    	return config;
    }
}
