package com.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public String author;
    public List<Layer> layers = new ArrayList<>();
    public double lastLoss = 0.0;
    public transient Random random = new Random();

    public Model(String name, String author) {
        this.name = name;
        this.author = author;
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
        if (layers.size() > 1 && !layer.type.equals("input")) {
            Layer prev = layers.get(layers.size() - 2);
            int rows = layer.neurons;
            int cols = prev.neurons;
            double[][] w = new double[rows][cols];
            double[] b = new double[rows];

            double scale;
            switch (layer.activation) {
                case "relu": scale = Math.sqrt(2.0 / cols); break;
                case "tanh":
                case "sigmoid": scale = Math.sqrt(1.0 / cols); break;
                default: scale = 1.0 / Math.sqrt(cols);
            }

            for (int i = 0; i < rows; i++) {
                b[i] = (random.nextDouble() - 0.5) * 0.1;
                for (int j = 0; j < cols; j++) {
                    w[i][j] = (random.nextDouble() * 2 - 1) * scale;
                }
            }
            layer.weights = w;
            layer.biases = b;
        }
    }

    public double activate(double x, String act) {
        switch (act) {
            case "relu": return Math.max(0, x);
            case "sigmoid": return 1.0 / (1.0 + Math.exp(-Math.max(-500, Math.min(500, x))));
            case "tanh": return Math.tanh(x);
            default: return x;
        }
    }

    public double activateDerivative(double x, String act) {
        switch (act) {
            case "relu": return x > 0 ? 1 : 0;
            case "sigmoid": return x * (1 - x);
            case "tanh": return 1 - x * x;
            default: return 1;
        }
    }

    public double[] predict(double[] inputs) {
        if (layers.size() < 2) return inputs;
        double[] current = Arrays.copyOf(inputs, inputs.length);
        for (int i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer.type.equals("dense") || layer.type.equals("output")) {
                double[] output = new double[layer.neurons];
                for (int n = 0; n < layer.neurons; n++) {
                    double sum = layer.biases[n];
                    for (int j = 0; j < current.length; j++) {
                        sum += layer.weights[n][j] * current[j];
                    }
                    output[n] = activate(sum, layer.activation);
                }
                current = output;
            }
        }
        return current;
    }

    public double trainEpoch(List<double[]> inputsList, List<double[]> targetsList, double lr) {
        if (layers.size() < 2 || inputsList.isEmpty()) return 0.0;

        int batchSize = inputsList.size();
        int outputIdx = -1;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).type.equals("output")) {
                outputIdx = i;
                break;
            }
        }
        if (outputIdx == -1) return 0.0;

        List<double[][]> gradW = new ArrayList<>();
        List<double[]> gradB = new ArrayList<>();
        for (Layer layer : layers) {
            if (layer.type.equals("dense") || layer.type.equals("output")) {
                gradW.add(new double[layer.neurons][layer.weights[0].length]);
                gradB.add(new double[layer.neurons]);
            } else {
                gradW.add(null);
                gradB.add(null);
            }
        }

        double totalLoss = 0.0;

        for (int s = 0; s < batchSize; s++) {
            double[] input = inputsList.get(s);
            double[] target = targetsList.get(s);

            List<double[]> outputs = new ArrayList<>();
            outputs.add(Arrays.copyOf(input, input.length));

            for (int i = 1; i < layers.size(); i++) {
                Layer layer = layers.get(i);
                if (layer.type.equals("dense") || layer.type.equals("output")) {
                    double[] a = new double[layer.neurons];
                    for (int n = 0; n < layer.neurons; n++) {
                        double sum = layer.biases[n];
                        for (int j = 0; j < outputs.get(i - 1).length; j++) {
                            sum += layer.weights[n][j] * outputs.get(i - 1)[j];
                        }
                        a[n] = activate(sum, layer.activation);
                    }
                    outputs.add(a);
                } else {
                    outputs.add(outputs.get(i - 1));
                }
            }

            Layer outLayer = layers.get(outputIdx);
            double[] out = outputs.get(outputIdx);

            double[] delta = new double[outLayer.neurons];
            for (int i = 0; i < outLayer.neurons; i++) {
                double diff = out[i] - target[i];
                totalLoss += diff * diff;
                delta[i] = diff * activateDerivative(out[i], outLayer.activation);
            }

            for (int i = outputIdx; i >= 1; i--) {
                Layer layer = layers.get(i);
                if (layer.type.equals("dense") || layer.type.equals("output")) {
                    double[] prevOut = outputs.get(i - 1);
                    double[][] gw = gradW.get(i);
                    double[] gb = gradB.get(i);

                    for (int n = 0; n < layer.neurons; n++) {
                        gb[n] += delta[n];
                        for (int j = 0; j < prevOut.length; j++) {
                            gw[n][j] += delta[n] * prevOut[j];
                        }
                    }

                    if (i > 1) {
                        Layer prevLayer = layers.get(i - 1);
                        if (prevLayer.type.equals("dense")) {
                            double[] prevDelta = new double[prevOut.length];
                            for (int j = 0; j < prevOut.length; j++) {
                                double sum = 0;
                                for (int n = 0; n < layer.neurons; n++) {
                                    sum += layer.weights[n][j] * delta[n];
                                }
                                prevDelta[j] = sum * activateDerivative(prevOut[j], prevLayer.activation);
                            }
                            delta = prevDelta;
                        }
                    }
                }
            }
        }

        for (int i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer.type.equals("dense") || layer.type.equals("output")) {
                double[][] gw = gradW.get(i);
                double[] gb = gradB.get(i);
                for (int n = 0; n < layer.neurons; n++) {
                    gb[n] = Math.max(-1.0, Math.min(1.0, gb[n] / batchSize));
                    layer.biases[n] -= lr * gb[n];
                    for (int j = 0; j < layer.weights[n].length; j++) {
                        gw[n][j] = Math.max(-1.0, Math.min(1.0, gw[n][j] / batchSize));
                        layer.weights[n][j] -= lr * gw[n][j];
                    }
                }
            }
        }

        return totalLoss / batchSize;
    }
}