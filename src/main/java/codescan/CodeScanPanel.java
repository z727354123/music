package codescan;

import codescan.util.IOHelper;
import codescan.util.ProgressBarUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;

public class CodeScanPanel extends JPanel {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public static final String DEFAULT_SRC_PATH = "/src/main/java";
    public static final String FILE_PATH_SPLIT = ";";

    private JLabel codePathLb, logPathLb, srcPathLb,projectLb;
    private JTextField codePathTf, logPathTf, srcPathTf,projectTf;
    private JButton codePathBtn, logPathBtn, btnOk, btnCancel;
    private JSeparator separator;
    private JScrollPane logTextAreaSp;
    private JTextPane logTextPane;
    private Window parentWindow;
    private String currentAppPath, codePath, srcPath, logPath;

    public CodeScanPanel(Window parentWindow, String currentAppPath, String codePath, String srcPath, String logPath) throws Exception {
        this.parentWindow = parentWindow;
        this.currentAppPath = currentAppPath;
        this.codePath = codePath;
        this.srcPath = srcPath;
        this.logPath = logPath;
        initUI();
    }

    public void initUI() throws Exception {
        this.setLayout(null);

        int y = 20;
        int lableWidth = 120;
        int selectBtWidth = 100;
        int textWidth = WIDTH - lableWidth - selectBtWidth - 20;
        int selectBtX = selectBtWidth + textWidth + 20;
        codePathLb = new JLabel("工程目录");
        codePathLb.setBounds(10, y, 80, 20);
        this.add(codePathLb);

        JLabel helpLb = new JLabel("ⓘ");
        helpLb.setForeground(Color.BLUE);
        helpLb.setBounds(80, y, 20, 20);
        helpLb.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                StringBuilder msg = new StringBuilder();
                msg.append("支持选择工作空间、工程、代码目录");
                msg.append("\r\n");
                msg.append("工作空间：扫描工作空间下所有工程");
                msg.append("\r\n");
                msg.append("工程目录：仅扫描选中工程");
                msg.append("\r\n");
                msg.append("代码目录：扫描代码所在的工程");
                msg.append("\r\n");
                JOptionPane.showMessageDialog(e.getComponent(), msg.toString(), "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        this.add(helpLb);

        codePathTf = new JTextField();
        codePathTf.setText(codePath);
        codePathTf.setBounds(lableWidth, y, textWidth, 22);
        codePathTf.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                projectTf.setText("");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                projectTf.setText("");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                projectTf.setText("");
            }
        });

        this.add(codePathTf);

        codePathBtn = new JButton("选择目录");
        codePathBtn.setActionCommand("code");
        codePathBtn.addActionListener(new ActionListenerImpl());
        codePathBtn.setBounds(selectBtX, y, selectBtWidth, 20);
        this.add(codePathBtn);

        y += 50;
        srcPathLb = new JLabel("代码相对路径");
        srcPathLb.setBounds(10, y, 100, 20);
        this.add(srcPathLb);

        srcPathTf = new JTextField("src");

        if (srcPath == null || srcPath.trim().length() == 0) {
            srcPath = DEFAULT_SRC_PATH;
        }
        srcPathTf.setText(srcPath);
        srcPathTf.setBounds(lableWidth, y, textWidth, 22);
        this.add(srcPathTf);

        //--
        y += 50;
        logPathLb = new JLabel("日志输出目录");
        logPathLb.setBounds(10, y, 100, 20);
        this.add(logPathLb);

        logPathTf = new JTextField();

        if (logPath == null || logPath.trim().length() == 0) {
            logPath = currentAppPath;
        }
        logPathTf.setText(logPath);
        logPathTf.setBounds(lableWidth, y, textWidth, 22);
        this.add(logPathTf);

        logPathBtn = new JButton("选择目录");
        logPathBtn.setActionCommand("log");
        logPathBtn.addActionListener(new

                ActionListenerImpl());
        logPathBtn.setBounds(selectBtX, y, selectBtWidth, 20);
        this.add(logPathBtn);

        //add by wangn
        y += 50;
        projectLb = new JLabel("工程编码");
        projectLb.setBounds(10, y, 100, 20);
        this.add(projectLb);

        projectTf = new JTextField("project");
        projectTf.setText("");
        projectTf.setBounds(lableWidth, y, textWidth, 22);
        this.add(projectTf);
        //add end

        //--
        y += 40;
        int btX = WIDTH / 2 - 120;
        btnOk = new JButton("确定");
        btnOk.setActionCommand("ok");
        btnOk.addActionListener(new ActionScan());
        btnOk.setBounds(btX, y, selectBtWidth, 20);
        this.add(btnOk);

        btnCancel = new JButton("取消");
        btnCancel.setActionCommand("cancel");
        btnCancel.addActionListener(new ActionScan());
        btnCancel.setBounds(btX + 120, y, selectBtWidth, 20);
        this.add(btnCancel);

        y += 40;
        separator = new JSeparator();

        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setBounds(0, y, WIDTH, 3);
        //separator.setBorder(new LineBorder(Color.black));
        this.add(separator, null);

        y += 20;
        JLabel logOutLb = new JLabel("日志");
        logOutLb.setBounds(10, y, 100, 20);
        this.add(logOutLb);

        JButton btnLog = new JButton("查看日志");
        btnLog.setActionCommand("log");
        btnLog.addActionListener(new ActionScan());
        btnLog.setBounds(WIDTH - selectBtWidth - 20, y, selectBtWidth, 20);
        this.add(btnLog);

        y += 20;
        int lw = WIDTH - 30;
        int lh = 300;
        logTextPane = new JTextPane();
        logTextPane.setBounds(10, y, lw, lh);
        logTextAreaSp = new JScrollPane(logTextPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        logTextAreaSp.setBounds(10, y, lw, lh);
        this.add(logTextAreaSp);

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

    private class ActionScan implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String comm = e.getActionCommand();
            if ("ok".equals(comm)) {
                actionOkPerformed();
            } else if ("cancel".equals(comm)) {
                parentWindow.dispose();
            } else {
                openFile(logPathTf.getText() + "\\CodeScanLog.txt");
            }
        }
    }

    private void openFile(String filePath) {
        try {
            Desktop.getDesktop().open(new File(filePath));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "打开文件[" + filePath + "]发生异常：" + ex.getMessage(), "提示", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 确定
     */
    public void actionOkPerformed() {
        logTextPane.setText("");
        List<String> codePathList = new ArrayList<>();
        checkInput(codePathList);
        if (codePathList.isEmpty()) {
            return;
        }

        Set<String> folderList = new HashSet<>();
        for (String projectPath : codePathList) {
            findAllCodePath(projectPath, folderList);
        }

        if (folderList.isEmpty()) {
            addLog("当前选择的" + codePathLb.getText() + "下不存在" + srcPathLb.getText(), Color.BLACK, false);
            return;
        }

        saveProperties();

        actionOkPerformed(folderList);
    }

    private void findAllCodePath(String projectPath, Set<String> folderList) {
        projectPath = projectPath.replaceAll("\\\\", "/");
        if (!projectPath.endsWith("/")) {
            projectPath = projectPath + "/";
        }
        String srcPath = this.srcPathTf.getText().trim();
        srcPath = srcPath.replaceAll("\\\\", "/");
        if (!srcPath.endsWith("/")) {
            srcPath = srcPath + "/";
        }
        srcPath = srcPath.toLowerCase();
        int srcIndex = projectPath.indexOf(srcPath);
        if (srcIndex > -1) {
            // 类路径时
            folderList.add(projectPath.substring(0, srcIndex + srcPath.length()));
        } else {
            findAllCodePath(projectPath, srcPath, folderList);
        }
    }

    private void actionOkPerformed(Set<String> folderList) {
        addLog("开始扫描代码", Color.BLACK, false);
        addLog("当前应用目录：" + currentAppPath, Color.BLACK, false);
        for (String path : folderList) {
            //若代码根目录正常，就创建资源文件输出目录src/main/java/resources
            String resPath = path.substring(0, path.lastIndexOf(srcPath) + srcPath.length()) + "\\resources";
            File resfile = new File(resPath);
            if (!resfile.exists()) {
                resfile.mkdirs();
            }
            addLog("当前扫描目录：" + path, Color.BLACK, false);
            final CodeScanMain codeScanMain = new CodeScanMain();
            List<Exception> exceptionList = new ArrayList<>(4);
            codeScanMain.setExceptionList(exceptionList);
            codeScanMain.setCodeFileRootPath(path);
            codeScanMain.setMetaDataRootPath(path.substring(0, path.lastIndexOf(srcPathTf.getText()) + srcPathTf.getText().length()) + "\\resources");
            codeScanMain.setLogFilePath(logPathTf.getText().trim());
            if(folderList.size() == 1){
                codeScanMain.setInputProjectId(projectTf.getText().trim());
            }
            try {
                Runnable run = new Runnable() {
                    public void run() {
                        try {
                            codeScanMain.start();
                            if(exceptionList.size()>0){
                                for(Exception e : exceptionList){
                                    addLog(e.getMessage(), Color.RED, true);
                                }
                            }
                        } catch (Exception e) {
                            PrintStream stream = null;
                            try {
                                addLog(e.getMessage(), Color.RED, true);
                                String path = getClass().getResource("/").getPath();
                                stream = new PrintStream(new FileOutputStream(path + "error.log"));
                                e.printStackTrace(stream);
                            } catch (IOException ioe) {
                            } finally {
                                IOHelper.closeOutputStream(stream);
                                //System.exit(1);
                            }
                        }
                    }
                };
                Thread t = new Thread(run);
                ProgressBarUtil.show(parentWindow, t, "正在扫描代码,请等待......", "", "Cancel");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        addLog("完成扫描代码", Color.BLACK, false);
        logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
        JOptionPane.showMessageDialog(this, "代码扫描已完成", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addLog(String str, Color col, boolean bold) {
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attrSet, col);
        if (bold == true) {
            StyleConstants.setBold(attrSet, true);
        }
        Document doc = logTextPane.getDocument();
        str = "\r\n" + str;
        try {
            doc.insertString(doc.getLength(), str, attrSet);
        } catch (BadLocationException e) {
        }
    }

    private void findAllCodePath(String projectPath, String srcPath, Set<String> filePathList) {
        projectPath = projectPath.replaceAll("\\\\", "/");
        if (!projectPath.endsWith("/")) {
            projectPath = projectPath + "/";
        }
        if (projectPath.toLowerCase().endsWith(srcPath)) {
            filePathList.add(projectPath.trim());
            return;
        }

        File[] files = new File(projectPath).listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findAllCodePath(file.getAbsolutePath(), srcPath, filePathList);
            }
        }
    }

    private void checkInput(List<String> codePathList) throws RuntimeException {
        String path = this.codePathTf.getText();
        if (path == null || path.trim().length() == 0) {
            String msg = "请选择" + codePathLb.getText();
            JOptionPane.showMessageDialog(CodeScanPanel.this, msg, "", JOptionPane.ERROR_MESSAGE);
            codePathTf.requestFocus();
            throw new RuntimeException(msg);
        } else {

            String[] selectedPaths = path.split(FILE_PATH_SPLIT);
            for (String projectPath : selectedPaths) {
                projectPath = projectPath.replaceAll("\\\\", "/");
                File file = new File(projectPath);
                if (file.isDirectory()) {
                    codePathList.add(projectPath);
                } else if (file.isFile()) {
                    codePathList.add(file.getParent());
                } else {
                    String msg = projectPath + "不存在，请选择正确的目录";
                    addLog(msg, Color.RED, true);
                }
            }

        }

        path = this.srcPathTf.getText();
        if (path == null || path.trim().length() == 0) {
            String msg = "请输入" + srcPathLb.getText();
            JOptionPane.showMessageDialog(CodeScanPanel.this, msg, "", JOptionPane.ERROR_MESSAGE);
            codePathTf.requestFocus();
            throw new RuntimeException(msg);
        }

        path = this.logPathTf.getText();
        if (path == null || path.trim().length() <= 0) {
            String msg = "请选择" + codePathLb.getText();
            JOptionPane.showMessageDialog(CodeScanPanel.this, msg, "", JOptionPane.ERROR_MESSAGE);
            codePathTf.requestFocus();
            throw new RuntimeException(msg);
        } else {
            File file = new File(path.trim());
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    private void saveProperties() {
        Properties prop = new Properties();
        prop.put("codePath", codePathTf.getText());
        prop.put("srcPath", srcPathTf.getText());
        prop.put("logPath", logPathTf.getText());

        String proPath = currentAppPath + "\\codeScanProp.properties";
        File pFile = new File(proPath);
        if (pFile.exists()) {
            pFile.mkdirs();
        }
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(proPath);
            prop.store(stream, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ActionListenerImpl implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            String command = e.getActionCommand();
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            if ("code".equals(command)) {
                fc.setMultiSelectionEnabled(true);
                String codePath = codePathTf.getText();
                if (codePath != null) {
                    String[] codePathArr = codePath.split(FILE_PATH_SPLIT);
                    String defaultPath = codePath;
                    if (codePathArr != null && codePathArr.length > 0) {
                        defaultPath = codePathArr[0];
                    }
                    fc.setCurrentDirectory(new File(defaultPath));
                }
            } else if ("log".equals(command)) {
                fc.setCurrentDirectory(new File(logPathTf.getText()));
            }

            fc.setFileFilter(new ChooseFileImpl());
            int returnVal = fc.showOpenDialog(CodeScanPanel.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fc.getSelectedFiles();
                if (selectedFiles == null || selectedFiles.length == 0) {
                    return;
                }

                StringBuilder filePaths = new StringBuilder();
                boolean isFirst = true;
                for (File file : selectedFiles) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        filePaths.append(FILE_PATH_SPLIT);
                    }
                    filePaths.append(file.getPath().trim());
                }
                if ("code".equals(command)) {
                    codePathTf.setText(filePaths.toString());
                } else if ("log".equals(command)) {
                    logPathTf.setText(filePaths.toString());
                }
            } else {

            }
        }
    }

    private static class ChooseFileImpl extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}