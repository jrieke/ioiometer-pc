import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Paint;
import org.jfree.chart.ChartFactory;
import java.awt.GridLayout;
import org.jfree.chart.JFreeChart;
import java.awt.Dimension;
import org.jfree.chart.ChartPanel;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JColorChooser;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import java.awt.LayoutManager;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager;
import java.awt.Window;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.AnalogInput;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import javax.swing.JSlider;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.plot.XYPlot;
import java.awt.Color;
import ioio.lib.util.pc.IOIOSwingApp;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import org.jfree.data.xy.XYDataItem;

// 
// Decompiled by Procyon v0.5.30
// 

public class TestClass extends IOIOSwingApp
{
    private boolean[] pinEnabled;
    private boolean paused;
    protected Color[] colors;
    private Color[] allColors;
    private XYPlot[] plots;
    protected final int firstPin = 31;
    protected final int lastPin = 42;
    protected final int numPins = 12;
    protected XYSeries[] xySeriesArray;
    protected long startTimeNanos;
    private boolean showSingleChart;
    protected final int timeRange = 10;
    JSlider sliderMilliseconds;
    
    public TestClass() {
        this.paused = false;
        this.allColors = new Color[] { Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW };
        this.startTimeNanos = -1L;
        this.showSingleChart = true;
        this.pinEnabled = new boolean[12];
        this.xySeriesArray = new XYSeries[12];
        for (int i = 0; i < 12; ++i) {
            this.pinEnabled[i] = true;
            this.xySeriesArray[i] = new XYSeries(String.valueOf(31 + i));
        }
    }
    
    @Override
    public IOIOLooper createIOIOLooper(final String connectionType, final Object extra) {
        return new BaseIOIOLooper() {
            private AnalogInput[] analogPins;
            private DigitalOutput led;
            
            @Override
            protected void setup() throws ConnectionLostException {
                this.led = this.ioio_.openDigitalOutput(0, true);
                this.analogPins = new AnalogInput[12];
                for (int i = 0; i < 12; ++i) {
                    this.analogPins[i] = this.ioio_.openAnalogInput(31 + i);
                }
            }
            
            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                while (TestClass.this.paused) {
                    final long currentTimeNanos = TestClass.this.getEllapsedTimeNanos();
                    this.led.write(true);
                    Thread.sleep(300L);
                    if (TestClass.this.startTimeNanos != -1L) {
                        final TestClass this$0 = TestClass.this;
                        this$0.startTimeNanos += TestClass.this.getEllapsedTimeNanos() - currentTimeNanos;
                    }
                }
                this.led.write(false);
                if (TestClass.this.startTimeNanos == -1L) {
                    TestClass.this.startTimeNanos = System.nanoTime();
                }
                for (int i = 0; i < 12; ++i) {
                    if (TestClass.this.pinEnabled[i]) {
                        TestClass.this.plotVoltage(i, this.analogPins[i].getVoltage());
                    }
                }
                Thread.sleep(TestClass.this.sliderMilliseconds.getValue());
            }
        };
    }
    
    protected void plotVoltage(final int pinIndex, final float voltage) {
        final double time = this.getEllapsedTime();
        this.xySeriesArray[pinIndex].add(time, voltage);
        XYPlot[] plots;
        for (int length = (plots = this.plots).length, i = 0; i < length; ++i) {
            final XYPlot plot = plots[i];
            if (time >= plot.getDomainAxis().getUpperBound()) {
                this.centerPlotsOnTime(time);
            }
        }
    }
    
    protected void breakPlotLine(final int pinIndex) {
        this.xySeriesArray[pinIndex].add(this.getEllapsedTime(), null);
    }
    
    protected long getEllapsedTimeNanos() {
        return System.nanoTime() - this.startTimeNanos;
    }
    
    protected double getEllapsedTime() {
        return (System.nanoTime() - this.startTimeNanos) / Math.pow(10.0, 9.0);
    }
    
    public void clear() {
        final boolean pausedBefore = this.paused;
        this.paused = true;
        for (int i = 0; i < 12; ++i) {
            this.xySeriesArray[i].clear();
        }
        XYPlot[] plots;
        for (int length = (plots = this.plots).length, j = 0; j < length; ++j) {
            final XYPlot plot = plots[j];
            plot.getDomainAxis().setRange(0.0, 10.0);
        }
        this.startTimeNanos = -1L;
        this.paused = pausedBefore;
    }
    
    public void setPinEnabled(final int index, final boolean enable) {
        if (!(this.pinEnabled[index] = enable)) {
            this.breakPlotLine(index);
        }
    }
    
    @Override
    protected Window createMainWindow(final String[] args) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
        final Color backgroundColor = Color.WHITE;
        final JFrame f = new JFrame("IOIOmeter");
        f.setDefaultCloseOperation(2);
        f.getContentPane().setBackground(backgroundColor);
        f.setSize(800, 500);
        f.setExtendedState(f.getExtendedState() | 0x6);
        f.setVisible(true);
        f.getContentPane().setLayout(new FlowLayout());
        final JCheckBox[] checkBoxes = new JCheckBox[12];
        final JPanel[] colorPanels = new JPanel[12];
        final JCheckBox checkBoxAll = new JCheckBox("All", true);
        checkBoxAll.setBackground(backgroundColor);
        checkBoxAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                final boolean isSelected = ((JCheckBox)event.getSource()).isSelected();
                for (int i = 0; i < 12; ++i) {
                    checkBoxes[i].setSelected(isSelected);
                    TestClass.this.setPinEnabled(i, isSelected);
                }
            }
        });
        f.add(checkBoxAll);
        for (int i = 0; i < 12; ++i) {
            (checkBoxes[i] = new JCheckBox(String.valueOf(31 + i), true)).setBackground(backgroundColor);
            checkBoxes[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final JCheckBox source = (JCheckBox)event.getSource();
                    boolean allSelected = true;
                    for (int i = 0; i < 12; ++i) {
                        if (checkBoxes[i] == source) {
                            TestClass.this.setPinEnabled(i, source.isSelected());
                        }
                        if (!checkBoxes[i].isSelected()) {
                            allSelected = false;
                        }
                    }
                    checkBoxAll.setSelected(allSelected);
                }
            });
            f.add(checkBoxes[i]);
            (colorPanels[i] = new JPanel()).setBackground(this.allColors[i % this.allColors.length]);
            colorPanels[i].addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {
                    final JPanel source = (JPanel)event.getSource();
                    final Color newBackground = JColorChooser.showDialog(f, "Choose line color", source.getBackground());
                    source.setBackground(newBackground);
                    for (int i = 0; i < 12; ++i) {
                        final JPanel panel = colorPanels[i];
                    }
                }
                
                @Override
                public void mouseEntered(final MouseEvent arg0) {
                }
                
                @Override
                public void mouseExited(final MouseEvent arg0) {
                }
                
                @Override
                public void mousePressed(final MouseEvent arg0) {
                }
                
                @Override
                public void mouseReleased(final MouseEvent arg0) {
                }
            });
        }
        f.add(new JLabel("Milliseconds between measurements"));
        (this.sliderMilliseconds = new JSlider(5, 255, 50)).setMajorTickSpacing(50);
        this.sliderMilliseconds.setMinorTickSpacing(10);
        this.sliderMilliseconds.setPaintTicks(true);
        this.sliderMilliseconds.setPaintLabels(true);
        this.sliderMilliseconds.setBackground(backgroundColor);
        f.add(this.sliderMilliseconds);
        final JButton btStartPause = new JButton("Pause");
        btStartPause.setBackground(backgroundColor);
        btStartPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                if (TestClass.this.paused) {
                    TestClass.access$2(TestClass.this, false);
                    ((JButton)event.getSource()).setText("Pause");
                }
                else {
                    TestClass.access$2(TestClass.this, true);
                    ((JButton)event.getSource()).setText("Start");
                }
            }
        });
        f.add(btStartPause);
        final JButton btClear = new JButton("Clear");
        btClear.setBackground(backgroundColor);
        btClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                TestClass.this.clear();
            }
        });
        f.add(btClear);
        final JPanel chartsPanel = new JPanel();
        chartsPanel.setBackground(backgroundColor);
        chartsPanel.add(this.createChartPanelBig());
        final JButton btSwitchView = new JButton("Switch View");
        btSwitchView.setBackground(backgroundColor);
        btSwitchView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                TestClass.access$2(TestClass.this, true);
                chartsPanel.removeAll();
                if (TestClass.this.showSingleChart) {
                    chartsPanel.add(TestClass.this.createChartPanelsSmall());
                }
                else {
                    chartsPanel.add(TestClass.this.createChartPanelBig());
                }
                TestClass.access$6(TestClass.this, !TestClass.this.showSingleChart);
                chartsPanel.revalidate();
                TestClass.access$2(TestClass.this, false);
            }
        });
        f.add(btSwitchView);
        f.add(chartsPanel);
        return f;
    }
   
    private JPanel createChartPanelBig() {
        final XYSeriesCollection collection = new XYSeriesCollection();
        for (int i = 0; i < 12; ++i) {
            collection.addSeries(this.xySeriesArray[i]);
        }
        final JFreeChart chart = this.createDefaultChart(collection);
        (this.plots = new XYPlot[1])[0] = chart.getXYPlot();
        this.plots[0].getRangeAxis().setRange(-0.1, 3.5);
        this.plots[0].getDomainAxis().setRange(0.0, 10.0);
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1300, 630));
        return chartPanel;
    }
    
    private JPanel createChartPanelsSmall() {
        final JPanel chartsPanel = new JPanel();
        chartsPanel.setLayout(new GridLayout(3, 4));
        this.plots = new XYPlot[12];
        for (int i = 0; i < 12; ++i) {
            final XYSeriesCollection collection = new XYSeriesCollection();
            collection.addSeries(this.xySeriesArray[i]);
            final JFreeChart chart = this.createDefaultChart(collection);
            this.plots[i] = chart.getXYPlot();
            this.plots[i].getRangeAxis().setRange(-0.1, 3.5);
            this.plots[i].getDomainAxis().setRange(0.0, 10.0);
            final ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            chartsPanel.add(chartPanel);
        }
        return chartsPanel;
    }
    
    protected JFreeChart createDefaultChart(final XYDataset dataset) {
        final JFreeChart chart = ChartFactory.createXYLineChart("", "Time / s", "Voltage / V", dataset);
        chart.setBackgroundPaint(Color.WHITE);
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlineStroke(new BasicStroke());
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        return chart;
    }
    
    protected void centerPlotsOnTime(final double time) {
        XYPlot[] plots;
        for (int length = (plots = this.plots).length, i = 0; i < length; ++i) {
            final XYPlot plot = plots[i];
            plot.getDomainAxis().centerRange(time);
        }
    }
    
    public static void main(final String[] args) {
        try {
            new TestClass().go(args);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static /* synthetic */ void access$2(final TestClass testClass, final boolean paused) {
        testClass.paused = paused;
    }
    
    static /* synthetic */ void access$6(final TestClass testClass, final boolean showSingleChart) {
        testClass.showSingleChart = showSingleChart;
    }
}
