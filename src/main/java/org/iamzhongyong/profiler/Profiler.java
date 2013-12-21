package org.iamzhongyong.profiler;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 用来测试并统计线程执行时间的工具。
 *
 * @author Michael Zhou   webx开源框架的作者
 * @version $Id: Profiler.java 1291 2005-03-04 03:23:30Z baobao $
 * 
 */
public final class Profiler {
	/**构建实体的存储缓存体*/
    private static final ThreadLocal<Entry> entryStack = new ThreadLocal<Entry>();
    
    /**开始计时*/
    public static void start() {
        start((String) null);
    }

    /**开始计时，创建一个Entry的实体对象*/
    public static void start(String message) {
        entryStack.set(new Entry(message, null, null));
    }

    /**threadLocal缓存清理，由于现在大多是线程池的设置，所以要做一个清理*/
    public static void reset() {
        entryStack.set(null);
    }

    /**由于Entry自身是树状结构，所以如果是进入非Root的节点，那就需要enter来搞*/
    public static void enter(String message) {
        Entry currentEntry = getCurrentEntry();
        if (currentEntry != null) {
            currentEntry.enterSubEntry(message);
        }
    }

    /**方法运行结束之后，把当前的Entry的endTime来设置成当前时间*/
    public static void release() {
        Entry currentEntry = getCurrentEntry();
        if (currentEntry != null) {
            currentEntry.release();
        }
    }

    /**获取start和end的时间差*/
    public static long getDuration() {
        Entry entry = (Entry) entryStack.get();
        if (entry != null) {
            return entry.getDuration();
        } else {
            return -1;
        }
    }

    /**把Entry的信息dump出来，可以打印到日志中去*/
    public static String dump() {
        return dump("", "");
    }

    public static String dump(String prefix1, String prefix2) {
        Entry entry = (Entry) entryStack.get();
        if (entry != null) {
            return entry.toString(prefix1, prefix2);
        } else {
            return "";
        }
    }

    /**获取Entry信息*/
    public static Entry getEntry() {
        return (Entry) entryStack.get();
    }

    /**entry中含有subentry，如此这样来进行循环来保持树状的结构*/
    private static Entry getCurrentEntry() {
        Entry subEntry = (Entry) entryStack.get();
        Entry entry = null;
        if (subEntry != null) {
            do {
                entry    = subEntry;
                subEntry = entry.getUnreleasedEntry();
            } while (subEntry != null);
        }
        return entry;
    }

    /**
     * 代表一个计时单元。
     */
    public static final class Entry {
    	//subEntries来表示树状的子节点
        private final List<Entry>   subEntries  = new ArrayList<Entry>(4);
        private final Object message;
        private final Entry  parentEntry;
        private final Entry  firstEntry;
        private final long   baseTime;
        private final long   startTime;
        private long         endTime;

        private Entry(Object message/*描述信息*/, Entry parentEntry/*父节点信息*/, Entry firstEntry/*第一个节点*/) {
            this.message     = message;
            this.startTime   = ProfilerSwitch.getInstance().isOpenProfilerNanoTime()==true?
            															 System.nanoTime():
            													System.currentTimeMillis();
            this.parentEntry = parentEntry;
            this.firstEntry  = (Entry) defaultIfNull(firstEntry, this);
            this.baseTime    = (firstEntry == null) ? 0 : firstEntry.startTime;
        }

        /**
         * 取得entry的信息。
         */
        public String getMessage() {
            return defaultIfEmpty((String)message, null);
        }
        
        public static String defaultIfEmpty(String str, String defaultStr) {
            return ((str == null) || (str.length() == 0)) ? defaultStr  : str;
        }
        
        public static Object defaultIfNull(Object object, Object defaultValue) {
            return (object != null) ? object : defaultValue;
        }
        /**获取当前节点的开始时间*/
        public long getStartTime() {
            return (baseTime > 0) ? (startTime - baseTime): 0;
        }

        /**获取当前节点的结束时间*/
        public long getEndTime() {
            if (endTime < baseTime) {
                return -1;
            } else {
                return endTime - baseTime;
            }
        }

        /**获取持续时间*/
        public long getDuration() {
            if (endTime < startTime) {
                return -1;
            } else {
                return endTime - startTime;
            }
        }

        /**取得entry自身所用的时间，即总时间减去所有子entry所用的时间。*/
        public long getDurationOfSelf() {
            long duration = getDuration();
            if (duration < 0) {
                return -1;
            } else if (subEntries.isEmpty()) {
                return duration;
            } else {
                for (int i = 0; i < subEntries.size(); i++) {
                    Entry subEntry = (Entry) subEntries.get(i);
                    duration -= subEntry.getDuration();
                }
                if (duration < 0) {
                    return -1;
                } else {
                    return duration;
                }
            }
        }

        /**取得当前entry在父entry中所占的时间百分比。*/
        public double getPecentage() {
            double parentDuration = 0;
            double duration = getDuration();
            if ((parentEntry != null) && parentEntry.isReleased()) {
                parentDuration = parentEntry.getDuration();
            }
            if ((duration > 0) && (parentDuration > 0)) {
                return duration / parentDuration;
            } else {
                return 0;
            }
        }

        /** 取得当前entry在第一个entry中所占的时间百分比。*/
        public double getPecentageOfAll() {
            double firstDuration = 0;
            double duration = getDuration();
            if ((firstEntry != null) && firstEntry.isReleased()) {
                firstDuration = firstEntry.getDuration();
            }
            if ((duration > 0) && (firstDuration > 0)) {
                return duration / firstDuration;
            } else {
                return 0;
            }
        }

        /**取得所有子entries。*/
        public List<Entry> getSubEntries() {
            return Collections.unmodifiableList(subEntries);
        }

        /**结束当前entry，并记录结束时间。    */
        private void release() {
            endTime = ProfilerSwitch.getInstance().isOpenProfilerNanoTime()==true?
            													System.nanoTime():
            											System.currentTimeMillis();
        }

        /**判断当前entry是否结束。*/
        public boolean isReleased() {
            return endTime > 0;
        }

        /**创建一个新的子entry。*/
        private void enterSubEntry(Object message) {
            Entry subEntry = new Entry(message, this, firstEntry);
            subEntries.add(subEntry);
        }

        /** 取得未结束的子entry,链表中的最后一个元素*/
        private Entry getUnreleasedEntry() {
            Entry subEntry = null;
            if (!subEntries.isEmpty()) {
                subEntry = (Entry) subEntries.get(subEntries.size() - 1);
                if (subEntry.isReleased()) {
                    subEntry = null;
                }
            }
            return subEntry;
        }

        public String toString() {
            return toString("", "");
        }

        private String toString(String prefix1, String prefix2) {
            StringBuffer buffer = new StringBuffer();
            toString(buffer, prefix1, prefix2);
            return buffer.toString();
        }

        private void toString(StringBuffer buffer, String prefix1, String prefix2) {
            buffer.append(prefix1);

            String   message        = getMessage();
            long     startTime      = getStartTime();
            long     duration       = getDuration();
            long     durationOfSelf = getDurationOfSelf();
            double   percent        = getPecentage();
            double   percentOfAll   = getPecentageOfAll();

            Object[] params = new Object[] {
                                  message, // {0} - entry信息 
            new Long(startTime), // {1} - 起始时间
            new Long(duration), // {2} - 持续总时间
            new Long(durationOfSelf), // {3} - 自身消耗的时间
            new Double(percent), // {4} - 在父entry中所占的时间比例
            new Double(percentOfAll) // {5} - 在总时间中所旧的时间比例
                              };

            StringBuffer pattern = new StringBuffer("{1,number} ");

            if (isReleased()) {
                pattern.append("[{2,number}");
                if(ProfilerSwitch.getInstance().isOpenProfilerNanoTime()){
                	pattern.append("ns");
                }else{
                	pattern.append("ms");
                }

                if ((durationOfSelf > 0) && (durationOfSelf != duration)) {
                    pattern.append(" ({3,number})");
                    if(ProfilerSwitch.getInstance().isOpenProfilerNanoTime()){
                    	pattern.append("ns");
                    }else{
                    	pattern.append("ms");
                    }
                }

                
                if (percent > 0) {
                    pattern.append(", {4,number,##%}");
                }

                if (percentOfAll > 0) {
                    pattern.append(", {5,number,##%}");
                }

                pattern.append("]");
            } else {
                pattern.append("[UNRELEASED]");
            }

            if (message != null) {
                pattern.append(" - {0}");
            }

            buffer.append(MessageFormat.format(pattern.toString(), params));

            for (int i = 0; i < subEntries.size(); i++) {
                Entry subEntry = (Entry) subEntries.get(i);

                buffer.append('\n');

                if (i == (subEntries.size() - 1)) {
                    subEntry.toString(buffer, prefix2 + "`---", prefix2 + "    "); // 最后一项
                } else if (i == 0) {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   "); // 第一项
                } else {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   "); // 中间项
                }
            }
        }
    }
    
    public static String[] split(String str, String separatorChars) {
        return split(str, separatorChars, -1);
    }
    private static String[] split(String str, String separatorChars, int max) {
        if (str == null) {
            return null;
        }

        int length = str.length();

        if (length == 0) {
            return new String[0];
        }

        List<String> list = new LinkedList<String>();
        int sizePlus1 = 1;
        int i = 0;
        int start = 0;
        boolean match = false;

        if (separatorChars == null) {
            // null表示使用空白作为分隔符
            while (i < length) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match) {
                        if (sizePlus1++ == max) {
                            i = length;
                        }

                        list.add(str.substring(start, i));
                        match = false;
                    }

                    start = ++i;
                    continue;
                }

                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            // 优化分隔符长度为1的情形
            char sep = separatorChars.charAt(0);

            while (i < length) {
                if (str.charAt(i) == sep) {
                    if (match) {
                        if (sizePlus1++ == max) {
                            i = length;
                        }

                        list.add(str.substring(start, i));
                        match = false;
                    }

                    start = ++i;
                    continue;
                }

                match = true;
                i++;
            }
        } else {
            // 一般情形
            while (i < length) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match) {
                        if (sizePlus1++ == max) {
                            i = length;
                        }

                        list.add(str.substring(start, i));
                        match = false;
                    }

                    start = ++i;
                    continue;
                }

                match = true;
                i++;
            }
        }

        if (match) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

}
