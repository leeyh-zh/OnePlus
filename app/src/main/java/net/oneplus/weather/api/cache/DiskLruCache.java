package net.oneplus.weather.api.cache;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.oneplus.weather.api.cache.DiskLruCache.Editor;
import net.oneplus.weather.util.StringUtils;
import net.oneplus.weather.widget.openglbase.RainSurfaceView;

public final class DiskLruCache implements Closeable {
    static final long ANY_SEQUENCE_NUMBER = -1;
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    private static final int IO_BUFFER_SIZE = 8192;
    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_TMP = "journal.tmp";
    static final String MAGIC = "libcore.io.DiskLruCache";
    private static final String READ = "READ";
    private static final String REMOVE = "REMOVE";
    private static final Charset UTF_8;
    static final String VERSION_1 = "1";
    private final int appVersion;
    private final Callable<Void> cleanupCallable;
    private final File directory;
    private final ExecutorService executorService;
    private final File journalFile;
    private final File journalFileTmp;
    private Writer journalWriter;
    private final LinkedHashMap<String, Entry> lruEntries;
    private final long maxSize;
    private long nextSequenceNumber;
    private int redundantOpCount;
    private long size;
    private final int valueCount;

    public final class Editor {
        private final Entry entry;
        private boolean hasErrors;

        private class FaultHidingOutputStream extends FilterOutputStream {
            private FaultHidingOutputStream(OutputStream out) {
                super(out);
            }

            public void write(int oneByte) {
                try {
                    this.out.write(oneByte);
                } catch (IOException e) {
                    net.oneplus.weather.api.cache.DiskLruCache.Editor.this.hasErrors = true;
                }
            }

            public void write(byte[] buffer, int offset, int length) {
                try {
                    this.out.write(buffer, offset, length);
                } catch (IOException e) {
                    net.oneplus.weather.api.cache.DiskLruCache.Editor.this.hasErrors = true;
                }
            }

            public void close() {
                try {
                    this.out.close();
                } catch (IOException e) {
                    net.oneplus.weather.api.cache.DiskLruCache.Editor.this.hasErrors = true;
                }
            }

            public void flush() {
                try {
                    this.out.flush();
                } catch (IOException e) {
                    net.oneplus.weather.api.cache.DiskLruCache.Editor.this.hasErrors = true;
                }
            }
        }

        private Editor(Entry entry) {
            this.entry = entry;
        }

        public InputStream newInputStream(int index) throws IOException {
            InputStream fileInputStream;
            synchronized (DiskLruCache.this) {
                if (this.entry.currentEditor != this) {
                    throw new IllegalStateException();
                } else if (this.entry.readable) {
                    fileInputStream = new FileInputStream(this.entry.getCleanFile(index));
                } else {
                    fileInputStream = null;
                }
            }
            return fileInputStream;
        }

        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? DiskLruCache.inputStreamToString(in) : null;
        }

        public OutputStream newOutputStream(int index) throws IOException {
            OutputStream faultHidingOutputStream;
            synchronized (DiskLruCache.this) {
                if (this.entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                faultHidingOutputStream = new FaultHidingOutputStream(new FileOutputStream(this.entry.getDirtyFile(index)), null);
            }
            return faultHidingOutputStream;
        }

        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                Writer writer2 = new OutputStreamWriter(newOutputStream(index), UTF_8);
                try {
                    writer2.write(value);
                    DiskLruCache.closeQuietly(writer2);
                } catch (Throwable th) {
                    Throwable th2 = th;
                    writer = writer2;
                    DiskLruCache.closeQuietly(writer);
                    throw th2;
                }
            } catch (Throwable th3) {
                th2 = th3;
                DiskLruCache.closeQuietly(writer);
                throw th2;
            }
        }

        public void commit() throws IOException {
            if (this.hasErrors) {
                DiskLruCache.this.completeEdit(this, false);
                DiskLruCache.this.remove(this.entry.key);
                return;
            }
            DiskLruCache.this.completeEdit(this, true);
        }

        public void abort() throws IOException {
            DiskLruCache.this.completeEdit(this, false);
        }
    }

    private final class Entry {
        private Editor currentEditor;
        private final String key;
        private final long[] lengths;
        private boolean readable;
        private long sequenceNumber;

        private Entry(String key) {
            this.key = key;
            this.lengths = new long[DiskLruCache.this.valueCount];
        }

        public String getLengths() throws IOException {
            StringBuilder result = new StringBuilder();
            for (long size : this.lengths) {
                result.append(' ').append(size);
            }
            return result.toString();
        }

        private void setLengths(String[] strings) throws IOException {
            if (strings.length != DiskLruCache.this.valueCount) {
                throw invalidLengths(strings);
            }
            int i = 0;
            while (i < strings.length) {
                try {
                    this.lengths[i] = Long.parseLong(strings[i]);
                    i++;
                } catch (NumberFormatException e) {
                    throw invalidLengths(strings);
                }
            }
        }

        private IOException invalidLengths(String[] strings) throws IOException {
            throw new IOException("unexpected journal line: " + Arrays.toString(strings));
        }

        public File getCleanFile(int i) {
            return new File(DiskLruCache.this.directory, this.key + "." + i);
        }

        public File getDirtyFile(int i) {
            return new File(DiskLruCache.this.directory, this.key + "." + i + ".tmp");
        }
    }

    public final class Snapshot implements Closeable {
        private final InputStream[] ins;
        private final String key;
        private final long sequenceNumber;

        private Snapshot(String key, long sequenceNumber, InputStream[] ins) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.ins = ins;
        }

        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(this.key, this.sequenceNumber);
        }

        public InputStream getInputStream(int index) {
            return this.ins[index];
        }

        public String getString(int index) throws IOException {
            return DiskLruCache.inputStreamToString(getInputStream(index));
        }

        public void close() {
            for (InputStream in : this.ins) {
                DiskLruCache.closeQuietly(in);
            }
        }
    }

    static {
        UTF_8 = Charset.forName("UTF-8");
    }

    private static <T> T[] copyOfRange(T[] original, int start, int end) {
        int originalLength = original.length;
        if (start > end) {
            throw new IllegalArgumentException();
        } else if (start < 0 || start > originalLength) {
            throw new ArrayIndexOutOfBoundsException();
        } else {
            int resultLength = end - start;
            Object[] result = (Object[]) Array.newInstance(original.getClass().getComponentType(), resultLength);
            System.arraycopy(original, start, result, 0, Math.min(resultLength, originalLength - start));
            return result;
        }
    }

    public static String readFully(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        char[] buffer = new char[1024];
        while (true) {
            int count = reader.read(buffer);
            if (count != -1) {
                writer.write(buffer, 0, count);
            } else {
                String toString = writer.toString();
                reader.close();
                return toString;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String readAsciiLine(java.io.InputStream r5_in) throws java.io.IOException {
        throw new UnsupportedOperationException("Method not decompiled: net.oneplus.weather.api.cache.DiskLruCache.readAsciiLine(java.io.InputStream):java.lang.String");
        /*
        r2 = new java.lang.StringBuilder;
        r3 = 80;
        r2.<init>(r3);
    L_0x0007:
        r0 = r5.read();
        r3 = -1;
        if (r0 != r3) goto L_0x0014;
    L_0x000e:
        r3 = new java.io.EOFException;
        r3.<init>();
        throw r3;
    L_0x0014:
        r3 = 10;
        if (r0 != r3) goto L_0x0032;
    L_0x0018:
        r1 = r2.length();
        if (r1 <= 0) goto L_0x002d;
    L_0x001e:
        r3 = r1 + -1;
        r3 = r2.charAt(r3);
        r4 = 13;
        if (r3 != r4) goto L_0x002d;
    L_0x0028:
        r3 = r1 + -1;
        r2.setLength(r3);
    L_0x002d:
        r3 = r2.toString();
        return r3;
    L_0x0032:
        r3 = (char) r0;
        r2.append(r3);
        goto L_0x0007;
        */
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }

    public static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("not a directory: " + dir);
        }
        int length = files.length;
        int i = 0;
        while (i < length) {
            File file = files[i];
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (file.delete()) {
                i++;
            } else {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
        this.size = 0;
        this.lruEntries = new LinkedHashMap(0, 0.75f, true);
        this.nextSequenceNumber = 0;
        this.executorService = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.cleanupCallable = new Callable<Void>() {
            public Void call() throws Exception {
                synchronized (DiskLruCache.this) {
                    if (DiskLruCache.this.journalWriter == null) {
                    } else {
                        DiskLruCache.this.trimToSize();
                        if (DiskLruCache.this.journalRebuildRequired()) {
                            DiskLruCache.this.rebuildJournal();
                            DiskLruCache.this.redundantOpCount = 0;
                        }
                    }
                }
                return null;
            }
        };
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
    }

    public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        } else if (valueCount <= 0) {
            throw new IllegalArgumentException("valueCount <= 0");
        } else {
            DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
            if (cache.journalFile.exists()) {
                try {
                    cache.readJournal();
                    cache.processJournal();
                    cache.journalWriter = new BufferedWriter(new FileWriter(cache.journalFile, true), 8192);
                    return cache;
                } catch (IOException e) {
                    cache.delete();
                }
            }
            directory.mkdirs();
            cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
            cache.rebuildJournal();
            return cache;
        }
    }

    private void readJournal() throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(this.journalFile), 8192);
        String magic = readAsciiLine(in);
        String version = readAsciiLine(in);
        String appVersionString = readAsciiLine(in);
        String valueCountString = readAsciiLine(in);
        String blank = readAsciiLine(in);
        if (MAGIC.equals(magic) && VERSION_1.equals(version) && Integer.toString(this.appVersion).equals(appVersionString) && Integer.toString(this.valueCount).equals(valueCountString) && StringUtils.EMPTY_STRING.equals(blank)) {
            while (true) {
                try {
                    readJournalLine(readAsciiLine(in));
                } catch (EOFException e) {
                    closeQuietly(in);
                }
            }
        }
        throw new IOException("unexpected journal header: [" + magic + ", " + version + ", " + valueCountString + ", " + blank + "]");
    }

    private void readJournalLine(String line) throws IOException {
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            throw new IOException("unexpected journal line: " + line);
        }
        String key = parts[1];
        if (parts[0].equals(REMOVE) && parts.length == 2) {
            this.lruEntries.remove(key);
            return;
        }
        Entry entry = (Entry) this.lruEntries.get(key);
        if (entry == null) {
            entry = new Entry(key, null);
            this.lruEntries.put(key, entry);
        }
        if (parts[0].equals(CLEAN) && parts.length == this.valueCount + 2) {
            entry.readable = true;
            entry.currentEditor = null;
            entry.setLengths((String[]) copyOfRange(parts, RainSurfaceView.RAIN_LEVEL_SHOWER, parts.length));
        } else if (parts[0].equals(DIRTY) && parts.length == 2) {
            entry.currentEditor = new Editor(entry, null);
        } else if (!parts[0].equals(READ) || parts.length != 2) {
            throw new IOException("unexpected journal line: " + line);
        }
    }

    private void processJournal() throws IOException {
        deleteIfExists(this.journalFileTmp);
        Iterator<Entry> i = this.lruEntries.values().iterator();
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
            int t;
            if (entry.currentEditor == null) {
                for (t = 0; t < this.valueCount; t++) {
                    this.size += entry.lengths[t];
                }
            } else {
                entry.currentEditor = null;
                for (t = 0; t < this.valueCount; t++) {
                    deleteIfExists(entry.getCleanFile(t));
                    deleteIfExists(entry.getDirtyFile(t));
                }
                i.remove();
            }
        }
    }

    private synchronized void rebuildJournal() throws IOException {
        if (this.journalWriter != null) {
            this.journalWriter.close();
        }
        Writer writer = new BufferedWriter(new FileWriter(this.journalFileTmp), 8192);
        writer.write(MAGIC);
        writer.write("\n");
        writer.write(VERSION_1);
        writer.write("\n");
        writer.write(Integer.toString(this.appVersion));
        writer.write("\n");
        writer.write(Integer.toString(this.valueCount));
        writer.write("\n");
        writer.write("\n");
        for (Entry entry : this.lruEntries.values()) {
            if (entry.currentEditor != null) {
                writer.write("DIRTY " + entry.key + '\n');
            } else {
                writer.write("CLEAN " + entry.key + entry.getLengths() + '\n');
            }
        }
        writer.close();
        this.journalFileTmp.renameTo(this.journalFile);
        this.journalWriter = new BufferedWriter(new FileWriter(this.journalFile, true), 8192);
    }

    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException();
        }
    }

    public synchronized Snapshot get(String key) throws IOException {
        Snapshot snapshot = null;
        synchronized (this) {
            checkNotClosed();
            validateKey(key);
            Entry entry = (Entry) this.lruEntries.get(key);
            if (entry != null) {
                if (entry.readable) {
                    InputStream[] ins = new InputStream[this.valueCount];
                    int i = 0;
                    while (i < this.valueCount) {
                        try {
                            ins[i] = new FileInputStream(entry.getCleanFile(i));
                            i++;
                        } catch (FileNotFoundException e) {
                        }
                    }
                    this.redundantOpCount++;
                    this.journalWriter.append("READ " + key + '\n');
                    if (journalRebuildRequired()) {
                        this.executorService.submit(this.cleanupCallable);
                    }
                    snapshot = new Snapshot(key, entry.sequenceNumber, ins, null);
                }
            }
        }
        return snapshot;
    }

    public Editor edit(String key) throws IOException {
        return edit(key, ANY_SEQUENCE_NUMBER);
    }

    private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
        Editor editor = null;
        synchronized (this) {
            checkNotClosed();
            validateKey(key);
            Entry entry = (Entry) this.lruEntries.get(key);
            if (expectedSequenceNumber == -1 || (entry != null && entry.sequenceNumber == expectedSequenceNumber)) {
                if (entry == null) {
                    entry = new Entry(key, null);
                    this.lruEntries.put(key, entry);
                } else if (entry.currentEditor != null) {
                }
                editor = new Editor(entry, null);
                entry.currentEditor = editor;
                this.journalWriter.write("DIRTY " + key + '\n');
                this.journalWriter.flush();
            }
        }
        return editor;
    }

    public File getDirectory() {
        return this.directory;
    }

    public long maxSize() {
        return this.maxSize;
    }

    public synchronized long size() {
        return this.size;
    }

    private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.currentEditor != editor) {
            throw new IllegalStateException();
        }
        int i;
        if (success) {
            if (!entry.readable) {
                i = 0;
                while (i < this.valueCount) {
                    if (entry.getDirtyFile(i).exists()) {
                        i++;
                    } else {
                        editor.abort();
                        throw new IllegalStateException("edit didn't create file " + i);
                    }
                }
            }
        }
        for (i = 0; i < this.valueCount; i++) {
            File dirty = entry.getDirtyFile(i);
            if (!success) {
                deleteIfExists(dirty);
            } else if (dirty.exists()) {
                File clean = entry.getCleanFile(i);
                dirty.renameTo(clean);
                long oldLength = entry.lengths[i];
                long newLength = clean.length();
                entry.lengths[i] = newLength;
                this.size = (this.size - oldLength) + newLength;
            }
        }
        this.redundantOpCount++;
        entry.currentEditor = null;
        if ((entry.readable | success) != 0) {
            entry.readable = true;
            this.journalWriter.write("CLEAN " + entry.key + entry.getLengths() + '\n');
            if (success) {
                long j = this.nextSequenceNumber;
                this.nextSequenceNumber = 1 + j;
                entry.sequenceNumber = j;
            }
        } else {
            this.lruEntries.remove(entry.key);
            this.journalWriter.write("REMOVE " + entry.key + '\n');
        }
        if (this.size > this.maxSize || journalRebuildRequired()) {
            this.executorService.submit(this.cleanupCallable);
        }
    }

    private boolean journalRebuildRequired() {
        return this.redundantOpCount >= 2000 && this.redundantOpCount >= this.lruEntries.size();
    }

    public synchronized boolean remove(String key) throws IOException {
        boolean z;
        checkNotClosed();
        validateKey(key);
        Entry entry = (Entry) this.lruEntries.get(key);
        if (entry == null || entry.currentEditor != null) {
            z = false;
        } else {
            int i = 0;
            while (i < this.valueCount) {
                File file = entry.getCleanFile(i);
                if (file.delete()) {
                    this.size -= entry.lengths[i];
                    entry.lengths[i] = 0;
                    i++;
                } else {
                    throw new IOException("failed to delete " + file);
                }
            }
            this.redundantOpCount++;
            this.journalWriter.append("REMOVE " + key + '\n');
            this.lruEntries.remove(key);
            if (journalRebuildRequired()) {
                this.executorService.submit(this.cleanupCallable);
            }
            z = true;
        }
        return z;
    }

    public boolean isClosed() {
        return this.journalWriter == null;
    }

    private void checkNotClosed() {
        if (this.journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    public synchronized void flush() throws IOException {
        checkNotClosed();
        trimToSize();
        this.journalWriter.flush();
    }

    public synchronized void close() throws IOException {
        if (this.journalWriter != null) {
            Iterator it = new ArrayList(this.lruEntries.values()).iterator();
            while (it.hasNext()) {
                Entry entry = (Entry) it.next();
                if (entry.currentEditor != null) {
                    entry.currentEditor.abort();
                }
            }
            trimToSize();
            this.journalWriter.close();
            this.journalWriter = null;
        }
    }

    private void trimToSize() throws IOException {
        while (this.size > this.maxSize) {
            remove((String) ((java.util.Map.Entry) this.lruEntries.entrySet().iterator().next()).getKey());
        }
    }

    public void delete() throws IOException {
        close();
        deleteContents(this.directory);
    }

    private void validateKey(String key) {
        if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
            throw new IllegalArgumentException("keys must not contain spaces or newlines: \"" + key + "\"");
        }
    }

    private static String inputStreamToString(InputStream in) throws IOException {
        return readFully(new InputStreamReader(in, UTF_8));
    }
}
