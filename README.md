# EasyAI

一个用 Java Swing 写的简易神经网络工具，用来练手和学习。

A simple neural network tool built with Java Swing, made for learning and practice.

---

## 这是什么 / What is this

自己写着玩的项目，主要目的是学习神经网络的基本原理。功能比较简陋，代码也写得一般，凑合能用。

A personal learning project for understanding the basics of neural networks. The code is simple and not polished, but it works.

---

## 功能 / Features

- 创建模型（输入层、隐藏层、输出层）/ Create models (input, hidden, output layers)
- 添加、删除、编辑层 / Add, delete, edit layers
- 支持激活函数：ReLU、Sigmoid、Tanh、Linear / Activation functions: ReLU, Sigmoid, Tanh, Linear
- 训练（可停止，有进度条和损失显示）/ Training (stoppable, with progress and loss display)
- 自适应学习率 / Adaptive learning rate
- 推理，查看概率分布 / Inference with probability output
- 反馈学习（正确奖励 / 错误纠正）/ Feedback learning (reward / correct)
- 遗传算法进化训练 / Genetic algorithm evolution training
- 导入 JSON 样本 / Import JSON samples
- 保存、加载模型（.msmodel 格式）/ Save & load models (.msmodel format)
- 训练曲线 / Training curve
- 中英文切换 / Chinese & English UI
- AI 聊天（需自备 API Key）/ AI chat (API key required)

---

## 环境要求 / Requirements

- JDK 8 或更高版本 / JDK 8 or later
- 无额外依赖 / No extra dependencies

---

## 项目结构 / Project Structure
```
EasyAI/
├── src/com/ai/
│   ├── Main.java              入口 / Entry point
│   ├── GUI.java               界面 / Swing UI
│   ├── Model.java             神经网络核心 / Neural network core
│   ├── Layer.java             层数据 / Layer data
│   ├── TrainWorker.java       训练线程 / Training thread
│   ├── GeneticOptimizer.java  遗传算法 / Genetic algorithm
│   ├── ModelIO.java           模型存取 / Model I/O
│   ├── JsonParser.java        JSON 解析 / JSON parser
│   ├── Lang.java              语言管理 / Language manager
│   ├── ConfigManager.java     配置管理 / Config manager
│   └── ChatPanel.java         AI 聊天 / AI chat panel
├── lang/
│   ├── zh_CN.lang             中文
│   └── en_US.lang             英文 / English
└── manifest.txt
```
---

## 模型格式 / Model Format
.msmodel 是一个 ZIP 压缩包，包含三个文件：

.msmodel is a ZIP archive containing three files:

文件 / File	内容 / Content
model.json	结构信息（层数、神经元、激活函数）/ Structure info
weights.bin	权重和偏置（float32 二进制）/ Weights & biases (float32 binary)
samples.json	训练样本 / Training samples

---

## 已知问题 / Known Issues
- 推理有时返回 null，还没查出来 / Inference sometimes returns null, not fixed yet

- 图片识别功能没做完 可能会放弃/ Image recognition not finished,May give up.

- 代码写得乱，很多地方能优化 / Code is messy, lots of room for improvement

- 界面偶尔会卡 / UI sometimes freezes

---

## 待改进 / TODO
- 修复推理返回 null / Fix inference returning null

- 支持 CNN / Add CNN support

- 图片直接推理 / Image inference

- 多线程训练，提升速度 / Multi-threaded training

- 代码重构 / Code refactoring

## 说明 / Notes
纯学习项目，水平有限，代码质量一般，见谅。

This is a learning project. Code quality is average. Please bear with it.

有问题欢迎提 Issue，或者直接帮我改。

Issues and pull requests are welcome.

---

## 编译运行 / Build & Run

```
# 编译 / Compile
javac -encoding UTF-8 -d out src/com/ai/*.java

# 复制语言文件 / Copy language files
mkdir -p out/lang
cp lang/* out/lang/

# 打包 / Package
cd out
jar cfm ../MiniAI.jar ../manifest.txt com/ai/*.class lang/*.lang
cd ..

# 运行 / Run
java -jar MiniAI.jar
```

# 许可证 / License
MIT License

---

## 相关工具 / Related Tool

还有一个配套工具，可以把图片转换成神经网络输入参数。

There is also a companion tool that converts images into neural network input parameters.
