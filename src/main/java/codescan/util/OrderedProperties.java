package codescan.util;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class OrderedProperties extends Properties {
    private static final long serialVersionUID = -4627607243846121965L;
    private File file;
    private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    public OrderedProperties() {
// TODO Auto-generated constructor stub
    }

    public OrderedProperties(File file) {
// TODO Auto-generated constructor stub
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Enumeration<Object> keys() {
        return Collections.enumeration(keys);
    }
//	    public Object put(Object key, Object value) {
//	        keys.add(key);
//	        return super.put(key, value);
//	    }

    /**
     * 重写put方法，按照property的存入顺序保存key到keyList，遇到重复的后者将覆盖前者。
     */
    @Override
    public synchronized Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

    public Set<Object> keySet() {
        return keys;
    }

    public Set<String> stringPropertyNames() {
        Set<String> set = new LinkedHashSet<String>();
        for (Object key : this.keys) {
            set.add((String) key);
        }
        return set;
    }
}