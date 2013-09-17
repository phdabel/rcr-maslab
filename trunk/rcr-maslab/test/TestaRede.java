
import java.io.File;
import java.io.IOException;
import org.neuroph.core.NeuralNetwork;
import util.MLPNetwork;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rgrunitzki
 */
public class TestaRede {

    public static void main(String[] args) throws IOException {
        File dir = new File(new File("").getAbsolutePath() + File.separator + "doc");
        File arq = new File(dir, "myMlPerceptron.nnet");
        System.out.println(arq.getPath());
        
        NeuralNetwork net = NeuralNetwork.load("rede.nnet");
        
        System.out.println("Não deu pau");
    }
}
