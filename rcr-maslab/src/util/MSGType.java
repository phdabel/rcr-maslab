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

    Type_1("Tipo 1"),
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