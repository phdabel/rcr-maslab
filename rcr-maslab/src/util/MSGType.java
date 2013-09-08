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
    UNLOCK_MAIN_STREET("Desbloquear via principal"),
    Type_2("Tipo 2"),
    Type_3("Tipo 3"),
    Type_4("Tipo 4");
    private String name;

    private MSGType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}