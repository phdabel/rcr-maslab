package util;


import java.io.File;
import java.io.InputStream;
import org.neuroph.core.NeuralNetwork;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rgrunitzki
 */
public class MLPNetwork {

    private NeuralNetwork network;

    public MLPNetwork(String path) {
        this.network = NeuralNetwork.createFromFile(path);
    }

    public MLPNetwork(File file) {
        this.network = NeuralNetwork.createFromFile(file);
    }

    public double eval(int fieryness, int temperature, int area) {
        network.setInput(fieryness, temperature, area);
        network.calculate();
        return network.getOutput()[0];
    }

    public NeuralNetwork getNetwork() {
        return network;
    }

    public void setNetwork(NeuralNetwork network) {
        this.network = network;
    }
}
