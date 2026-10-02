package com.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 遗传算法优化器
 * 1. 随机生成多个模型
 * 2. 训练并评估准确率
 * 3. 保留最好的，其余变异
 * 4. 循环直到达到目标准确率
 */
public class GeneticOptimizer {

    private final List<double[]> sampleInputs;
    private final List<double[]> sampleOutputs;
    private final int inputDim;
    private final int outputDim;

    private int populationSize = 10;      // 每代模型数
    private int generations = 20;          // 最大进化代数
    private int epochsPerModel = 100;      // 每个模型训练轮数
    private double lr = 0.02;              // 学习率
    private double targetAccuracy = 0.90;  // 目标准确率
    private int patience = 5;              // 连续几代无提升就停止

    private String[] activations = {"relu", "sigmoid", "tanh"};

    private Random random = new Random();
    private volatile boolean stopFlag = false;

    private Model bestModel = null;
    private double bestAccuracy = 0.0;

    public GeneticOptimizer(List<double[]> inputs, List<double[]> outputs,
                            int inputDim, int outputDim) {
        this.sampleInputs = inputs;
        this.sampleOutputs = outputs;
        this.inputDim = inputDim;
        this.outputDim = outputDim;
    }

    public void stop() {
        stopFlag = true;
    }

    public Model getBestModel() {
        return bestModel;
    }

    public double getBestAccuracy() {
        return bestAccuracy;
    }

    /**
     * 运行遗传算法
     * @param callback 进度回调 (代数, 准确率)
     */
    public void run(ProgressCallback callback) {
        List<Model> population = new ArrayList<>();

        // 1. 初始化种群
        for (int i = 0; i < populationSize; i++) {
            population.add(generateRandomModel());
        }

        double bestAccOverall = 0.0;
        Model bestModelOverall = null;
        int noImproveCount = 0;

        for (int gen = 0; gen < generations && !stopFlag; gen++) {

            // 2. 评估每个模型
            double[] scores = new double[population.size()];
            for (int i = 0; i < population.size(); i++) {
                if (stopFlag) return;
                scores[i] = evaluateModel(population.get(i));
            }

            // 3. 找最好的
            int bestIdx = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[bestIdx]) bestIdx = i;
            }
            double bestAcc = scores[bestIdx];
            Model bestModel = population.get(bestIdx);

            // 4. 更新全局最优
            if (bestAcc > bestAccOverall) {
                bestAccOverall = bestAcc;
                bestModelOverall = cloneModel(bestModel);
                noImproveCount = 0;
            } else {
                noImproveCount++;
            }

            // 5. 回调
            if (callback != null) {
                callback.onGeneration(gen + 1, bestAcc);
            }

            // 6. 检查停止条件
            if (bestAccOverall >= targetAccuracy) {
                break;
            }
            if (noImproveCount >= patience) {
                break;
            }

            // 7. 生成下一代：保留最好的 + 变异
            List<Model> newPopulation = new ArrayList<>();
            newPopulation.add(bestModel);  // 保留最优

            while (newPopulation.size() < populationSize) {
                Model mutated = mutateModel(bestModel);
                newPopulation.add(mutated);
            }

            population = newPopulation;
        }

        this.bestModel = bestModelOverall;
        this.bestAccuracy = bestAccOverall;
    }

    /**
     * 随机生成一个模型
     */
    private Model generateRandomModel() {
        Model model = new Model("进化模型", "AI");

        // 输入层
        Layer inputLayer = new Layer("input", "input", inputDim, "none");
        model.addLayer(inputLayer);

        // 随机 1~3 个隐藏层
        int hiddenCount = random.nextInt(3) + 1;
        int prevNeurons = inputDim;

        for (int i = 0; i < hiddenCount; i++) {
            int neurons = random.nextInt(8) + 4;  // 4~11 个神经元
            String act = activations[random.nextInt(activations.length)];
            Layer hidden = new Layer("hidden_" + (i + 1), "dense", neurons, act);
            model.addLayer(hidden);
            prevNeurons = neurons;
        }

        // 输出层
        Layer outputLayer = new Layer("output", "output", outputDim, "sigmoid");
        model.addLayer(outputLayer);

        return model;
    }

    /**
     * 变异模型：在结构上做小幅修改
     */
    private Model mutateModel(Model base) {
        Model newModel = new Model("进化模型", "AI");

        for (int i = 0; i < base.layers.size(); i++) {
            Layer src = base.layers.get(i);

            if (src.type.equals("input") || src.type.equals("output")) {
                // 输入/输出层不变
                Layer copy = new Layer(src.name, src.type, src.neurons, src.activation);
                newModel.addLayer(copy);
            } else {
                // 隐藏层变异
                int neurons = src.neurons;
                String act = src.activation;

                // 30% 概率改变神经元数量
                if (random.nextDouble() < 0.3) {
                    neurons = Math.max(2, neurons + random.nextInt(5) - 2);
                }

                // 20% 概率改变激活函数
                if (random.nextDouble() < 0.2) {
                    act = activations[random.nextInt(activations.length)];
                }

                Layer copy = new Layer(src.name, "dense", neurons, act);
                newModel.addLayer(copy);
            }
        }

        return newModel;
    }

    /**
     * 评估模型准确率
     */
    private double evaluateModel(Model model) {
        int n = sampleInputs.size();
        if (n == 0) return 0.0;

        // 训练
        for (int e = 0; e < epochsPerModel && !stopFlag; e++) {
            model.trainEpoch(sampleInputs, sampleOutputs, lr);
        }

        // 评估
        int correct = 0;
        for (int i = 0; i < n; i++) {
            double[] pred = model.predict(sampleInputs.get(i));
            int predMax = argmax(pred);
            int trueMax = argmax(sampleOutputs.get(i));
            if (predMax == trueMax) correct++;
        }

        return (double) correct / n;
    }

    private int argmax(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    private Model cloneModel(Model src) {
        Model copy = new Model(src.name, src.author);
        for (Layer layer : src.layers) {
            Layer l = new Layer(layer.name, layer.type, layer.neurons, layer.activation);
            if (layer.weights != null) {
                l.weights = new double[layer.weights.length][];
                for (int i = 0; i < layer.weights.length; i++) {
                    l.weights[i] = layer.weights[i].clone();
                }
            }
            if (layer.biases != null) {
                l.biases = layer.biases.clone();
            }
            copy.layers.add(l);
        }
        copy.lastLoss = src.lastLoss;
        return copy;
    }

    public interface ProgressCallback {
        void onGeneration(int generation, double accuracy);
    }

    // ============================================================
    // Setter（可选调整参数）
    // ============================================================

    public void setPopulationSize(int size) { this.populationSize = size; }
    public void setGenerations(int g) { this.generations = g; }
    public void setEpochsPerModel(int e) { this.epochsPerModel = e; }
    public void setLr(double lr) { this.lr = lr; }
    public void setTargetAccuracy(double acc) { this.targetAccuracy = acc; }
    public void setPatience(int p) { this.patience = p; }
}