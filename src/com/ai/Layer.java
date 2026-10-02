package com.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Layer implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public String type;
    public int neurons;
    public String activation;
    public double[][] weights;
    public double[] biases;
    public String[] outputLabels;
    public List<double[]> sampleInputs = new ArrayList<>();
    public List<double[]> sampleOutputs = new ArrayList<>();

    public Layer(String name, String type, int neurons, String activation) {
        this.name = name;
        this.type = type;
        this.neurons = neurons;
        this.activation = activation;
        this.outputLabels = new String[neurons];
        for (int i = 0; i < neurons; i++) {
            outputLabels[i] = "输出" + (i + 1);
        }
    }
}