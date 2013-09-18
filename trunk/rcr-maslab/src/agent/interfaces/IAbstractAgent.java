/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.interfaces;

import java.util.Collection;
import java.util.List;

import model.BurningMensagem;
import rescuecore2.messages.Command;
import util.MSGType;

/**
 * 
 * @author rgrunitzki
 */
public interface IAbstractAgent {
    
    /**
     *
     */
    void sendMessage(MSGType type, boolean radio, int time, List<BurningMensagem> mensagens);
    void sendMessage(MSGType type, boolean radio, int time, BurningMensagem mensagens);
    
    List<String> heardMessage(Collection<Command> messages);
}   
