package codescan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import codescan.util.IOHelper;
import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.UnicodeDetector;
import codescan.util.OrderedProperties;

public class CodeScanMain {
    private boolean isBlockComment = false;//注释块
    //代码扫描起始目录
    private String codeFileRootPath = "";
    //元数据跟目录
    private String resFilePath = "";
    private CodepageDetectorProxy detector;
    private String logFilePath = "";
    private PrintWriter logWriter;
    private File resFile; //资源文件
    private String resProjectName;//资源文件全名称
    //private Map existResMap;//资源文件中已有的词条
    private Map<String, Map<String, String>> existResMap = new HashMap<>();
    //private Map newResMap = new LinkedHashMap();//新词条
    private LinkedHashMap<String, String> newResMap = new LinkedHashMap();//新词条
    private LinkedHashMap<String, String> codeExistResMap = new LinkedHashMap();//代码中已存在6词条
    //java文件集合
    private Set<String> fileNameSet = new LinkedHashSet();
    public boolean hasImport = false;
    boolean hasChinese = false; //是否存在中文硬编码
    /**
     * ResManager包路径
     */
    public static final String PACKAGE_RESMANAGER = "kd.bos.dataentity.resource.ResManager";
    /**
     * SubSystemType 包路径
     */
    public static final String PACKAGE_SUBSYSTEMTYPE = "kd.bos.dataentity.resource.SubSystemType";
    public static final String REPLACESTR = "REPLACE_INTESPLIT_STR";
    public static final String REPLACEMONEY = "REPLACE_STR_MONEY";
    List<Exception> exceptionList = null;
    private static int resCurrIndex = 0;
    private String inputProjectId = "";
    private boolean isEnumClass = false;//注释块

    private void init() throws Exception {
        //日志文件
        logFilePath = logFilePath + "\\CodeScanLog.txt";
        //logWriter = new PrintWriter(new FileOutputStream(new File(logFilePath)));
        logWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logFilePath)), Charset.forName("utf-8")));
        //获取资源文件
        resFile = this.getResourceFile(resFilePath);
        //资源文件全名称
        resProjectName = this.getResourceFullClassName(resFile.getCanonicalPath());
        //获取资源文件已有的中文
        existResMap = getExistResource(resFile);
        //文件编码检测器
        detector = CodepageDetectorProxy.getInstance();
        detector.add(UnicodeDetector.getInstance());
        detector.add(ASCIIDetector.getInstance());
        detector.add(JChardetFacade.getInstance());
    }

    //获取资源文件已有的中文
    private Map<String, Map<String, String>> getExistResource(File resFile) throws Exception {
        if (resFile == null) {
            return Collections.EMPTY_MAP;
        }
        // 获取资源文件
        FileInputStream fis = null;
        Properties p = new Properties();
        fis = new FileInputStream(resFile);
        BufferedReader bf = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        p.load(bf);
        Map<String, Map<String, String>> map = new HashMap<>();
        Enumeration enu = p.keys();
        while (enu.hasMoreElements()) {
            String obj = enu.nextElement().toString();
            String val = obj.substring(0, obj.lastIndexOf("_"));
            String num = obj.substring(obj.lastIndexOf("_") + 1, obj.length());
            ;
            Map<String, String> resmap = map.get(val);
            if (resmap == null) {
                resmap = new HashMap<String, String>();
                map.put(val, resmap);
            }
            String objv = p.get(obj).toString();
            resmap.put(objv, num);
        }
        bf.close();
        return map;
    }

    /**
     * 获取资源文件的全名称
     */
    private static Pattern resFullClassNamePattern = Pattern.compile("(src/main/java/resources[^.]*)", Pattern.CASE_INSENSITIVE);

    private String getResourceFullClassName(String resFilePath) {
        resFilePath = resFilePath.replaceAll("\\\\", "/");
        Matcher matcher = resFullClassNamePattern.matcher(resFilePath);
        if (matcher.find()) {
            String s = matcher.group();
            s = s.substring(s.lastIndexOf("/") + 1, s.indexOf("_zh_CN"));
            return s.replaceAll("/", ".");
        }
        return null;
    }

    /**
     * @param path 获取对应类的系统产生的资源文件，如果不存在则产生个新的
     */
    private File getResourceFile(String path) throws Exception {
        path = path.replaceAll("\\\\", "/");
        if (!path.endsWith("/"))
            path += "/";
        //String regex = "(com/kingdee/eas(/\\w+)*)";
        String regex = "(src/main/java/resources(/\\w+)*)";
        Pattern patt = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(path);
        if (matcher.find()) {
            String g1 = matcher.group(1);
            String pkg = g1.substring(0, g1.length()).replaceAll("/", ".");
            String arr[] = pkg.split("\\.");
            String preName;
            if ("client".equalsIgnoreCase(arr[arr.length - 1]) || "app".equalsIgnoreCase(arr[arr.length - 1])) {
                preName = arr[arr.length - 2] + arr[arr.length - 1];
            } else {
                preName = arr[arr.length - 1];
            }
            String str = path.substring(0, path.lastIndexOf("/src"));
            String name = str.substring(str.lastIndexOf("/") + 1);
            String resName = name + "_zh_CN.properties";
            if (inputProjectId != null && !inputProjectId.equalsIgnoreCase("")) {
                resName = inputProjectId + "_zh_CN.properties";
            } else {
                resName = name + "_zh_CN.properties";
            }
            File file = new File(path + resName);
            if (!file.exists()) {
                file = createNewResFile(path + resName);
            }
            return file;
        } else {
            throw new Exception("请选择正确的资源文件存放目录");
        }
    }

    /**
     * @param resFilePath 创建资源文件
     * @throws Exception
     */
    private File createNewResFile(String resFilePath) throws Exception {
        //创建Properties
        Properties properties = new Properties();
        properties.store(new FileWriter(resFilePath), null);
        return new File(resFilePath);
    }

    /**
     * 开始扫描代码
     *
     * @throws Exception
     */
    public synchronized void start() throws Exception {
        try {
            init();
            File rootFile = new File(codeFileRootPath);
            searchFile(rootFile);
            end();
        } catch (Exception e) {
            if (logWriter != null) {
                e.printStackTrace(logWriter);
            }
            throw e;
        } finally {
            if (logWriter != null) {
                logWriter.flush();
            }
            IOHelper.closeWriter(logWriter);
        }
    }

    public void end() throws Exception {
        //输出资源文件
        if (newResMap != null && newResMap.size() > 0) {
            reorganizaeResMap();
            writeResFile(codeExistResMap, resFile.getPath(), resProjectName);
            //writeResFile(newResMap,resFile.getPath(),resProjectName);
        } else {
            writeResFile(codeExistResMap, resFile.getPath(), resProjectName);
        }
        logWriter.write("替换完成！");
    }

    private void reorganizaeResMap() {
        Iterator it = newResMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            codeExistResMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    /**
     * @param words
     * @param path  写资源文件
     * @throws
     */
    private void writeResFile(Map words, String path, String resProjectName) throws Exception {
        // 获取资源文件
        FileInputStream fis = null;
        OrderedProperties properties = new OrderedProperties();
        fis = new FileInputStream(path);
        BufferedReader bf = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        //properties.load(bf);
        OutputStream out = new FileOutputStream(path);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        for (Iterator it = words.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Entry) it.next();
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            //p.put(key, value);
            properties.put(key, value);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        properties.store(bw, null);  //, "Update Time:'" +df.format(new Date())
        out.close();
    }

    private static FileFilterImpl fileFilter = new FileFilterImpl();

    public void searchFile(File rootFile) throws Exception {
        File[] files = rootFile.listFiles(fileFilter);
        for (int i = 0, j = files.length; i < j; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                searchFile(file);
            } else {
                try {
                    scanFile(file);
                } catch (Exception e) {
                    exceptionList.add(e);
                    if (logWriter != null) {
                        e.printStackTrace(logWriter);
                    }
                }
            }
        }
    }

    /**
     * @param file
     * 扫描.java文件中的中文
     */
    private static Pattern chieseWordPattern = Pattern.compile("(\"[^\"]*[\\u4e00-\\u9fa5]+[^\"]*[^\\\\]?\")");

    public void scanFile(File file) throws Exception {
        if (file == null) {
            return;
        }
        isEnumClass = false;
        //获取资源文件ResourceItem条数
        //int resCurrIndex = getCurrentResItemSize(resFile);
        //resCurrIndex = existResMap.size();
        //java文件名
        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
        if (!existResMap.containsKey(fileName)) {//fileNameSet.contains(fileName)
            resCurrIndex = 0;
        } else {
            Iterator<String> it = existResMap.get(fileName).values().iterator();
            List<Integer> max = new ArrayList<Integer>();
            while (it.hasNext()) {
                String cc = it.next();
                Integer next = Integer.valueOf(cc);
                max.add(next);
            }
            resCurrIndex = Collections.max(max) + 1;
        }
        fileNameSet.add(fileName);
        BufferedReader reader = null;
        StringWriter strWriter = null;
        BufferedWriter bw = null;
        try {
            //reader = new BufferedReader(new FileReader(file));
            Charset cc = detector.detectCodepage(file.toURI().toURL());
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), cc));
            strWriter = new StringWriter(10000);
            String line, trimLine, checkLine = "";
            boolean isFirstMatchLine = true;
            int lineNum = 0;//行号
            hasChinese = false; //是否存在中文硬编码
            hasImport = false;
            StringBuilder errorMsgSb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                lineNum++;
                trimLine = line.trim();
                // \"的先替换为特殊字符,在替换回来,以免正则匹配有问题
                boolean isWrite = writeLine(line, strWriter);
                if (isWrite) continue;
                boolean enumLineCon = line.contains("new MultiLangEnumBridge");
                boolean lineCon = line.contains("ResManager.loadKDString");
                boolean lineCon1 = line.contains("loadKDString");
                boolean checkLineCon = checkLine.contains("ResManager.loadKDString");
                if (isEnumClass && enumLineCon) {
                    //处理枚举类中的中文
                    boolean isRepeat = checkRepeat4Enum(line, strWriter, fileName, fileName);
                    checkLine = "";
                    if (isRepeat) continue;
                }
                if (lineCon || checkLineCon) {
                    String wordData = lineCon ? line.substring(line.indexOf("ResManager.loadKDString"))
                        : checkLine.substring(checkLine.indexOf("ResManager.loadKDString"));
                    wordData = wordData.replaceFirst("ResManager.loadKDString", "");
                    if (wordData.split("\"").length < 5) {
                        if (checkLine == "") {
                            checkLine = line;
                        } else {
                            checkLine = checkLine + "\n" + line;
                        }
                        continue;
                    }
                } else if (lineCon1 || checkLineCon) {
                    String wordData = lineCon1 ? line.substring(line.indexOf("loadKDString"))
                        : checkLine.substring(checkLine.indexOf("loadKDString"));
                    wordData = wordData.replaceFirst("loadKDString", "");
                    if (wordData.split("\"").length < 5) {
                        if (checkLine == "") {
                            checkLine = line;
                        } else {
                            checkLine = checkLine + "\n" + line;
                        }
                        continue;
                    }
                }
                try {
                    if (checkLine.trim() == "") {
                        //重复抽取处理
                        //boolean isRepeat = checkRepeat(checkLine + " " + line, strWriter, fileName, fileName);
                        boolean isRepeat = checkRepeat4Blank(line, strWriter, fileName, fileName);
                        checkLine = "";
                        if (isRepeat) continue;
                    } else {
                        //重复抽取处理
                        boolean isRepeat = checkRepeat(checkLine + "\n" + line, strWriter, fileName, fileName);
                        checkLine = "";
                        if (isRepeat) continue;
                    }
                } catch (Exception ce) {
                    addLog(errorMsgSb, ce.getMessage());
                }
                // 匹配中文字符串
                String key;
                line = line.replaceAll("\\\\\"", REPLACESTR);
                line = line.replace("$", REPLACEMONEY);
                Matcher matcher = chieseWordPattern.matcher(line);
                StringBuffer sb = new StringBuffer();
                String str = "";
                boolean isMatch = false;
                while (matcher.find()) {
                    if (line.indexOf(" static ") > -1) {
                        String errorMsg = fileName + "：不允许定义带中文的静态常量，请根据多语言规范修改" + line;
                        addLog(errorMsgSb, errorMsg);
                    }
                    int idx1 = line.indexOf("//");
                    int idx2 = line.indexOf("/*");
                    if (idx1 >= 0 && (matcher.start() >= idx1 || matcher.end() >= idx1)) {
                        matcher.appendReplacement(sb, matcher.group());
                    } else if (idx2 >= 0 && (matcher.start() >= idx2 || matcher.end() >= idx2)) {
                        matcher.appendReplacement(sb, matcher.group());
                    } else {
                        hasChinese = true;
                        isMatch = true;
                        String s = matcher.group();// 匹配的中文
                        String word = s.substring(1, s.length() - 1);// 去掉引号
                        word.replaceAll("\"", "&quot;");// 引号替换
                        //word = word.replaceAll("\\\\", "\\\\\\\\");//处理"\"
                        if (word.length() >= 1024) {
                            continue;
                        }
                        if (word != null && !word.isEmpty()) {
                            //boolean exist=existResMap.entrySet().stream().anyMatch(e -> e.getKey().equals(word));
                            word = word.replaceAll(REPLACESTR, "\\\\\"");
                            word = word.replace(REPLACEMONEY, "$");
                            if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(word))//
                            {
                                //key = existResMap.entrySet().stream().filter(e -> e.getKey().equals(word)).collect(Collectors.);
                                key = fileName + "_" + existResMap.get(fileName).get(word).toString();
                            } else {
                                key = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                                newResMap.put(key, word);//换回\
                                Map<String, String> resmap = existResMap.get(fileName);
                                if (resmap == null) {
                                    resmap = new HashMap<String, String>();
                                }
                                //Map<String,String> resmap = new HashMap<String,String>();
                                resmap.put(word, key.split("_")[1]);
                                existResMap.put(fileName, resmap);
                                resCurrIndex++;
                            }
                            word = word.replace("$", REPLACEMONEY);
                            matcher.appendReplacement(sb, buildResManager(word.replaceAll("\\\\", "\\\\\\\\"), key));
                        }
                    }
                }
                matcher.appendTail(sb);
                str = sb.toString();
                str = str.replaceAll(REPLACESTR, "\\\\\"");
                str = str.replaceAll(REPLACEMONEY, "\\$");
                str = str.replaceAll("SubSystemType.SL", "\"" + resProjectName + "\"");
                //写日志
                if (isMatch) {
                    if (isFirstMatchLine) {
                        logWriter.println();
                        logWriter.write("-----------" + file.getPath());
                        logWriter.println();
                        isFirstMatchLine = false;
                    }
                    logWriter.write(lineNum + "行:\r\n");
                    logWriter.println(trimLine);
                    logWriter.write("替换为:\r\n");
                    logWriter.println(str);
                    logWriter.println();
                }
                //回写文件
                strWriter.write(str);
                strWriter.write("\n");
            }
            if (errorMsgSb.toString().length() != 0) {
                throw new Exception(errorMsgSb.toString());
            }
            // 如果存在中文硬编码，输出替换后的代码
            if (hasChinese) {
                //reader.close();
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), cc));
                bw.write(strWriter.toString());
                bw.flush();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            strWriter.close();
            IOHelper.closeInputStream(reader);
            IOHelper.closeWriter(strWriter);
            IOHelper.closeWriter(bw);
        }
    }

    /**
     * 添加日志
     *
     * @param errorMsgSb
     * @param errorMsg
     */
    private void addLog(StringBuilder errorMsgSb, String errorMsg) {
        errorMsgSb.append("代码扫描出错：");
        errorMsgSb.append("\r\n");
        errorMsgSb.append(errorMsg);
        errorMsgSb.append("\r\n");
        logWriter.println(errorMsg);
    }

    /**
     * @param line
     * @param strWriter
     * @param fileName
     * @param projectName
     * @return
     */
    private boolean checkRepeat4Enum(String line, StringWriter strWriter, String fileName, String projectName) throws Exception {
        boolean result = false;
        String trimLine = line.trim();
        line = line.replaceAll("SubSystemType.SL", "\"" + resProjectName + "\"");
        String interceptTrimLine = new String(trimLine);
        if (trimLine.indexOf("new MultiLangEnumBridge") >= 0) {
            // 将\"替换掉
            interceptTrimLine = interceptTrimLine.replaceAll("\\\\\"", REPLACESTR);
            // 已抽取的跳过
            while (interceptTrimLine.indexOf("new MultiLangEnumBridge") >= 0) {
                //校验已抽取的词条是否更改
                String wordData = interceptTrimLine.substring(interceptTrimLine.indexOf("new MultiLangEnumBridge"));
                wordData = wordData.replaceFirst("new MultiLangEnumBridge", "");
                String[] wArr = wordData.split("\"");
                if (wArr.length < 4) {
                    String msg = fileName + "：" + trimLine + "\r\n 枚举类多语言内容要在同一行代码中，并保证格式正确。\r\n";
                    logWriter.write(msg);
                    throw new Exception(msg);
                }
                String translation = wArr[1];
                String resId = wArr[3];
                String newResId = "";
                translation = translation.replaceAll(REPLACESTR, "\\\\\"");
                if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(translation)) {
                    newResId = fileName + "_" + existResMap.get(fileName).get(translation).toString();
                } else {
                    newResId = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                    newResMap.put(newResId, translation);
                    //Map<String,String> resmap = new HashMap<String,String>();
                    Map<String, String> resmap = existResMap.get(fileName);
                    if (resmap == null) {
                        resmap = new HashMap<String, String>();
                    }
                    //Map<String,String> resmap = new HashMap<String,String>();
                    resmap.put(translation, newResId.split("_")[1]);
                    //resmap.put(translation, newResId);
                    existResMap.put(fileName, resmap);
                    resCurrIndex++;
                }
                if (!resId.equals(newResId)) {
                    line = line.replace(resId, String.valueOf(newResId));
                    hasChinese = true;
                    codeExistResMap.put(newResId, translation);
                } else {
                    codeExistResMap.put(resId, translation);
                }
                if (wArr.length > 6) {
                    String resProjectId = wArr[5];
                    line = line.replace(resProjectId, resProjectName);
                }
                interceptTrimLine = wordData;
            }
            // 替换回\"
            interceptTrimLine = interceptTrimLine.replaceAll(REPLACESTR, "\\\\\"");
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        return result;
    }

    /**
     * @param line
     * @param strWriter
     * @param fileName
     * @param projectName
     * @return
     */
    private boolean checkRepeat4Blank(String line, StringWriter strWriter, String fileName, String projectName) throws Exception {
        boolean result = false;
        if (line.indexOf(" static ") > -1) {
            // 静态变量都要重新检查
            return result;
        }
        String trimLine = line.trim();
        line = line.replaceAll("SubSystemType.SL", "\"" + resProjectName + "\"");
        String interceptTrimLine = new String(trimLine);
        if (trimLine.indexOf("ResManager.loadKDString") >= 0) {
            // 将\"替换掉
            interceptTrimLine = interceptTrimLine.replaceAll("\\\\\"", REPLACESTR);
            // 已抽取的跳过
            while (interceptTrimLine.indexOf("ResManager.loadKDString") >= 0) {
                //校验已抽取的词条是否更改
                String wordData = interceptTrimLine.substring(interceptTrimLine.indexOf("ResManager.loadKDString"));
                wordData = wordData.replaceFirst("ResManager.loadKDString", "");
                String[] wArr = wordData.split("\"");
                if (wArr.length < 4) {
                    String msg = fileName + "：" + trimLine + "\r\n ResManager.loadKDString()的内容要在同一行代码中，并保证格式正确。\r\n";
                    logWriter.write(msg);
                    throw new Exception(msg);
                }
                String translation = wArr[1];
                String resId = wArr[3];
                String newResId = "";
                translation = translation.replaceAll(REPLACESTR, "\\\\\"");
                if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(translation)) {
                    newResId = fileName + "_" + existResMap.get(fileName).get(translation).toString();
                } else {
                    newResId = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                    newResMap.put(newResId, translation);
                    //Map<String,String> resmap = new HashMap<String,String>();
                    Map<String, String> resmap = existResMap.get(fileName);
                    if (resmap == null) {
                        resmap = new HashMap<String, String>();
                    }
                    //Map<String,String> resmap = new HashMap<String,String>();
                    resmap.put(translation, newResId.split("_")[1]);
                    //resmap.put(translation, newResId);
                    existResMap.put(fileName, resmap);
                    resCurrIndex++;
                }
                if (!resId.equals(newResId)) {
                    line = line.replace(resId, String.valueOf(newResId));
                    hasChinese = true;
                    codeExistResMap.put(newResId, translation);
                } else {
                    codeExistResMap.put(resId, translation);
                }
                if (wArr.length > 6) {
                    String resProjectId = wArr[5];
                    line = line.replace(resProjectId, resProjectName);
                }
                interceptTrimLine = wordData;
            }
            // 替换回\"
            interceptTrimLine = interceptTrimLine.replaceAll(REPLACESTR, "\\\\\"");
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        } else if (trimLine.indexOf("loadKDString") >= 0) {
            // 将\"替换掉
            interceptTrimLine = interceptTrimLine.replaceAll("\\\\\"", REPLACESTR);
            // 已抽取的跳过
            while (interceptTrimLine.indexOf("loadKDString") >= 0) {
                //校验已抽取的词条是否更改
                String wordData = interceptTrimLine.substring(interceptTrimLine.indexOf("loadKDString"));
                wordData = wordData.replaceFirst("loadKDString", "");
                String[] wArr = wordData.split("\"");
                if (wArr.length < 4) {
                    String msg = fileName + "：" + trimLine + "\r\n ResManager.loadKDString()的内容要在同一行代码中，并保证格式正确。\r\n";
                    logWriter.write(msg);
                    throw new Exception(msg);
                }
                String translation = wArr[1];
                String resId = wArr[3];
                String newResId = "";
                translation = translation.replaceAll(REPLACESTR, "\\\\\"");
                if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(translation)) {
                    newResId = fileName + "_" + existResMap.get(fileName).get(translation).toString();
                } else {
                    newResId = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                    newResMap.put(newResId, translation);
                    //Map<String,String> resmap = new HashMap<String,String>();
                    Map<String, String> resmap = existResMap.get(fileName);
                    if (resmap == null) {
                        resmap = new HashMap<String, String>();
                    }
                    //Map<String,String> resmap = new HashMap<String,String>();
                    resmap.put(translation, newResId.split("_")[1]);
                    //resmap.put(translation, newResId);
                    existResMap.put(fileName, resmap);
                    resCurrIndex++;
                }
                if (!resId.equals(newResId)) {
                    line = line.replace(resId, String.valueOf(newResId));
                    hasChinese = true;
                    codeExistResMap.put(newResId, translation);
                } else {
                    codeExistResMap.put(resId, translation);
                }
                if (wArr.length > 6) {
                    String resProjectId = wArr[5];
                    line = line.replace(resProjectId, resProjectName);
                }
                interceptTrimLine = wordData;
            }
            // 替换回\"
            interceptTrimLine = interceptTrimLine.replaceAll(REPLACESTR, "\\\\\"");
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        return result;
    }

    /**
     * @param line
     * @param strWriter
     * @param fileName
     * @param projectName
     * @return
     */
    private boolean checkRepeat(String line, StringWriter strWriter, String fileName, String projectName) throws Exception {
        boolean result = false;
        if (line.indexOf(" static ") > -1) {
            // 静态变量都要重新检查
            return result;
        }
        String trimLine = line.trim();
        line = line.replaceAll("SubSystemType.SL", "\"" + resProjectName + "\"");
        String interceptTrimLine = new String(trimLine);
        if (trimLine.indexOf("ResManager.loadKDString") >= 0) {
            // 将\"替换掉
            interceptTrimLine = interceptTrimLine.replaceAll("\\\\\"", REPLACESTR);
            // 已抽取的跳过
            while (interceptTrimLine.indexOf("ResManager.loadKDString") >= 0) {
                //校验已抽取的词条是否更改
                String wordData = interceptTrimLine.substring(interceptTrimLine.indexOf("ResManager.loadKDString"));
                wordData = wordData.replaceFirst("ResManager.loadKDString", "");
                String[] wArr = wordData.split("\"");
                if (wArr.length < 4) {
                    String msg = fileName + "：" + trimLine + "\r\n ResManager.loadKDString()的内容要在同一行代码中，并保证格式正确。\r\n";
                    logWriter.write(msg);
                    throw new Exception(msg);
                }
                String translation = wArr[1];
                String resId = wArr[3];
                String newResId = "";
                translation = translation.replaceAll(REPLACESTR, "\\\\\"");
                if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(translation)) {
                    newResId = fileName + "_" + existResMap.get(fileName).get(translation).toString();
                } else {
                    newResId = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                    newResMap.put(newResId, translation);
                    //Map<String,String> resmap = new HashMap<String,String>();
                    Map<String, String> resmap = existResMap.get(fileName);
                    if (resmap == null) {
                        resmap = new HashMap<String, String>();
                    }
                    //Map<String,String> resmap = new HashMap<String,String>();
                    resmap.put(translation, newResId.split("_")[1]);
                    //resmap.put(translation, newResId);
                    existResMap.put(fileName, resmap);
                    resCurrIndex++;
                }
                if (!resId.equals(newResId)) {
                    line = line.replace(resId, String.valueOf(newResId));
                    hasChinese = true;
                    codeExistResMap.put(newResId, translation);
                } else {
                    codeExistResMap.put(resId, translation);
                }
                if (wArr.length > 6) {
                    String resProjectId = wArr[5];
                    line = line.replace(resProjectId, resProjectName);
                }
                interceptTrimLine = wordData;
            }
            // 替换回\"
            interceptTrimLine = interceptTrimLine.replaceAll(REPLACESTR, "\\\\\"");
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        } else if (trimLine.indexOf("loadKDString") >= 0) {
            // 将\"替换掉
            interceptTrimLine = interceptTrimLine.replaceAll("\\\\\"", REPLACESTR);
            // 已抽取的跳过
            while (interceptTrimLine.indexOf("loadKDString") >= 0) {
                //校验已抽取的词条是否更改
                String wordData = interceptTrimLine.substring(interceptTrimLine.indexOf("loadKDString"));
                wordData = wordData.replaceFirst("loadKDString", "");
                String[] wArr = wordData.split("\"");
                if (wArr.length < 4) {
                    String msg = fileName + "：" + trimLine + "\r\n ResManager.loadKDString()的内容要在同一行代码中，并保证格式正确。\r\n";
                    logWriter.write(msg);
                    throw new Exception(msg);
                }
                String translation = wArr[1];
                String resId = wArr[3];
                String newResId = "";
                translation = translation.replaceAll(REPLACESTR, "\\\\\"");
                if (existResMap.containsKey(fileName) && existResMap.get(fileName).containsKey(translation)) {
                    newResId = fileName + "_" + existResMap.get(fileName).get(translation).toString();
                } else {
                    newResId = createResKey(((existResMap.get(fileName) == null) ? new ArrayList() : existResMap.get(fileName).values()), fileName + "_", resCurrIndex);
                    newResMap.put(newResId, translation);
                    //Map<String,String> resmap = new HashMap<String,String>();
                    Map<String, String> resmap = existResMap.get(fileName);
                    if (resmap == null) {
                        resmap = new HashMap<String, String>();
                    }
                    //Map<String,String> resmap = new HashMap<String,String>();
                    resmap.put(translation, newResId.split("_")[1]);
                    //resmap.put(translation, newResId);
                    existResMap.put(fileName, resmap);
                    resCurrIndex++;
                }
                if (!resId.equals(newResId)) {
                    line = line.replace(resId, String.valueOf(newResId));
                    hasChinese = true;
                    codeExistResMap.put(newResId, translation);
                } else {
                    codeExistResMap.put(resId, translation);
                }
                if (wArr.length > 6) {
                    String resProjectId = wArr[5];
                    line = line.replace(resProjectId, resProjectName);
                }
                interceptTrimLine = wordData;
            }
            // 替换回\"
            interceptTrimLine = interceptTrimLine.replaceAll(REPLACESTR, "\\\\\"");
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        return result;
    }

    /**
     * @param existResKey
     * @param index
     * @param suffix      获取资源文件key
     */
    public String createResKey(Collection existResKey, String suffix, int index) {
        String key = suffix + index;
        Collection existResKeyColl = new ArrayList();
        Iterator<String> it = existResKey.iterator();
        while (it.hasNext()) {
            String next = it.next();
            existResKeyColl.add(suffix + next);
        }
        if (existResKeyColl.contains(key)) {
            createResKey(existResKey, suffix, index++);
        }
        return key;
    }

    /**
     * 拼装替换字符串  示例： ResManager.loadKDString("构造动态实体失败，构造参数：实体类型dt不能为空!", "014009000001644", SubSystemType.SL);
     */
    private String buildResManager(String resVal, Object resId) {
        resVal = resVal.replaceAll(REPLACESTR, "\\\\\\\\\"");
        StringBuffer sb = new StringBuffer();
        sb.append("ResManager.loadKDString(");
        sb.append("\"");
        sb.append(resVal);
        sb.append("\", \"");
        sb.append(resId);
        sb.append("\", SubSystemType.SL)");
        return sb.toString();
    }

    /**
     * 是否直接写入行
     */
    private boolean writeLine(String line, StringWriter strWriter) {
        boolean result = false;
        String trimLine = line.trim();
        //  /*untrans*/不需要多语言处理
        if (trimLine.endsWith("/*untrans*/")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        if (trimLine.trim().length() <= 0 && !isBlockComment) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // 块注释中
        if ((isBlockComment && trimLine.indexOf("*/") < 0) || trimLine.startsWith("//") || trimLine.startsWith("case ")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // 块注释结束
        if (isBlockComment && trimLine.indexOf("*/") >= 0) {
            strWriter.write(line);
            strWriter.write("\n");
            isBlockComment = false;
            result = true;
        }
        // package
        if (trimLine.startsWith("package")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
            if (!hasImport) {
                strWriter.write("import " + PACKAGE_RESMANAGER + ";\n");
                //strWriter.write("import " + PACKAGE_SUBSYSTEMTYPE + ";\n");
                hasImport = true;
            } else {
                if (trimLine.indexOf(PACKAGE_RESMANAGER) >= 0) {
                    result = true;
                }
            }
        }
        // import
        if (trimLine.startsWith("import ") && !trimLine.startsWith("importlogs.add")) {
/*if(!hasImport){
strWriter.write("import " + PACKAGE_RESMANAGER + ";\n");
strWriter.write("import " + PACKAGE_SUBSYSTEMTYPE + ";\n");
hasImport = true;
}else{
if(trimLine.indexOf(PACKAGE_SUBSYSTEMTYPE) >= 0 || trimLine.indexOf(PACKAGE_RESMANAGER) >= 0){
result = true;
}
}*/
            if (trimLine.indexOf(PACKAGE_SUBSYSTEMTYPE) >= 0 || trimLine.indexOf(PACKAGE_RESMANAGER) >= 0) {
            } else {
                strWriter.write(line);
                strWriter.write("\n");
            }
            result = true;
        }
        // 日志处处信息屏蔽
        if (trimLine.startsWith("importlogs.add")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // 拼装的提示语暂不处理
        //		if(trimLine.indexOf("\\\"") >= 0){
        //			strWriter.write(line);
        //			strWriter.write("\n");
        //			result = true;
        //		}
        // 拼装的提示语暂不处理
        if (trimLine.indexOf("@DisplayName") >= 0) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // @KDTripField所在一行的中文不处理
        if (trimLine.startsWith("@KDTripField")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // System.out.println
        if (trimLine.startsWith("System.out.println")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        // 日志处处信息屏蔽
        if (trimLine.startsWith("logger.debug") || trimLine.startsWith("logger.info")
            || trimLine.startsWith("logger.warn") || trimLine.startsWith("logger.error") || trimLine.startsWith("log.debug") || trimLine.startsWith("log.info")
            || trimLine.startsWith("log.warn") || trimLine.startsWith("log.error") || trimLine.startsWith("this.logger.error")) {
            if (!isBlockComment) {
                strWriter.write(line);
                strWriter.write("\n");
                result = true;
            }
        }
        // 日志处处信息屏蔽
        //		if (trimLine.startsWith("log.debug") || trimLine.startsWith("log.info")
        //				|| trimLine.startsWith("log.warn") || trimLine.startsWith("log.error")|| trimLine.startsWith("this.logger.error")) {
        //			strWriter.write(line);
        //			strWriter.write("\n");
        //			result = true;
        //		}
        // 行注释//
        //		if (trimLine.startsWith("//")) {
        //			strWriter.write(line);
        //			strWriter.write("\n");
        //			result = true;
        //		}
        // 行注释/* */
        if (trimLine.startsWith("/*") && trimLine.indexOf("*/") > 0) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        } else if (trimLine.startsWith("/*") && trimLine.indexOf("*/") < 0) {
            // 块注释
            strWriter.write(line);
            strWriter.write("\n");
            isBlockComment = true;
            result = true;
        } else if (trimLine.startsWith("switch ")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        //		else if (trimLine.startsWith("case ")){
        //			//case前面非英文才是java保留词"case"(反例:String switchcase = ...)
        //			// TODO 先修复case后面的汉字抽取出现的问题,后续需考虑如下代码进行抽取时的判断逻辑
        //			/*switch ("") {case "":
        //				String qetqrcase = "";
        //				break;
        //			default:
        //				break;
        //			}*/
        //			strWriter.write(line);
        //			strWriter.write("\n");
        //			result = true;
        //		}
        int idx1 = line.indexOf("//");
        int idx2 = line.indexOf("/*");
        // "/*"不在行首的情况
        if (idx2 < idx1 && idx2 > 0 && trimLine.indexOf("*/") < 0) {
            isBlockComment = true;
        }
        if (trimLine.contains("public enum")) {
            strWriter.write(line);
            strWriter.write("\n");
            isEnumClass = true;
            result = true;
        }
        if (isEnumClass && trimLine.contains("loadKDString()")) {
            strWriter.write(line);
            strWriter.write("\n");
            isEnumClass = true;
            result = true;
        }
        if (trimLine.contains("return ResManager.loadKDString") && !trimLine.contains("\"")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        if (trimLine.contains("String loadKDString()") && !trimLine.contains("\"")) {
            strWriter.write(line);
            strWriter.write("\n");
            result = true;
        }
        return result;
    }

    /**
     * 只处理.java文件,Abstract打头抽象类不做考虑
     * Info,Factory,Enum,Exception,Collection,Facade类不检索
     * 只处理com.kingdee.eas.?.?.包下面的文件
     */
    //private static Pattern fileNamePattern = Pattern.compile("^(Abstract\\w+\\.java|\\w+(Info|Factory|Enum|Exception|Collection|Facade)\\.java)$");
    //    private static Pattern filePathPattern = Pattern.compile("(com/kingdee/eas/(\\w+)/(\\w+)/)",Pattern.CASE_INSENSITIVE);
    //private static Pattern filePathPattern = Pattern.compile("(com/kingdee/(\\w+)/(\\w+)/)",Pattern.CASE_INSENSITIVE);
    private static class FileFilterImpl implements FileFilter {
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            String filePath = file.getPath();
            //			Matcher filePathM =filePathPattern.matcher(filePath.replaceAll("\\\\", "/"));
            //			if(!filePathM.find())
            //			{
            //				return false;
            //			}
            String name = filePath.substring(filePath.lastIndexOf("\\") + 1);
            int index = name.lastIndexOf(".");
            if (index < 0) {
                return false;
            }
            String suffix = name.substring(index);
            if (!".java".equals(suffix)) {
                return false;
            }
            //			Matcher m = fileNamePattern.matcher(name);
            //			if(m.find())
            //			{
            //				return false;
            //			}
            return true;
        }
    }

    public String getCodeFileRootPath() {
        return codeFileRootPath;
    }

    public void setCodeFileRootPath(String codeFileRootPath) {
        this.codeFileRootPath = codeFileRootPath;
    }

    public String getMetaDataRootPath() {
        return resFilePath;
    }

    public void setMetaDataRootPath(String metaDataRootPath) {
        this.resFilePath = metaDataRootPath;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public void setExceptionList(List<Exception> exceptionList) {
        this.exceptionList = exceptionList;
    }

    public void setInputProjectId(String inputProjectId) {
        this.inputProjectId = inputProjectId;
    }
}