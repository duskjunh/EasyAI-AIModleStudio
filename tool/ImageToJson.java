import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 图片转 JSON 样本工具
 * 功能：选择文件夹 → 自动把图片转成 JSON 样本（带压缩）
 */
public class ImageToJson extends JFrame {

    private JTextField folderField;
    private JComboBox<String> sizeCombo;
    private JComboBox<String> levelsCombo;
    private JComboBox<String> blockCombo;
    private JCheckBox colorCheck;
    private JTextArea logArea;
    private JProgressBar progressBar;

    public ImageToJson() {
        super("图片转 JSON 样本工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 600);
        setLocationRelativeTo(null);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ========== 顶部：配置 ==========
        JPanel configPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("配置"));

        // 文件夹
        configPanel.add(new JLabel("图片文件夹:"));
        JPanel folderPanel = new JPanel(new BorderLayout(5, 0));
        folderField = new JTextField();
        JButton browseBtn = new JButton("浏览...");
        browseBtn.addActionListener(e -> browseFolder());
        folderPanel.add(folderField, BorderLayout.CENTER);
        folderPanel.add(browseBtn, BorderLayout.EAST);
        configPanel.add(folderPanel);

        // 尺寸
        configPanel.add(new JLabel("缩放尺寸:"));
        sizeCombo = new JComboBox<>(new String[]{
                "16×16", "24×24", "28×28", "32×32", "48×48", "64×64"
        });
        sizeCombo.setSelectedItem("32×32");
        configPanel.add(sizeCombo);

        // 量化等级
        configPanel.add(new JLabel("量化等级:"));
        levelsCombo = new JComboBox<>(new String[]{
                "256 (不量化)", "16", "8", "6", "4", "2"
        });
        levelsCombo.setSelectedItem("6");
        configPanel.add(levelsCombo);

        // 分块
        configPanel.add(new JLabel("分块大小:"));
        blockCombo = new JComboBox<>(new String[]{
                "1 (不分块)", "2×2", "4×4", "8×8"
        });
        blockCombo.setSelectedItem("4×4");
        configPanel.add(blockCombo);

        // 颜色
        configPanel.add(new JLabel("颜色模式:"));
        colorCheck = new JCheckBox("彩色（RGB）", false);
        configPanel.add(colorCheck);

        mainPanel.add(configPanel, BorderLayout.NORTH);

        // ========== 中间：日志 ==========
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("日志"));
        mainPanel.add(scroll, BorderLayout.CENTER);

        // ========== 底部：进度条 + 按钮 ==========
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        progressBar = new JProgressBar(0, 100);
        bottomPanel.add(progressBar, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton previewBtn = new JButton("预览压缩结果");
        previewBtn.addActionListener(e -> previewSample());
        JButton startBtn = new JButton("开始转换");
        startBtn.addActionListener(e -> startConvert());
        JButton exitBtn = new JButton("退出");
        exitBtn.addActionListener(e -> System.exit(0));
        btnPanel.add(previewBtn);
        btnPanel.add(startBtn);
        btnPanel.add(exitBtn);

        bottomPanel.add(btnPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void browseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择图片文件夹");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            folderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startConvert() {
        String folderPath = folderField.getText().trim();
        if (folderPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择文件夹");
            return;
        }

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this, "文件夹不存在");
            return;
        }

        // 解析配置
        String sizeStr = (String) sizeCombo.getSelectedItem();
        int size = Integer.parseInt(sizeStr.split("×")[0]);

        String levelsStr = (String) levelsCombo.getSelectedItem();
        int levels = Integer.parseInt(levelsStr.split(" ")[0]);

        String blockStr = (String) blockCombo.getSelectedItem();
        int blockSize = blockStr.startsWith("1") ? 1 : Integer.parseInt(blockStr.split("×")[0]);

        boolean color = colorCheck.isSelected();

        // 询问标签
        String labelStr = JOptionPane.showInputDialog(this,
                "这个文件夹对应哪个类别标签？\n(比如：猫输入 0，狗输入 1)",
                "0");
        if (labelStr == null) return;

        int label;
        try {
            label = Integer.parseInt(labelStr.trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "标签格式错误");
            return;
        }

        // 询问输出维度
        String outputDimStr = JOptionPane.showInputDialog(this,
                "输出维度是多少？\n(比如：猫狗二分类输入 2)",
                "2");
        if (outputDimStr == null) return;

        int outputDim;
        try {
            outputDim = Integer.parseInt(outputDimStr.trim());
            if (label >= outputDim) {
                JOptionPane.showMessageDialog(this, "标签超出输出维度范围");
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "输出维度格式错误");
            return;
        }

        // 后台线程
        final int fSize = size;
        final int fLevels = levels;
        final int fBlockSize = blockSize;
        final boolean fColor = color;
        final int fLabel = label;
        final int fOutputDim = outputDim;

        new Thread(() -> convertFolder(folder, fSize, fLevels, fBlockSize,
                fColor, fLabel, fOutputDim)).start();
    }

    private void convertFolder(File folder, int size, int levels, int blockSize,
                               boolean color, int label, int outputDim) {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "文件夹为空"));
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));

        // 过滤图片
        List<File> imageFiles = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".bmp")
                    || name.endsWith(".gif")) {
                imageFiles.add(f);
            }
        }

        if (imageFiles.isEmpty()) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "文件夹里没有图片"));
            return;
        }

        // 计算输出维度
        int outSize = size / blockSize;
        int inputDim = color ? outSize * outSize * 3 : outSize * outSize;

        log("========================================");
        log("开始转换");
        log("文件夹: " + folder.getAbsolutePath());
        log("图片数量: " + imageFiles.size());
        log("缩放尺寸: " + size + "×" + size);
        log("量化等级: " + levels);
        log("分块大小: " + blockSize + "×" + blockSize);
        log("颜色模式: " + (color ? "彩色" : "灰度"));
        log("输出维度: " + inputDim);
        log("标签: " + label);
        log("========================================");

        // 生成 JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"image_size\": ").append(size).append(",\n");
        json.append("  \"levels\": ").append(levels).append(",\n");
        json.append("  \"block_size\": ").append(blockSize).append(",\n");
        json.append("  \"color\": ").append(color).append(",\n");
        json.append("  \"input_dim\": ").append(inputDim).append(",\n");
        json.append("  \"output_dim\": ").append(outputDim).append(",\n");
        json.append("  \"label\": ").append(label).append(",\n");
        json.append("  \"samples\": [\n");

        int success = 0;
        int fail = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < imageFiles.size(); i++) {
            File file = imageFiles.get(i);

            try {
                double[] input = color
                        ? compressColor(file, size, levels, blockSize)
                        : compressGray(file, size, levels, blockSize);

                if (i > 0) json.append(",\n");
                json.append("    {\"file\": \"").append(escape(file.getName())).append("\", ");
                json.append("\"input\": ");
                json.append(arrayToJson(input));
                json.append("}");

                success++;

                if ((i + 1) % 10 == 0 || i == imageFiles.size() - 1) {
                    final int progress = (i + 1) * 100 / imageFiles.size();
                    final int current = i + 1;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        log("进度: " + current + "/" + imageFiles.size()
                                + " (" + progress + "%)");
                    });
                }
            } catch (Exception e) {
                fail++;
                log("失败: " + file.getName() + " - " + e.getMessage());
            }
        }

        json.append("\n  ]\n");
        json.append("}\n");

        long elapsed = System.currentTimeMillis() - startTime;

        // 保存
        String outputName = folder.getName() + "_label" + label + ".json";
        File outputFile = new File(folder.getParentFile(), outputName);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), "UTF-8"))) {
            pw.write(json.toString());
        } catch (Exception e) {
            log("保存失败: " + e.getMessage());
            return;
        }

        long fileSize = outputFile.length();

        log("========================================");
        log("转换完成!");
        log("成功: " + success);
        log("失败: " + fail);
        log("耗时: " + String.format("%.1f", elapsed / 1000.0) + " 秒");
        log("输出文件: " + outputFile.getAbsolutePath());
        log("文件大小: " + String.format("%.2f", fileSize / 1024.0) + " KB");
        log("========================================");

        final int fSuccess = success;
        final int fFail = fail;
        final String fOutputPath = outputFile.getAbsolutePath();

        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(100);
            JOptionPane.showMessageDialog(this,
                    "转换完成!\n\n" +
                    "成功: " + fSuccess + "\n" +
                    "失败: " + fFail + "\n" +
                    "输出: " + fOutputPath);
        });
    }

    // ============================================================
    // 灰度压缩
    // ============================================================

    private double[] compressGray(File file, int size, int levels, int blockSize)
            throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new Exception("无法读取");

        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();

        // 量化
        int step = 256 / levels;
        int[] quantized = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int rgb = resized.getRGB(x, y);
                int gray = rgb & 0xFF;
                int q = gray / step;
                if (q >= levels) q = levels - 1;
                quantized[y * size + x] = q;
            }
        }

        // 分块平均
        return blockAverage(quantized, size, blockSize, levels);
    }

    // ============================================================
    // 彩色压缩
    // ============================================================

    private double[] compressColor(File file, int size, int levels, int blockSize)
            throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new Exception("无法读取");

        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, size, size, null);
        g.dispose();

        // 分别处理 RGB
        int[] r = new int[size * size];
        int[] gChan = new int[size * size];
        int[] b = new int[size * size];
        int step = 256 / levels;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int rgb = resized.getRGB(x, y);
                int rv = (rgb >> 16) & 0xFF;
                int gv = (rgb >> 8) & 0xFF;
                int bv = rgb & 0xFF;

                r[y * size + x] = Math.min(levels - 1, rv / step);
                gChan[y * size + x] = Math.min(levels - 1, gv / step);
                b[y * size + x] = Math.min(levels - 1, bv / step);
            }
        }

        // 合并
        double[] rAvg = blockAverage(r, size, blockSize, levels);
        double[] gAvg = blockAverage(gChan, size, blockSize, levels);
        double[] bAvg = blockAverage(b, size, blockSize, levels);

        double[] result = new double[rAvg.length * 3];
        for (int i = 0; i < rAvg.length; i++) {
            result[i * 3] = rAvg[i];
            result[i * 3 + 1] = gAvg[i];
            result[i * 3 + 2] = bAvg[i];
        }
        return result;
    }

    // ============================================================
    // 分块平均
    // ============================================================

    private double[] blockAverage(int[] img, int size, int blockSize, int levels) {
        int outSize = size / blockSize;
        double[] out = new double[outSize * outSize];
        double maxVal = levels - 1;

        for (int by = 0; by < outSize; by++) {
            for (int bx = 0; bx < outSize; bx++) {
                double sum = 0;
                for (int y = 0; y < blockSize; y++) {
                    for (int x = 0; x < blockSize; x++) {
                        sum += img[(by * blockSize + y) * size + (bx * blockSize + x)];
                    }
                }
                out[by * outSize + bx] = (sum / (blockSize * blockSize)) / maxVal;
            }
        }
        return out;
    }

    // ============================================================
    // JSON 工具
    // ============================================================

    private String arrayToJson(double[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%.4f", arr[i]));
            if (i < arr.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ============================================================
    // 预览
    // ============================================================

    private void previewSample() {
        String folderPath = folderField.getText().trim();
        if (folderPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择文件夹");
            return;
        }

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, "文件夹为空");
            return;
        }

        File firstImage = null;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png") || name.endsWith(".bmp")) {
                firstImage = f;
                break;
            }
        }

        if (firstImage == null) {
            JOptionPane.showMessageDialog(this, "找不到图片");
            return;
        }

        try {
            String sizeStr = (String) sizeCombo.getSelectedItem();
            int size = Integer.parseInt(sizeStr.split("×")[0]);

            String levelsStr = (String) levelsCombo.getSelectedItem();
            int levels = Integer.parseInt(levelsStr.split(" ")[0]);

            String blockStr = (String) blockCombo.getSelectedItem();
            int blockSize = blockStr.startsWith("1") ? 1 : Integer.parseInt(blockStr.split("×")[0]);

            boolean color = colorCheck.isSelected();

            double[] input = color
                    ? compressColor(firstImage, size, levels, blockSize)
                    : compressGray(firstImage, size, levels, blockSize);

            JTextArea area = new JTextArea(20, 60);
            area.setEditable(false);
            area.setFont(new Font("Consolas", Font.PLAIN, 11));
            area.append("文件: " + firstImage.getName() + "\n");
            area.append("缩放: " + size + "×" + size + "\n");
            area.append("量化: " + levels + " 级\n");
            area.append("分块: " + blockSize + "×" + blockSize + "\n");
            area.append("颜色: " + (color ? "彩色" : "灰度") + "\n");
            area.append("输入维度: " + input.length + "\n\n");
            area.append("前 100 个值:\n");
            for (int i = 0; i < Math.min(100, input.length); i++) {
                area.append(String.format("%.4f ", input[i]));
                if ((i + 1) % 10 == 0) area.append("\n");
            }

            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(600, 450));
            JOptionPane.showMessageDialog(this, scroll, "压缩预览",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "预览失败: " + e.getMessage());
        }
    }

    // ============================================================
    // 入口
    // ============================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 忽略
        }
        SwingUtilities.invokeLater(ImageToJson::new);
    }
}