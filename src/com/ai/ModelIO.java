package com.ai;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.*;

public class ModelIO {

    public static void save(Model model, String basePath) throws IOException {
        if (!basePath.endsWith(".msmodel")) {
            basePath += ".msmodel";
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(basePath))) {
            zos.putNextEntry(new ZipEntry("model.json"));
            zos.write(buildStructureJson(model).getBytes("UTF-8"));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("weights.bin"));
            DataOutputStream dos = new DataOutputStream(zos);
            for (Layer layer : model.layers) {
                if (layer.weights != null) {
                    dos.writeInt(layer.weights.length);
                    for (double[] row : layer.weights) {
                        dos.writeInt(row.length);
                        for (double v : row) {
                            dos.writeFloat((float) v);
                        }
                    }
                } else {
                    dos.writeInt(0);
                }
                if (layer.biases != null) {
                    dos.writeInt(layer.biases.length);
                    for (double v : layer.biases) {
                        dos.writeFloat((float) v);
                    }
                } else {
                    dos.writeInt(0);
                }
            }
            dos.flush();
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("samples.json"));
            zos.write(buildSamplesJson(model).getBytes("UTF-8"));
            zos.closeEntry();
        }

        System.out.println("[OK] 模型已保存: " + basePath);
    }

    public static Model load(String filePath) throws IOException {
        Model model = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.equals("model.json")) {
                    String json = readAll(zis);
                    model = parseStructureJson(json);

                } else if (name.equals("weights.bin") && model != null) {
                    DataInputStream dis = new DataInputStream(zis);
                    for (Layer layer : model.layers) {
                        if (layer.type.equals("dense") || layer.type.equals("output")) {
                            int rows = dis.readInt();
                            if (rows > 0) {
                                layer.weights = new double[rows][];
                                for (int i = 0; i < rows; i++) {
                                    int cols = dis.readInt();
                                    layer.weights[i] = new double[cols];
                                    for (int j = 0; j < cols; j++) {
                                        layer.weights[i][j] = dis.readFloat();
                                    }
                                }
                                int biasLen = dis.readInt();
                                layer.biases = new double[biasLen];
                                for (int i = 0; i < biasLen; i++) {
                                    layer.biases[i] = dis.readFloat();
                                }
                            }
                        }
                    }

                } else if (name.equals("samples.json") && model != null) {
                    String json = readAll(zis);
                    parseSamplesJson(json, model);
                }

                zis.closeEntry();
            }
        }

        return model;
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
            baos.write(buf, 0, len);
        }
        return baos.toString("UTF-8");
    }

    private static String buildStructureJson(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escape(model.name)).append("\",\n");
        sb.append("  \"author\": \"").append(escape(model.author)).append("\",\n");
        sb.append("  \"version\": \"1.0\",\n");
        sb.append("  \"loss\": ").append(model.lastLoss).append(",\n");
        sb.append("  \"layers\": [\n");

        for (int i = 0; i < model.layers.size(); i++) {
            Layer layer = model.layers.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escape(layer.name)).append("\",\n");
            sb.append("      \"type\": \"").append(layer.type).append("\",\n");
            sb.append("      \"neurons\": ").append(layer.neurons).append(",\n");
            sb.append("      \"activation\": \"").append(layer.activation).append("\"\n");
            sb.append("    }");
            if (i < model.layers.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildSamplesJson(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        List<double[]> inputs = null;
        List<double[]> outputs = null;
        for (Layer layer : model.layers) {
            if (layer.type.equals("output")) {
                inputs = layer.sampleInputs;
                outputs = layer.sampleOutputs;
                break;
            }
        }

        if (inputs == null) {
            sb.append("]\n");
            return sb.toString();
        }

        for (int i = 0; i < inputs.size(); i++) {
            sb.append("  {\"input\": [");
            for (int j = 0; j < inputs.get(i).length; j++) {
                sb.append(inputs.get(i)[j]);
                if (j < inputs.get(i).length - 1) sb.append(", ");
            }
            sb.append("], \"output\": [");
            for (int j = 0; j < outputs.get(i).length; j++) {
                sb.append(outputs.get(i)[j]);
                if (j < outputs.get(i).length - 1) sb.append(", ");
            }
            sb.append("]}");
            if (i < inputs.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("]\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Model parseStructureJson(String json) {
        Map<String, Object> obj = JsonParser.asObject(new JsonParser(json).parse());
        Model model = new Model(
                JsonParser.asString(obj.get("name")),
                JsonParser.asString(obj.get("author"))
        );
        Object lossObj = obj.get("loss");
        if (lossObj != null) {
            model.lastLoss = JsonParser.asDouble(lossObj);
        }

        List<Object> layersArr = JsonParser.asArray(obj.get("layers"));
        for (Object lObj : layersArr) {
            Map<String, Object> l = JsonParser.asObject(lObj);
            Layer layer = new Layer(
                    JsonParser.asString(l.get("name")),
                    JsonParser.asString(l.get("type")),
                    JsonParser.asInt(l.get("neurons")),
                    JsonParser.asString(l.get("activation"))
            );
            model.layers.add(layer);
        }

        return model;
    }

    @SuppressWarnings("unchecked")
    private static void parseSamplesJson(String json, Model model) {
        try {
            List<Object> arr = JsonParser.asArray(new JsonParser(json).parse());
            Layer outputLayer = null;
            for (Layer layer : model.layers) {
                if (layer.type.equals("output")) {
                    outputLayer = layer;
                    break;
                }
            }
            if (outputLayer == null) return;

            for (Object sObj : arr) {
                Map<String, Object> s = JsonParser.asObject(sObj);
                List<Object> inArr = JsonParser.asArray(s.get("input"));
                List<Object> outArr = JsonParser.asArray(s.get("output"));

                double[] inputs = new double[inArr.size()];
                for (int j = 0; j < inArr.size(); j++) {
                    inputs[j] = JsonParser.asDouble(inArr.get(j));
                }

                double[] outputs = new double[outArr.size()];
                for (int j = 0; j < outArr.size(); j++) {
                    outputs[j] = JsonParser.asDouble(outArr.get(j));
                }

                outputLayer.sampleInputs.add(inputs);
                outputLayer.sampleOutputs.add(outputs);
            }
        } catch (Exception e) {
            System.out.println("解析样本失败: " + e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}