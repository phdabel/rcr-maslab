/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.interfaces;

import java.util.Collection;
import java.util.List;
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
    void sendMessage(MSGType type, boolean radio, int time, String ... params);
    
    List<String> heardMessage(Collection<Command> messages);
}   
