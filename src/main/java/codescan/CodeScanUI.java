package codescan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Properties;

public class CodeScanUI extends JFrame {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private String currentAppPath = "";

    public static void main(String args[]) {
        try {
            CodeScanUI codeScanUI3 = new CodeScanUI();
            codeScanUI3.initUI();
            codeScanUI3.showUI();
        } catch (Exception e) {
            PrintStream stream;
            try {
                String path = CodeScanUI.class.getResource("/").getPath();
                stream = new PrintStream(new FileOutputStream(path + "error.log", true));
                e.printStackTrace(stream);
            } catch (Exception ex) {

            }
        }
    }

    public void showUI() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(WIDTH, HEIGHT);
        this.setResizable(false);
        this.setTitle("中文硬编码检索替换工具");

        Dimension a = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension b = this.getSize();
        this.setLocation((a.width - b.width) / 2, (a.height - b.height) / 2);

        this.setVisible(true);
    }

    private JPanel panel;

    public void initUI() throws Exception {
        currentAppPath = getAppPath();
        Properties properties = getProperties();

        panel = new CodeScanPanel(this, currentAppPath, properties.getProperty("codePath"), properties.getProperty(
                "srcPath"), properties.getProperty("logPath"));
        this.getContentPane().add(panel);

        //创建并添加菜单栏
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        JMenu jm = new JMenu();
        JMenuItem mt = new JMenuItem("帮助(Help)");
        mt.setForeground(Color.BLUE);
        mt.setPreferredSize(new Dimension(100, 20));
        mt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelpDialog((Component) e.getSource());
            }
        });

        jm.add(mt);
        menuBar.add(mt);
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        // 使用ClassLoader加载properties配置文件生成对应的输入流
        InputStream in = null;
        try {
            in = new FileInputStream(new File(currentAppPath + "\\codeScanProp.properties"));
            // 使用properties对象加载输入流
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //获取key对应的value值
        return properties;
    }

    private static String getAppPath() {
        URL url = CodeScanPanel.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (filePath.endsWith(".jar")) {
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }
        File file = new File(filePath);
        filePath = file.getAbsolutePath();
        return filePath;
    }

    /**
     * 显示帮助对话框
     *
     * @param pc
     */
    private void showHelpDialog(Component pc) {
        JOptionPane pane = new JOptionPane();
        pane.setAutoscrolls(true);
        JDialog dialog = pane.createDialog(pc, "帮助");
        dialog.setSize(WIDTH, HEIGHT);
        dialog.setLocation(pc.getX() + WIDTH, pc.getY() + 300);
        int lw = WIDTH - 40;
        int lh = HEIGHT - 40;

        JTextArea mta = new JTextArea();
        mta.setEditable(false);
        mta.setBounds(20, 10, lw, lh);
        mta.setLineWrap(true);

        JScrollPane msp = new JScrollPane(mta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        msp.setBounds(20, 10, lw, lh);
        msp.setVisible(true);
        dialog.setContentPane(msp);

        Charset cc = Charset.forName("UTF-8");
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("help.txt");
        if (resourceAsStream == null) {
            JOptionPane.showMessageDialog(this, "找不到帮助文档", "提示", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream, cc));
        String line = "";

        while (true) {
            try {
                if ((line = in.readLine()) == null) {
                    break;
                }
                mta.append(line);
                mta.append("\r\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        mta.setCaretPosition(0);
        dialog.show();
    }
}