package com.ai;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GUI extends JFrame {

    private Model model;
    private List<Layer> layers;
    private List<double[]> sampleInputs;
    private List<double[]> sampleOutputs;

    private JTextField modelNameField;
    private JTextField modelAuthorField;
    private JTextField inputDimField;
    private JList<String> layerList;
    private DefaultListModel<String> layerListModel;
    private JTextArea logArea;
    private JTextField epochsField;
    private JTextField lrField;
    private JCheckBox autoLrCheck;
    private JProgressBar progressBar;
    private JLabel lossLabel;
    private JLabel sampleCountLabel;
    private JLabel statusLabel;
    private JLabel pathLabel;
    private JLabel statLabel;

    private int correctCount = 0;
    private int wrongCount = 0;
    private boolean isTraining = false;
    private List<Double> trainLosses = new ArrayList<>();
    private String modelDir;
    private TrainWorker currentWorker;
    private GeneticOptimizer currentOptimizer;

    public GUI() {
        super(Lang.get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        loadAppIcon();

        // ========== 从配置读路径 ==========
        modelDir = ConfigManager.getModelDir();

        layers = new ArrayList<>();
        sampleInputs = new ArrayList<>();
        sampleOutputs = new ArrayList<>();

        initMenuBar();
        initMainPanel();
        initStatusBar();

        model = null;
        updateLayerList();
        log("请点击「新建」创建模型");

        setVisible(true);
    }

    private void loadAppIcon() {
        try {
            Image icon = null;
            File external = new File("icon/app.png");
            if (external.exists()) {
                icon = new ImageIcon(external.getAbsolutePath()).getImage();
            }
            if (icon == null) {
                URL url = GUI.class.getResource("/com/ai/app.png");
                if (url != null) icon = new ImageIcon(url).getImage();
            }
            if (icon != null) setIconImage(icon);
        } catch (Exception e) {
            System.out.println("[Icon] 加载失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 菜单栏
    // ============================================================

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件
        JMenu fileMenu = new JMenu(Lang.get("menu.file"));
        JMenuItem importItem = new JMenuItem(Lang.get("menu.import"));
        importItem.addActionListener(e -> importModel());
        JMenuItem saveItem = new JMenuItem(Lang.get("menu.save"));
        saveItem.addActionListener(e -> saveModel());
        JMenuItem setPathItem = new JMenuItem(Lang.get("menu.setPath"));
        setPathItem.addActionListener(e -> setModelPath());
        JMenuItem exitItem = new JMenuItem(Lang.get("menu.exit"));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(importItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(setPathItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 语言
        JMenu langMenu = new JMenu("语言 / Language");
        JMenuItem zhItem = new JMenuItem("中文");
        zhItem.addActionListener(e -> switchLanguage("zh_CN"));
        JMenuItem enItem = new JMenuItem("English");
        enItem.addActionListener(e -> switchLanguage("en_US"));
        langMenu.add(zhItem);
        langMenu.add(enItem);

        // 帮助
        JMenu helpMenu = new JMenu(Lang.get("menu.help"));
        JMenuItem aboutItem = new JMenuItem(Lang.get("menu.about"));
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(langMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    /**
     * 切换语言
     */
    private void switchLanguage(String langCode) {
        // 保存到配置
        ConfigManager.setLanguage(langCode);
        log("语言已设置为: " + langCode);

        int choice = JOptionPane.showConfirmDialog(this,
                "切换语言需要重启程序才能生效。\n是否立即退出？",
                "切换语言",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void initMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 左侧
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.model")));
        leftPanel.setPreferredSize(new Dimension(380, 0));

        JPanel modelInfoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        modelInfoPanel.add(new JLabel(Lang.get("label.modelName")));
        modelNameField = new JTextField("我的模型");
        modelInfoPanel.add(modelNameField);
        modelInfoPanel.add(new JLabel(Lang.get("label.author")));
        modelAuthorField = new JTextField("AI工坊");
        modelInfoPanel.add(modelAuthorField);
        modelInfoPanel.add(new JLabel(Lang.get("label.inputDim")));
        inputDimField = new JTextField("3");
        modelInfoPanel.add(inputDimField);
        leftPanel.add(modelInfoPanel, BorderLayout.NORTH);

        layerListModel = new DefaultListModel<>();
        layerList = new JList<>(layerListModel);
        layerList.setFont(new Font("Consolas", Font.PLAIN, 12));
        leftPanel.add(new JScrollPane(layerList), BorderLayout.CENTER);

        JPanel layerBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newModelBtn = new JButton(Lang.get("btn.new"));
        newModelBtn.addActionListener(e -> createNewModel());
        JButton addLayerBtn = new JButton(Lang.get("btn.addLayer"));
        addLayerBtn.addActionListener(e -> addLayer());
        JButton editLayerBtn = new JButton("编辑层");
        editLayerBtn.addActionListener(e -> editLayer());
        JButton delLayerBtn = new JButton(Lang.get("btn.delLayer"));
        delLayerBtn.addActionListener(e -> deleteLayer());
        layerBtnPanel.add(newModelBtn);
        layerBtnPanel.add(addLayerBtn);
        layerBtnPanel.add(editLayerBtn);
        layerBtnPanel.add(delLayerBtn);
        leftPanel.add(layerBtnPanel, BorderLayout.SOUTH);

        // 右侧
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        JPanel samplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        samplePanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.samples")));
        JButton addSampleBtn = new JButton(Lang.get("btn.addSample"));
        addSampleBtn.addActionListener(e -> addSample());
        JButton showSamplesBtn = new JButton(Lang.get("btn.showSamples"));
        showSamplesBtn.addActionListener(e -> showSamples());
        JButton importJsonBtn = new JButton("导入JSON样本");
        importJsonBtn.addActionListener(e -> importJsonSamples());
        JButton recommendBtn = new JButton(Lang.get("btn.recommend"));
        recommendBtn.addActionListener(e -> showRecommend());
        sampleCountLabel = new JLabel(Lang.get("label.sampleCount") + "0");
        samplePanel.add(addSampleBtn);
        samplePanel.add(showSamplesBtn);
        samplePanel.add(importJsonBtn);
        samplePanel.add(recommendBtn);
        samplePanel.add(sampleCountLabel);

        JPanel trainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        trainPanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.train")));
        trainPanel.add(new JLabel(Lang.get("label.epochs")));
        epochsField = new JTextField("500", 6);
        trainPanel.add(epochsField);
        trainPanel.add(new JLabel(Lang.get("label.lr")));
        lrField = new JTextField("0.02", 6);
        trainPanel.add(lrField);
        autoLrCheck = new JCheckBox(Lang.get("label.autoLr"), true);
        trainPanel.add(autoLrCheck);

        JPanel trainBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton trainBtn = new JButton(Lang.get("btn.train"));
        trainBtn.addActionListener(e -> startTraining());
        JButton stopBtn = new JButton(Lang.get("btn.stop"));
        stopBtn.addActionListener(e -> stopTraining());
        JButton curveBtn = new JButton(Lang.get("btn.curve"));
        curveBtn.addActionListener(e -> showCurve());
        JButton evolveBtn = new JButton("进化训练");
        evolveBtn.addActionListener(e -> startEvolution());
        trainBtnPanel.add(trainBtn);
        trainBtnPanel.add(stopBtn);
        trainBtnPanel.add(curveBtn);
        trainBtnPanel.add(evolveBtn);

        JPanel trainContainer = new JPanel(new BorderLayout());
        trainContainer.add(trainPanel, BorderLayout.NORTH);
        trainContainer.add(trainBtnPanel, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        progressPanel.add(progressBar, BorderLayout.NORTH);
        lossLabel = new JLabel(Lang.get("label.loss") + "0.0");
        statusLabel = new JLabel(Lang.get("label.status"));
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.add(statusLabel);
        statusPanel.add(lossLabel);
        progressPanel.add(statusPanel, BorderLayout.SOUTH);

        JPanel inferPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inferPanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.infer")));
        JButton inferBtn = new JButton(Lang.get("btn.infer"));
        inferBtn.addActionListener(e -> doInference());
        inferPanel.add(inferBtn);

        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statPanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.stat")));
        statLabel = new JLabel(Lang.get("label.stat"));
        statPanel.add(statLabel);

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        savePanel.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.save")));
        JButton saveBtn = new JButton(Lang.get("btn.save"));
        saveBtn.addActionListener(e -> saveModel());
        savePanel.add(saveBtn);

        logArea = new JTextArea(15, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(Lang.get("panel.log")));

        JPanel rightTop = new JPanel(new GridLayout(5, 1, 5, 5));
        rightTop.add(samplePanel);
        rightTop.add(trainContainer);
        rightTop.add(progressPanel);
        rightTop.add(inferPanel);
        rightTop.add(statPanel);

        rightPanel.add(rightTop, BorderLayout.NORTH);
        rightPanel.add(savePanel, BorderLayout.CENTER);
        rightPanel.add(logScroll, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        // Tab 页
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("模型工坊", mainPanel);
        tabbedPane.addTab("AI 聊天", new ChatPanel());
        add(tabbedPane);
    }

    private void initStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.add(new JLabel(" " + Lang.get("label.status")), BorderLayout.WEST);
        pathLabel = new JLabel(Lang.get("label.path") + modelDir + " ");
        statusBar.add(pathLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ============================================================
    // 模型操作
    // ============================================================

    private void createNewModel() {
        String name = modelNameField.getText().trim();
        if (name.isEmpty()) name = "我的模型";

        String author = modelAuthorField.getText().trim();
        if (author.isEmpty()) author = "AI工坊";

        int inputDim;
        try {
            inputDim = Integer.parseInt(inputDimField.getText().trim());
            if (inputDim < 1) inputDim = 1;
        } catch (Exception e) {
            inputDim = 3;
        }

        model = new Model(name, author);
        layers = new ArrayList<>();
        Layer inputLayer = new Layer("input", "input", inputDim, "none");
        layers.add(inputLayer);
        model.addLayer(inputLayer);
        sampleInputs = new ArrayList<>();
        sampleOutputs = new ArrayList<>();
        correctCount = 0;
        wrongCount = 0;
        trainLosses = new ArrayList<>();
        isTraining = false;

        updateLayerList();
        updateSampleCount();
        updateStats();
        log("模型已创建: " + name + " (输入 " + inputDim + " 维)");
    }

    private void addLayer() {
        if (model == null) {
            JOptionPane.showMessageDialog(this, "请先创建模型");
            return;
        }

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField nameField = new JTextField("layer_" + layers.size());
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"dense", "output"});
        JTextField neuronField = new JTextField("4");
        JComboBox<String> actCombo = new JComboBox<>(new String[]{"relu", "sigmoid", "tanh", "linear", "none"});

        panel.add(new JLabel("层名称:"));
        panel.add(nameField);
        panel.add(new JLabel("类型:"));
        panel.add(typeCombo);
        panel.add(new JLabel("神经元数量:"));
        panel.add(neuronField);
        panel.add(new JLabel("激活函数:"));
        panel.add(actCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加层",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "layer_" + layers.size();
            String type = (String) typeCombo.getSelectedItem();
            int neurons = Integer.parseInt(neuronField.getText().trim());
            String activation = (String) actCombo.getSelectedItem();

            Layer layer = new Layer(name, type, neurons, activation);
            layers.add(layer);
            model.addLayer(layer);
            updateLayerList();
            log("已添加层: " + name);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "输入格式错误: " + e.getMessage());
        }
    }

    private void editLayer() {
        int idx = layerList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "请先选择要编辑的层");
            return;
        }

        Layer layer = layers.get(idx);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField nameField = new JTextField(layer.name);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"dense", "output"});
        typeCombo.setSelectedItem(layer.type);
        JTextField neuronField = new JTextField(String.valueOf(layer.neurons));
        JComboBox<String> actCombo = new JComboBox<>(new String[]{"relu", "sigmoid", "tanh", "linear", "none"});
        actCombo.setSelectedItem(layer.activation);

        if (idx == 0) {
            typeCombo.setEnabled(false);
            neuronField.setEnabled(false);
        }

        panel.add(new JLabel("层名称:"));
        panel.add(nameField);
        panel.add(new JLabel("类型:"));
        panel.add(typeCombo);
        panel.add(new JLabel("神经元数量:"));
        panel.add(neuronField);
        panel.add(new JLabel("激活函数:"));
        panel.add(actCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "编辑层",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = layer.name;
            String type = (String) typeCombo.getSelectedItem();
            int neurons = Integer.parseInt(neuronField.getText().trim());
            String activation = (String) actCombo.getSelectedItem();

            layer.name = name;
            layer.type = type;
            layer.activation = activation;

            if (neurons != layer.neurons && idx > 0) {
                layer.neurons = neurons;
                rebuildModel();
            }

            updateLayerList();
            log("已修改层: " + name);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "输入格式错误: " + e.getMessage());
        }
    }

    private void rebuildModel() {
        Model newModel = new Model(model.name, model.author);
        for (Layer l : layers) {
            l.weights = null;
            l.biases = null;
            newModel.addLayer(l);
        }
        model = newModel;
    }

    private void deleteLayer() {
        int idx = layerList.getSelectedIndex();
        if (idx <= 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的层 (不能删除输入层)");
            return;
        }
        if (idx < layers.size()) {
            Layer removed = layers.get(idx);
            if (removed.type.equals("output")) {
                sampleInputs = new ArrayList<>();
                sampleOutputs = new ArrayList<>();
            }
            layers.remove(idx);
            model.layers = new ArrayList<>(layers);
            updateLayerList();
            updateSampleCount();
            log("已删除层: " + removed.name);
        }
    }

    private void updateLayerList() {
        layerListModel.clear();
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            String label = (i + 1) + ". " + layer.name +
                    " (" + layer.type + ", " + layer.neurons + "个, " + layer.activation + ")";
            if (i == 0) label += " <- 输入层";
            layerListModel.addElement(label);
        }
    }

    // ============================================================
    // 样本操作
    // ============================================================

    private void addSample() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }

        int inputDim = layers.get(0).neurons;
        int outputDim = 1;
        for (Layer layer : layers) {
            if (layer.type.equals("output")) {
                outputDim = layer.neurons;
                break;
            }
        }

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField inputField = new JTextField("1, 0.8, 1");
        JTextField outputField = new JTextField("0.9, 0.1");

        panel.add(new JLabel("输入 (" + inputDim + "维):"));
        panel.add(inputField);
        panel.add(new JLabel("输出 (" + outputDim + "维):"));
        panel.add(outputField);

        int result = JOptionPane.showConfirmDialog(this, panel, "添加样本",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String[] inParts = inputField.getText().trim().split(",");
            double[] inputs = new double[inParts.length];
            for (int i = 0; i < inParts.length; i++) {
                inputs[i] = Double.parseDouble(inParts[i].trim());
            }

            String[] outParts = outputField.getText().trim().split(",");
            double[] outputs = new double[outParts.length];
            for (int i = 0; i < outParts.length; i++) {
                outputs[i] = Double.parseDouble(outParts[i].trim());
            }

            if (inputs.length != inputDim) {
                JOptionPane.showMessageDialog(this, "输入维度应为 " + inputDim);
                return;
            }
            if (outputs.length != outputDim) {
                JOptionPane.showMessageDialog(this, "输出维度应为 " + outputDim);
                return;
            }

            sampleInputs.add(inputs);
            sampleOutputs.add(outputs);
            for (Layer layer : layers) {
                if (layer.type.equals("output")) {
                    layer.sampleInputs.add(inputs);
                    layer.sampleOutputs.add(outputs);
                    break;
                }
            }

            updateSampleCount();
            log("样本添加成功");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "格式错误: " + e.getMessage());
        }
    }

    private void showSamples() {
        if (sampleInputs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "暂无样本");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sampleInputs.size(); i++) {
            sb.append("样本 ").append(i + 1).append(": (");
            for (double v : sampleInputs.get(i)) sb.append(String.format("%.4f ", v));
            sb.append(") -> (");
            for (double v : sampleOutputs.get(i)) sb.append(String.format("%.4f ", v));
            sb.append(")\n");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 11));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scroll, "样本列表", JOptionPane.INFORMATION_MESSAGE);
    }

    private void importJsonSamples() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }

        JFileChooser chooser = new JFileChooser(modelDir);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JSON 样本", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File jsonFile = chooser.getSelectedFile();

        try {
            String json = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()), "UTF-8");
            Map<String, Object> root = JsonParser.asObject(new JsonParser(json).parse());

            int outputDim = JsonParser.asInt(root.get("output_dim"));
            int label = JsonParser.asInt(root.get("label"));
            List<Object> samples = JsonParser.asArray(root.get("samples"));

            if (samples == null || samples.isEmpty()) {
                JOptionPane.showMessageDialog(this, "JSON 里没有样本");
                return;
            }

            int modelOutputDim = layers.get(layers.size() - 1).neurons;
            if (outputDim != modelOutputDim) {
                JOptionPane.showMessageDialog(this,
                        "输出维度不匹配!\nJSON: " + outputDim + "\n模型: " + modelOutputDim);
                return;
            }

            if (label < 0 || label >= outputDim) {
                JOptionPane.showMessageDialog(this, "标签超出范围: " + label);
                return;
            }

            Map<String, Object> firstSample = JsonParser.asObject(samples.get(0));
            List<Object> firstInput = JsonParser.asArray(firstSample.get("input"));
            int jsonInputDim = firstInput.size();
            int modelInputDim = layers.get(0).neurons;

            if (jsonInputDim != modelInputDim) {
                JOptionPane.showMessageDialog(this,
                        "输入维度不匹配!\nJSON: " + jsonInputDim + "\n模型: " + modelInputDim);
                return;
            }

            int count = 0;
            for (Object sObj : samples) {
                Map<String, Object> s = JsonParser.asObject(sObj);
                List<Object> inputArr = JsonParser.asArray(s.get("input"));

                double[] inputs = new double[inputArr.size()];
                for (int i = 0; i < inputArr.size(); i++) {
                    inputs[i] = JsonParser.asDouble(inputArr.get(i));
                }

                double[] outputs = new double[outputDim];
                outputs[label] = 1.0;

                sampleInputs.add(inputs);
                sampleOutputs.add(outputs);
                for (Layer layer : layers) {
                    if (layer.type.equals("output")) {
                        layer.sampleInputs.add(inputs);
                        layer.sampleOutputs.add(outputs);
                        break;
                    }
                }
                count++;
            }

            updateSampleCount();
            log("已导入 " + count + " 个样本 (标签 " + label + ")");
            JOptionPane.showMessageDialog(this,
                    "导入成功!\n样本数: " + count + "\n标签: " + label);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "导入失败: " + e.getMessage());
        }
    }

    private void showRecommend() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }

        int totalParams = 0;
        for (int i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            Layer prev = layers.get(i - 1);
            totalParams += layer.neurons * prev.neurons + layer.neurons;
        }

        int recommended = Math.max(10, (int) (totalParams * 0.5));
        int current = sampleInputs.size();
        String status = current >= recommended ? "样本充足" : "建议增加样本";

        JOptionPane.showMessageDialog(this,
                "模型参数量: " + totalParams + "\n" +
                "推荐最少样本数: " + recommended + "\n" +
                "当前样本数: " + current + "\n" +
                "状态: " + status,
                "推荐样本数", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateSampleCount() {
        sampleCountLabel.setText(Lang.get("label.sampleCount") + sampleInputs.size());
    }

    private void updateStats() {
        int total = correctCount + wrongCount;
        double acc = total > 0 ? (double) correctCount / total * 100 : 0;
        statLabel.setText(String.format("正确: %d | 错误: %d | 准确率: %.1f%%",
                correctCount, wrongCount, acc));
    }

    // ============================================================
    // 训练
    // ============================================================

    private void startTraining() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }
        if (sampleInputs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先添加训练样本");
            return;
        }
        if (isTraining) {
            JOptionPane.showMessageDialog(this, "训练正在进行中");
            return;
        }

        int epochs;
        double lr;
        try {
            epochs = Integer.parseInt(epochsField.getText().trim());
            lr = Double.parseDouble(lrField.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "请输入有效的训练参数");
            return;
        }

        isTraining = true;
        trainLosses = new ArrayList<>();
        progressBar.setValue(0);

        currentWorker = new TrainWorker(
                model, sampleInputs, sampleOutputs,
                epochs, lr, autoLrCheck.isSelected(),
                progressBar, lossLabel, statusLabel, trainLosses);
        currentWorker.execute();
    }

    private void stopTraining() {
        if (currentWorker != null) {
            currentWorker.stop();
            isTraining = false;
            statusLabel.setText("已停止");
        }
        if (currentOptimizer != null) {
            currentOptimizer.stop();
        }
    }

    private void showCurve() {
        if (trainLosses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "暂无训练数据");
            return;
        }

        JFrame curveFrame = new JFrame("训练曲线");
        curveFrame.setSize(800, 500);
        curveFrame.setLocationRelativeTo(this);

        curveFrame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int pad = 50;

                g2.setColor(Color.BLACK);
                g2.drawLine(pad, h - pad, w - pad, h - pad);
                g2.drawLine(pad, pad, pad, h - pad);

                if (trainLosses.size() < 2) return;

                double maxLoss = 0;
                for (double l : trainLosses) {
                    if (l > maxLoss) maxLoss = l;
                }
                if (maxLoss <= 0) maxLoss = 1;

                g2.setColor(Color.BLUE);
                int n = trainLosses.size();
                int prevX = pad;
                int prevY = h - pad - (int) (trainLosses.get(0) / maxLoss * (h - 2 * pad));
                for (int i = 1; i < n; i++) {
                    int x = pad + (int) ((double) i / (n - 1) * (w - 2 * pad));
                    int y = h - pad - (int) (trainLosses.get(i) / maxLoss * (h - 2 * pad));
                    g2.drawLine(prevX, prevY, x, y);
                    prevX = x;
                    prevY = y;
                }

                g2.setColor(Color.BLACK);
                g2.drawString("Loss", 10, 20);
                g2.drawString("Epoch", w - 60, h - 10);
                g2.drawString(String.format("Max: %.6f", maxLoss), 10, 40);
            }
        });

        curveFrame.setVisible(true);
    }

    private void startEvolution() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }
        if (sampleInputs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先添加训练样本");
            return;
        }
        if (isTraining) {
            JOptionPane.showMessageDialog(this, "请先停止普通训练");
            return;
        }

        int inputDim = layers.get(0).neurons;
        int outputDim = layers.get(layers.size() - 1).neurons;

        JDialog dialog = new JDialog(this, "遗传算法进化训练", false);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JTextArea evoLog = new JTextArea();
        evoLog.setEditable(false);
        evoLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(evoLog);

        JProgressBar evoProgress = new JProgressBar(0, 100);
        JButton stopBtn = new JButton("停止");
        JButton closeBtn = new JButton("关闭");
        closeBtn.setEnabled(false);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(stopBtn);
        btnPanel.add(closeBtn);
        bottom.add(evoProgress, BorderLayout.CENTER);
        bottom.add(btnPanel, BorderLayout.EAST);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);

        evoLog.append("遗传算法启动...\n");
        evoLog.append("种群大小: 10, 最大代数: 20\n\n");

        GeneticOptimizer optimizer = new GeneticOptimizer(
                sampleInputs, sampleOutputs, inputDim, outputDim);
        optimizer.setTargetAccuracy(0.90);
        currentOptimizer = optimizer;

        stopBtn.addActionListener(e -> {
            optimizer.stop();
            evoLog.append("正在停止...\n");
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        new Thread(() -> {
            optimizer.run((gen, acc) -> {
                SwingUtilities.invokeLater(() -> {
                    evoLog.append(String.format("第 %d 代: 准确率 %.2f%%\n", gen, acc * 100));
                    evoLog.setCaretPosition(evoLog.getDocument().getLength());
                    evoProgress.setValue((int)(acc * 100));
                });
            });

            SwingUtilities.invokeLater(() -> {
                Model best = optimizer.getBestModel();
                if (best != null) {
                    model = best;
                    layers = new ArrayList<>(best.layers);

                    sampleInputs = new ArrayList<>();
                    sampleOutputs = new ArrayList<>();
                    for (Layer layer : layers) {
                        if (layer.type.equals("output")) {
                            sampleInputs = new ArrayList<>(layer.sampleInputs);
                            sampleOutputs = new ArrayList<>(layer.sampleOutputs);
                            break;
                        }
                    }

                    updateLayerList();
                    updateSampleCount();
                    updateStats();

                    isTraining = false;
                    currentWorker = null;
                    currentOptimizer = null;

                    evoLog.append(String.format("\n完成! 最佳准确率: %.2f%%\n",
                            optimizer.getBestAccuracy() * 100));
                    evoLog.append("已自动加载最优模型\n");

                    closeBtn.setEnabled(true);
                    stopBtn.setEnabled(false);

                    JOptionPane.showMessageDialog(dialog,
                            "进化完成!\n最佳准确率: " +
                            String.format("%.2f%%", optimizer.getBestAccuracy() * 100));
                } else {
                    evoLog.append("\n未找到有效模型\n");
                    closeBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    currentOptimizer = null;
                }
            });
        }).start();
    }

    // ============================================================
    // 推理
    // ============================================================

    private void doInference() {
        if (model == null || layers.size() < 2) {
            JOptionPane.showMessageDialog(this, "请先创建模型并添加层");
            return;
        }

        int inputDim = layers.get(0).neurons;

        String inputStr = JOptionPane.showInputDialog(this,
                "输入 (" + inputDim + "维, 逗号分隔):", "");
        if (inputStr == null) return;

        try {
            String[] parts = inputStr.trim().split(",");
            double[] inputs = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                inputs[i] = Double.parseDouble(parts[i].trim());
            }

            if (inputs.length != inputDim) {
                JOptionPane.showMessageDialog(this, "输入维度应为 " + inputDim);
                return;
            }

            double[] result = model.predict(inputs);

            StringBuilder sb = new StringBuilder();
            sb.append("推理结果:\n\n");
            for (int i = 0; i < result.length; i++) {
                sb.append(String.format("  输出%d: %.4f (%.2f%%)\n",
                        i + 1, result[i], result[i] * 100));
            }

            int maxIdx = 0;
            for (int i = 1; i < result.length; i++) {
                if (result[i] > result[maxIdx]) maxIdx = i;
            }
            sb.append(String.format("\n最可能: 输出%d (%.2f%%)",
                    maxIdx + 1, result[maxIdx] * 100));

            int choice = JOptionPane.showConfirmDialog(this, sb.toString(),
                    "推理结果", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                feedback(inputs, maxIdx, result);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "错误: " + e.getMessage());
        }
    }

    private void feedback(double[] inputs, int predictedLabel, double[] result) {
        int outputDim = layers.get(layers.size() - 1).neurons;
        double[] target = new double[outputDim];
        if (predictedLabel < outputDim) target[predictedLabel] = 1.0;

        model.trainEpoch(
                java.util.Collections.singletonList(inputs),
                java.util.Collections.singletonList(target),
                0.001);

        sampleInputs.add(inputs.clone());
        sampleOutputs.add(target.clone());
        for (Layer layer : layers) {
            if (layer.type.equals("output")) {
                layer.sampleInputs.add(inputs.clone());
                layer.sampleOutputs.add(target.clone());
                break;
            }
        }

        correctCount++;
        updateSampleCount();
        updateStats();
        log("正确反馈! 样本数: " + sampleInputs.size());
    }

    // ============================================================
    // 保存 / 加载
    // ============================================================

    private void saveModel() {
        if (model == null) {
            JOptionPane.showMessageDialog(this, "没有模型可保存");
            return;
        }

        String baseName = model.name.replace(" ", "_");
        if (baseName.isEmpty()) baseName = "model";

        String filePath = modelDir + File.separator + baseName + ".msmodel";

        try {
            ModelIO.save(model, filePath);
            JOptionPane.showMessageDialog(this, "模型已保存:\n" + filePath);
            log("模型已保存: " + filePath);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage());
        }
    }

    private void importModel() {
        JFileChooser chooser = new JFileChooser(modelDir);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "AI 模型文件 (*.msmodel)", "msmodel"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String filePath = chooser.getSelectedFile().getAbsolutePath();

        try {
            Model loaded = ModelIO.load(filePath);
            if (loaded != null) {
                model = loaded;
                layers = new ArrayList<>(model.layers);
                sampleInputs = new ArrayList<>();
                sampleOutputs = new ArrayList<>();
                for (Layer layer : layers) {
                    if (layer.type.equals("output")) {
                        sampleInputs = new ArrayList<>(layer.sampleInputs);
                        sampleOutputs = new ArrayList<>(layer.sampleOutputs);
                        break;
                    }
                }
                correctCount = 0;
                wrongCount = 0;
                trainLosses = new ArrayList<>();
                isTraining = false;

                updateLayerList();
                updateSampleCount();
                updateStats();
                log("模型已加载: " + model.name);
                JOptionPane.showMessageDialog(this,
                        "模型已加载: " + model.name + "\n样本数: " + sampleInputs.size());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载失败: " + e.getMessage());
        }
    }

    // ========== 保存路径到配置 ==========
    private void setModelPath() {
        JFileChooser chooser = new JFileChooser(modelDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        modelDir = chooser.getSelectedFile().getAbsolutePath();

        // 保存到配置
        ConfigManager.setModelDir(modelDir);

        pathLabel.setText(Lang.get("label.path") + modelDir + " ");
        log("模型路径已更新: " + modelDir);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "AI 模型工坊 - Java 版\n\n" +
                "版本: 2.2\n\n" +
                "功能:\n" +
                "  - 创建/编辑神经网络\n" +
                "  - 编辑层（神经元/激活函数）\n" +
                "  - 训练 + 自适应学习率\n" +
                "  - 推理 + 反馈学习\n" +
                "  - 遗传算法进化训练\n" +
                "  - 导入 JSON 样本\n" +
                "  - 模型保存 (.msmodel)\n" +
                "  - AI 聊天\n" +
                "  - 语言切换\n" +
                "  - 设置自动保存",
                "关于", JOptionPane.INFORMATION_MESSAGE);
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}