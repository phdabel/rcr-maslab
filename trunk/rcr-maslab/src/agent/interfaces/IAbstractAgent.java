/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.interfaces;

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

}   
