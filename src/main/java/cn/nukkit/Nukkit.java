package cn.nukkit;

import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.ServerKiller;
import com.google.common.base.Preconditions;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;

@Log4j2
public class Nukkit {
    public final static String VERSION = "1.0.1";
    public final static String API_VERSION = "1.0.0";

    public final static String CARROT_VERSION = "1.0.1";
    public final static String CODENAME = "https://vk.com/tunekovv";

    public final static String NAME = "Nukkit";
    @Deprecated
    public final static String[] MINECRAFT_VERSION = ProtocolInfo.MINECRAFT_VERSION;
    @Deprecated
    public final static String MINECRAFT_VERSION_NETWORK = ProtocolInfo.MINECRAFT_VERSION_NETWORK;

    public final static String PATH = System.getProperty("user.dir") + "/";
    public final static String DATA_PATH = System.getProperty("user.dir") + "/";
    public final static String PLUGIN_PATH = DATA_PATH + "plugins";
    public static final long START_TIME = System.currentTimeMillis();
    public static boolean ANSI = true;
    public static boolean TITLE = false;
    public static boolean shortTitle = requiresShortTitle();
    public static int DEBUG = 1;

    public static void main(String[] args) {

        System.setProperty("java.net.preferIPv4Stack" , "true");
        System.setProperty("log4j.skipJansi", "false");
        System.getProperties().putIfAbsent("io.netty.allocator.type", "unpooled");

        System.setProperty("leveldb.mmap", "true");

        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        OptionSpec<Void> helpSpec = parser.accepts("help", "Shows this page").forHelp();
        OptionSpec<Void> ansiSpec = parser.accepts("disable-ansi", "Disables console coloring");
        OptionSpec<Void> titleSpec = parser.accepts("enable-title", "Enables title at the top of the window");
        OptionSpec<String> vSpec = parser.accepts("v", "Set verbosity of logging").withRequiredArg().ofType(String.class);
        OptionSpec<String> verbositySpec = parser.accepts("verbosity", "Set verbosity of logging").withRequiredArg().ofType(String.class);
        OptionSpec<String> languageSpec = parser.accepts("language", "Set a predefined language").withOptionalArg().ofType(String.class);

        OptionSet options = parser.parse(args);

        if (options.has(helpSpec)) {
            try {
                // Display help page
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                // ignore
            }
            return;
        }

        ANSI = !options.has(ansiSpec);
        TITLE = options.has(titleSpec);

        String verbosity = options.valueOf(vSpec);
        if (verbosity == null) {
            verbosity = options.valueOf(verbositySpec);
        }
        if (verbosity != null) {

            try {
                Level level = Level.valueOf(verbosity);
                setLogLevel(level);
            } catch (Exception e) {
                // ignore
            }
        }

        String language = options.valueOf(languageSpec);

        try {
            if (TITLE) {
                System.out.print((char) 0x1b + "]0;Nukkit is starting up..." + (char) 0x07);
            }
            new Server(PATH, DATA_PATH, PLUGIN_PATH, language);
        } catch (Throwable t) {
            log.throwing(t);
        }

        if (TITLE) {
            System.out.print((char) 0x1b + "]0;Stopping Server..." + (char) 0x07);
        }
        log.info("Stopping other threads");

        for (Thread thread : java.lang.Thread.getAllStackTraces().keySet()) {
            if (!(thread instanceof InterruptibleThread)) {
                continue;
            }
            log.debug("Stopping {} thread", thread.getClass().getSimpleName());
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        ServerKiller killer = new ServerKiller(8);
        killer.start();

        if (TITLE) {
            System.out.print((char) 0x1b + "]0;Server Stopped" + (char) 0x07);
        }
        System.exit(0);
    }

    private static boolean requiresShortTitle() {
        //Shorter title for windows 8/2012
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows") &&(osName.contains("windows 8") || osName.contains("2012"));
    }

    public static void setLogLevel(Level level) {
        Preconditions.checkNotNull(level, "level");
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    public static Level getLogLevel() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        return loggerConfig.getLevel();
    }
}
