package Project.Common;

import java.util.logging.*;

import Project.Common.LoggerUtil.LoggerConfig;

public enum LoggerUtil {
    INSTANCE;

    public static class LoggerConfig {
        private int fileSizeLimit = 1024 * 1024;
        private int fileCount = 1;
        private String logLocation = "app.log";

        public int getFileSizeLimit() { return fileSizeLimit; }
        public void setFileSizeLimit(int fileSizeLimit) { this.fileSizeLimit = fileSizeLimit; }

        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }

        public String getLogLocation() { return logLocation; }
        public void setLogLocation(String logLocation) { this.logLocation = logLocation; }
    }

    private Logger logger = Logger.getLogger("IT114Client");
    private boolean configured = false;

    public void setConfig(LoggerConfig config) {
        if (configured) return;
        try {
            logger.setUseParentHandlers(false);
            for (Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
            }
            Handler fh = new FileHandler(
                    config.getLogLocation(),
                    config.getFileSizeLimit(),
                    config.getFileCount(),
                    true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            configured = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void info(String msg) {
        logger.info(msg);
        System.out.println(msg);
    }
    public void fine(String msg) {
        logger.fine(msg);
        System.out.println(msg);
    }

    public void warning(String msg, Throwable t) {
        logger.log(Level.WARNING, msg, t);
        System.out.println(msg);
        t.printStackTrace();
    }

    public void warning(String msg) {
        logger.warning(msg);
        System.out.println(msg);
    }

    public void severe(String msg) {
        logger.severe(msg);
        System.err.println(msg);
    }

    public void severe(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
        System.err.println(msg);
        t.printStackTrace();
    }
}
