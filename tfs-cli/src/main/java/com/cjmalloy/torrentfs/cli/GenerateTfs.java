package com.cjmalloy.torrentfs.cli;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;

import com.cjmalloy.torrentfs.model.Encoding;
import com.cjmalloy.torrentfs.util.TfsUtil;
import com.turn.ttorrent.common.Torrent;
import jargs.gnu.CmdLineParser;
import org.apache.log4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateTfs {

  private static final Logger logger = LoggerFactory.getLogger(GenerateTfs.class);

  /**
   * Display program usage on the given {@link PrintStream}.
   */
  private static void usage(PrintStream s) {
    usage(s, null);
  }

  /**
   * Display a message and program usage on the given {@link PrintStream}.
   */
  private static void usage(PrintStream s, String msg) {
    if (msg != null) {
      s.println(msg);
      s.println();
    }

    s.println("usage: Torrent [options] [file|directory]");
    s.println();
    s.println("Available options:");
    s.println("  -h,--help             Show this help and exit.");
    s.println();
    s.println("  -a,--announce         Tracker URL (can be repeated).");
    s.println("  -c,--cache            Seed cache to initialize");
    s.println("  -s,--symbolic-links   Use symbolic links to initialize seed cache");
    s.println();
  }

  /**
   * Torrent reader and creator.
   *
   * <p> You can use the {@code main()} function of this class to read or create
   * torrent files. See usage for details.</p>
   */
  public static void main(String[] args) {
    BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%-5p: %m%n")));

    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option argHelp = parser.addBooleanOption('h', "help");
    CmdLineParser.Option argAnnounce = parser.addStringOption('a', "announce");
    CmdLineParser.Option argCache = parser.addStringOption('c', "cache");
    CmdLineParser.Option argLink = parser.addBooleanOption('s', "symbolic-links");

    try {
      parser.parse(args);
    } catch (CmdLineParser.OptionException oe) {
      System.err.println(oe.getMessage());
      usage(System.err);
      System.exit(1);
    }

    // Display help and exit if requested
    if (Boolean.TRUE.equals(parser.getOptionValue(argHelp))) {
      usage(System.out);
      System.exit(0);
    }

    // For repeated announce urls
    @SuppressWarnings("unchecked")
    Vector<String> announceUrls = (Vector<String>) parser.getOptionValues(argAnnounce);
    String cache = (String) parser.getOptionValue(argCache, null);
    boolean link = (Boolean) parser.getOptionValue(argLink, false);

    File cacheFile = cache == null ? null : new File(cache);

    String[] otherArgs = parser.getRemainingArgs();

    try {
      // Process the announce URLs into URIs
      List<URI> announceUris = new ArrayList<URI>();
      for (String url : announceUrls) {
        announceUris.add(new URI(url));
      }

      // Create the announce-list as a list of lists of URIs
      // Assume all the URI's are first tier trackers
      List<List<URI>> announceList = new ArrayList<List<URI>>();
      announceList.add(announceUris);

      File source = new File(otherArgs[0]);
      if (!source.exists() || !source.canRead()) {
        throw new IllegalArgumentException("Cannot access source file or directory "
          + source.getName());
      }

      String creator = String.format("%s (ttorrent)", System.getProperty("user.name"));

      List<Torrent> torrents = TfsUtil.generateTorrentFromTfs(source, Encoding.BENCODE_BASE64, announceList, creator, cacheFile, link);
      TfsUtil.saveTorrents(new File("."), torrents);
    } catch (Exception e) {
      logger.error("{}", e.getMessage(), e);
      System.exit(2);
    }
  }
}
