package Mark2;

import org.apache.commons.io.FileUtils;
import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CSVRecordReader;
import org.canova.api.split.FileSplit;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @ author- Shikhar Srivastava
 * @ credit due to : Relevant Authors of DeepLearning4J Library
 *
 */
public class MLPClassifier_Mark1 {


    public static void main(String[] args) throws Exception {
        String type="Mark2_ver0.1";
        int seed = 123;
        double learningRate = 0.005;

        int batchSize=683;

        int nEpochs =8001;
        int splitTrainNum = (int) (batchSize * .6);
        int iterations= 1;
        int numInputs = 23;
        int numOutputs = 6;
        int numHiddenNodes = 10;

        RecordReader trainTest= new CSVRecordReader();
        trainTest.initialize(new FileSplit(new File("dl4j_0.4_examples/src/main/resources/classification/FeatureData_ann.csv")));
        DataSetIterator trainTestIter = new RecordReaderDataSetIterator(trainTest,batchSize,23,6);
        DataSet next= trainTestIter.next();
        next.shuffle();
        next.normalizeZeroMeanZeroUnitVariance();

        SplitTestAndTrain testAndTrain = next.splitTestAndTrain(splitTrainNum,new java.util.Random(seed));
        DataSet train = testAndTrain.getTrain();
        DataSet test = testAndTrain.getTest();
        Nd4j.ENFORCE_NUMERICAL_STABILITY = true;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .iterations(iterations)
            .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
            .learningRate(learningRate)
            .l1(1e-1).regularization(true).l2(2e-4)
            .updater(Updater.NESTEROVS)
            .list()
            .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes*2)
                .name("FirstHiddenLayer")
                .weightInit(WeightInit.XAVIER)
                .activation("relu")
                .momentum(0.9)
                .build())
            .layer(1,new DenseLayer.Builder()
                .name("SecondHiddenLayer")
                .nIn(numHiddenNodes*2).nOut(numHiddenNodes)
                .weightInit(WeightInit.XAVIER)
                .activation("relu")
                .momentum(0.9)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                .name("OutputLayer")
                .weightInit(WeightInit.XAVIER)
                .momentum(0.9)
                .activation("softmax").weightInit(WeightInit.XAVIER)
                .nIn(numHiddenNodes).nOut(numOutputs).build())
            .pretrain(false).backprop(true).build();


        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(Collections.singletonList((IterationListener) new ScoreIterationListener(1)));
        for ( int n = 0; n < nEpochs; n++) {
            model.fit(train);
        }


        System.out.println("Evaluate model....");
        Evaluation eval = new Evaluation(numOutputs);

        INDArray features = test.getFeatureMatrix();
        INDArray lables = test.getLabels();
        INDArray predicted = model.output(features,false);

        eval.eval(lables, predicted);

        //Print the evaluation statistics
        System.out.println(eval.stats());

        // **********Training is done. Code that follows is to save and print the model parameters***************

        OutputStream fos = Files.newOutputStream(Paths.get("coefficients_MLP_org"+type+"_"+nEpochs+".bin"));
        DataOutputStream dos = new DataOutputStream(fos);
        Nd4j.write(model.params(), dos);
        dos.flush();
        dos.close();
        FileUtils.writeStringToFile(new File("conf_MLP_org"+type+"_"+nEpochs+".json"), model.getLayerWiseConfigurations().toJson());

        MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(FileUtils.readFileToString(new File("conf_MLP_org"+type+"_"+nEpochs+".json")));
        DataInputStream dis = new DataInputStream(new FileInputStream("coefficients_MLP_org"+type+"_"+nEpochs+".bin"));
        INDArray newParams = Nd4j.read(dis);
        dis.close();
        MultiLayerNetwork savedNetwork = new MultiLayerNetwork(confFromJson);
        savedNetwork.init();
        savedNetwork.setParams(newParams);
        System.out.println("Original network params: " + model.params());
        // System.out.println("Saved network params: " + savedNetwork.params());

    }
}
