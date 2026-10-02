package com.ai;

import javax.swing.*;
import java.util.List;

public class TrainWorker extends SwingWorker<Void, Void> {

    private final Model model;
    private final List<double[]> inputs;
    private final List<double[]> outputs;
    private final int epochs;
    private final double lr;
    private final boolean autoLr;
    private final JProgressBar progressBar;
    private final JLabel lossLabel;
    private final JLabel statusLabel;
    private final List<Double> trainLosses;

    private volatile boolean stopFlag = false;

    public TrainWorker(Model model, List<double[]> inputs, List<double[]> outputs,
                       int epochs, double lr, boolean autoLr,
                       JProgressBar progressBar, JLabel lossLabel, JLabel statusLabel,
                       List<Double> trainLosses) {
        this.model = model;
        this.inputs = inputs;
        this.outputs = outputs;
        this.epochs = epochs;
        this.lr = lr;
        this.autoLr = autoLr;
        this.progressBar = progressBar;
        this.lossLabel = lossLabel;
        this.statusLabel = statusLabel;
        this.trainLosses = trainLosses;
    }

    public void stop() {
        stopFlag = true;
    }

    @Override
    protected Void doInBackground() {
        double currentLr = lr;
        double prevLoss = 0;
        long startTime = System.currentTimeMillis();

        for (int epoch = 0; epoch < epochs && !stopFlag; epoch++) {
            double loss = model.trainEpoch(inputs, outputs, currentLr);
            trainLosses.add(loss);

            if (autoLr && epoch >= 5 && prevLoss > 0) {
                double ratio = loss / prevLoss;
                if (ratio > 1.05) currentLr *= 0.9;
                else if (ratio < 0.85) currentLr *= 1.02;
            }
            prevLoss = loss;

            final int progress = (epoch + 1) * 100 / epochs;
            final double fLoss = loss;

            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(progress);
                lossLabel.setText(Lang.get("label.loss") + String.format("%.8f", fLoss));
                statusLabel.setText(Lang.get("msg.training") + " " + progress + "%");
            });
        }

        long elapsed = System.currentTimeMillis() - startTime;
        model.lastLoss = trainLosses.isEmpty() ? 0 :
                trainLosses.get(trainLosses.size() - 1);

        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(0);
            statusLabel.setText(Lang.get("msg.trainComplete") + " (" + (elapsed / 1000.0) + "s)");
            lossLabel.setText(Lang.get("label.loss") + String.format("%.8f", model.lastLoss));
        });

        return null;
    }
}