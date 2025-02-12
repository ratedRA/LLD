package com.ratedra.logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class LoggerLLD {
    public static void main(String[] args) {
        LogManager logManager = LogManagerFactory.getLogManager(AppenderType.FILE);
        Logger logger = logManager.getLogger(LoggerLLD.class.getCanonicalName(), LogLevel.INFO);

        logger.log("My first log", LogLevel.INFO);
        logger.log("My second log", LogLevel.ERROR);

        LogManager logManager2 = LogManagerFactory.getLogManager(AppenderType.CONSOLE);
        Logger logger2 = logManager2.getLogger(LoggerLLD.class.getCanonicalName(), LogLevel.INFO);

        logger2.log("My first log", LogLevel.INFO);
        logger2.log("My second log", LogLevel.ERROR);
    }


}

enum LogLevel{
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    private int ordinal;
    LogLevel(int ordinal){
        this.ordinal = ordinal;
    }

    public int getOrdinal(){
        return ordinal;
    }
}

enum AppenderType{
    CONSOLE,
    FILE;
}

interface LogAppender{
    void append(String message);
}

class ConsoleLogAppender implements LogAppender{
    private static ConsoleLogAppender INSTANCE;
    private ConsoleLogAppender(){

    }
    public static ConsoleLogAppender getInstance(){
        if(INSTANCE == null){
            synchronized (ConsoleLogAppender.class.getCanonicalName()) {
                if(INSTANCE == null) {
                    INSTANCE = new ConsoleLogAppender();
                }
            }
        }

        return INSTANCE;
    }
    @Override
    public void append(String message) {
        System.out.println(message);
    }
}

class FileLogAppender implements LogAppender{
    private static final String GLOBAL_LOG_FILE_PATH = "/Users/amanprasad/codebase/LLD/src/main/java/com/ratedra/logger/logFile.txt";
    LogAppender fallBackAppender;

    private static FileLogAppender INSTANCE;
    public static FileLogAppender getInstance(){
        if(INSTANCE == null){
            synchronized (FileLogAppender.class.getCanonicalName()) {
                if(INSTANCE == null) {
                    INSTANCE = new FileLogAppender();
                }
            }
        }
        return INSTANCE;
    }

    private FileLogAppender(){
        fallBackAppender = ConsoleLogAppender.getInstance();
        clearLogFile();
    }


    @Override
    public synchronized void append(String message) {
        appendWithRetry(message, 3);
    }

    private void appendWithRetry(String message, int retriesLeft){
        if(retriesLeft == 0){
            fallBackAppender.append("failed to append the log with FileLogAppender, using fallBack appender, log is: " + message);
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(GLOBAL_LOG_FILE_PATH, true))){
            writer.write(message);
            writer.newLine();
        } catch (Exception ex){
            try{
                Thread.sleep(100);
            } catch (Exception e){
                Thread.currentThread().interrupt();
            }
            appendWithRetry(message, retriesLeft - 1);
        }
    }

    private void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GLOBAL_LOG_FILE_PATH, false))) {
            // Opening in 'false' mode clears the file
        } catch (IOException ex) {
            System.err.println("Error clearing log file: " + ex.getMessage());
        }
    }
}

class Logger{
    String className;
    LogLevel minAllowedLevel;
    LogAppender logAppender;


    public Logger(String className, LogLevel minAllowedLevel, LogAppender appender) {
        this.className = className;
        this.minAllowedLevel = minAllowedLevel;
        this.logAppender = appender;
    }

    public synchronized void log(String message, LogLevel level){
        if(level.getOrdinal() >= minAllowedLevel.getOrdinal()){
            String formattedLogMessage = String.format("%s %s %s: %s", new Date(), level.name(), className, message);
            logAppender.append(formattedLogMessage);
        }
    }
}

class LogManagerFactory{
    public synchronized static LogManager getLogManager(AppenderType type){
        LogManager logManager = null;
        switch (type){
            case CONSOLE:
                logManager = ConsoleLogManager.getInstance();
                break;
            case FILE:
                logManager = FileLogManager.getInstance();
                break;
            default:
                throw new RuntimeException("can't find LogManager for the appender type: " + type);
        }
        return logManager;
    }
}

interface LogManager{
    Logger getLogger(String name, LogLevel minAllowedLevel);
}

class ConsoleLogManager implements LogManager{
    final LogAppender appender = ConsoleLogAppender.getInstance();

    private static ConsoleLogManager INSTANCE;
    private ConsoleLogManager(){
    }
    public static ConsoleLogManager getInstance(){
        if(INSTANCE == null){
            synchronized (ConsoleLogManager.class.getCanonicalName()){
                if(INSTANCE == null){
                    INSTANCE = new ConsoleLogManager();
                }
            }
        }

        return INSTANCE;
    }

    @Override
    public Logger getLogger(String name, LogLevel minAllowedLevel) {
        return new Logger(name, minAllowedLevel, appender);
    }
}

class FileLogManager implements LogManager{
    final LogAppender appender = FileLogAppender.getInstance();

    private static FileLogManager INSTANCE;
    private FileLogManager(){
    }
    public static FileLogManager getInstance(){
        if(INSTANCE == null){
            synchronized (FileLogManager.class.getCanonicalName()){
                if(INSTANCE == null){
                    INSTANCE = new FileLogManager();
                }
            }
        }

        return INSTANCE;
    }

    @Override
    public Logger getLogger(String name, LogLevel minAllowedLevel) {
        return new Logger(name, minAllowedLevel, appender);
    }
}
