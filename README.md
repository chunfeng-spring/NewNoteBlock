NewNoteBlock is a Fabric mod designed to raise the ceiling of NoteBlock music, whether using vanilla Minecraft sounds or external custom sounds.

🚀 Features
1. NewNoteBlock GUI
🎹 Basic Properties

Instrument Selection: Category-based selector (e.g., Piano, Bass, Guitar) breaking vanilla limitations.

Piano Roll: Clickable keys to preview and select pitch (MIDI 0-127) instantly.

Volume Envelope: Visual volume curve editor for drawing complex dynamics like fade-ins and fade-outs.

Pitch Envelope: Visual pitch curve editor mimicking DAW automation, enabling effects like Pitch Bend and Vibrato.

Duration Control: Precise control over note duration (in Ticks) with auto-adapting envelopes.

Playback Delay: Millisecond-level (0-5000ms) delay settings with a built-in helper calculator.

Real-time Spectrogram: Built-in audio spectrum analyzer displaying frequency response in real-time.

🎚️ Mixer

Reverb: OpenAL-based reverb effector with adjustable Send, Density, Diffusion, Decay Time, Reflection Gain, etc., plus various presets (Hall, Room).

EQ (Equalizer): 3-band parametric EQ (Low, Mid, High), with adjustable Frequency, Gain, and Q-factor for each band.

📦 Motion (Dynamic Audio source)

Trajectory Expressions: Define movement functions for X, Y, Z axes using mathematical expressions (supports variable t).

Coordinate Modes: Supports both Relative (local to the block) and Absolute (world coordinates) modes.

Trajectory Preview: 3D coordinate system window visualizing the calculated sound path in real-time.

2. WorldEdit GUI
🔍 Filters

Conditional Filtering: Filter blocks within a selection by Instrument Type (e.g., Piano only) or Pitch Range (e.g., C4-C5 only), supporting Intersect/Union/Complement logic.

🎯 Target Editor

Selective Overwrite: Use checkboxes (Masks) to overwrite specific properties only (e.g., change Instrument without affecting existing Pitch or Envelopes).

Batch Editing: Reuses the full NewNoteBlock interface to define target values and apply them to all matching blocks in the selection with one click.

➕ Math Operators

Relative Adjustment: Perform +, -, x, ÷ operations on properties of blocks in the selection.

Supported Properties: Pitch (semitone transposition), Master Volume, Envelope Scale, Playback Delay (global offset), Reverb Send/Gain, and Motion Expressions.



NewNoteBlock 是一个 Fabric 模组，旨在为了提高音符盒音乐的上限，不管是用 Minecraft 原版音色还是外部音色。

🚀 功能特性
一、 NewNoteBlock GUI
🎹 基础属性调节

乐器选择：基于分类的乐器选择器（如 Piano, Bass, Guitar 等），突破原版乐器限制。

钢琴卷帘 (Piano Roll)：可直接点击琴键试听并选择音高（MIDI 0-127）。

音量包络：可视化的音量曲线编辑器，可随意绘制复杂的音量变化（如渐强、渐弱）。

音高包络：可视化的音高曲线编辑器，模仿编曲软件的风格，可实现滑音（Pitch Bend）和颤音等效果。

时长控制：可精确控制音符的时长（Tick），包络线会自动适应时长。

播放延迟：支持毫秒级（0-5000ms）的播放延迟设置，并附带辅助计算器。

实时频谱图：内置音频频谱分析仪，实时显示当前声音的频率响应。

🎚️ 混音台 (Mixer)

混响 (Reverb)：OpenAL 的混响效果器，支持调节发送量（Send）、密度、扩散、衰减时间、反射增益等许多参数，并提供多种预设（如 Hall, Room 等）。

均衡器 (EQ)：三段式参数均衡器（Low, Mid, High），每段均可调节频率（Freq）、增益（Gain）和 Q 值。

📦 声源移动 (Motion)

轨迹表达式：支持使用数学表达式（支持变量 t）分别为 X, Y, Z 轴定义运动轨迹函数。

坐标模式：支持相对坐标（相对于音符盒本身）和绝对坐标（世界坐标）两种模式。

轨迹预览：提供 3D 坐标系预览窗口，实时绘制计算出的声源运动路径。

二、 WorldEdit GUI
🔍 筛选器 (Filters)

条件筛选：支持根据乐器类型（如仅选中钢琴）或音高范围（如仅选中 C4-C5）来过滤选区内的音符盒，支持交集并集补集的逻辑。

🎯 属性覆写 (Target Editor)

选择性覆盖：提供复选框（Mask），允许仅覆盖特定的属性（如只修改乐器，而不改变原有的音高和包络）。

批量编辑：复用 NewNoteBlock 的完整编辑界面，设定好目标值后，一键应用到选区内所有满足条件的音符盒。

➕ 数学运算 (Operators)

相对调整：支持对选区内的音符盒属性进行 + - x ÷ 运算。

支持属性：包括音高（半音平移）、主音量、包络比例、播放延迟（整体偏移）、混响发送量/增益、以及运动轨迹表达式。
