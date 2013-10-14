/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

/**
 *
 * @author rgrunitzki
 */
public enum MSGType {

    //início, fim.
    BURNING_BUILDING("Edificio em chamas"),
    UNBLOCK_ME("Me desbloqueie"),
    SAVE_ME("Me salvem"),
    BURIED_HUMAN("Humano soterrado");
    
    private String name;

    private MSGType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}