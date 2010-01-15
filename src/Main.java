import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Coordinate downloading and parsing of descriptors and extraction of
 * statistically relevant data for later processing with R.
 */
public class Main {
// TODO handle exceptions better!
  public static void main(String[] args) throws IOException,
      ParseException {

    // use lock file to avoid overlapping runs
    File lockFile = new File("lock");
    if (lockFile.exists()) {
      BufferedReader br = new BufferedReader(new FileReader("lock"));
      long runStarted = Long.parseLong(br.readLine());
      br.close();
      if (System.currentTimeMillis() - runStarted < 15L * 60L * 1000L) {
        System.out.println("Warning: ERNIE is already running or has not "
            + "exited cleanly! Exiting.");
        System.exit(1);
      }
    }
    BufferedWriter bw = new BufferedWriter(new FileWriter("lock"));
    bw.append("" + System.currentTimeMillis() + "\n");
    bw.close();

    // Should we only import from disk or only download descriptors?
    boolean importOnly = args.length > 0
        && args[0].equals("import");
    boolean downloadOnly = args.length > 0
        && args[0].equals("download");

    // Define which stats we are interested in
    String authority = "80.190.246.100";
    SortedSet<String> countries = new TreeSet<String>();
    countries.add("bh");
    countries.add("cn");
    countries.add("cu");
    countries.add("et");
    countries.add("ir");
    countries.add("mm");
    countries.add("sa");
    countries.add("sy");
    countries.add("tn");
    countries.add("tm");
    countries.add("uz");
    countries.add("vn");
    countries.add("ye");
    SortedMap<String, String> directories = new TreeMap<String, String>();
    directories.put("8522EB98C91496E80EC238E732594D1509158E77",
        "trusted");
    directories.put("FFCB46DB1339DA84674C70D7CB586434C4370441",
        "moria1");

    // Initialize stats file handlers
    String statsDirectory = "stats";
    ConsensusStatsFileHandler csfh = new ConsensusStatsFileHandler(
        statsDirectory);
    BridgeStatsFileHandler bsfh = new BridgeStatsFileHandler(
        statsDirectory, countries);
    DirreqStatsFileHandler dsfh = new DirreqStatsFileHandler(
        statsDirectory, countries);

    // Initialize parsers
    RelayDescriptorParser rdp = new RelayDescriptorParser(csfh, bsfh,
        dsfh, countries, directories);
    BridgeDescriptorParser bdp = new BridgeDescriptorParser(csfh, bsfh,
        countries);

    // Read files in archives/ and bridges/ directory
    if (!downloadOnly) {
      ArchiveReader ar = new ArchiveReader(rdp, "archives");
      SanitizedBridgesReader sbr = new SanitizedBridgesReader(bdp,
          "bridges", countries);
      BridgeSnapshotReader bsr = new BridgeSnapshotReader(bdp,
          "bridge-directories", "stats/parsed-bridge-directories",
          countries);
      TorperfProcessor tp = new TorperfProcessor("stats", "torperf");
    }

    // Download current descriptors
    if (!importOnly) {
      ConsensusDownloader cd = new ConsensusDownloader(rdp, authority);
      ExtraInfoDownloader eid = new ExtraInfoDownloader(rdp, authority,
          directories);
    }

    // Write updated stats files to disk
    bsfh.writeFile();
    csfh.writeFile();
    dsfh.writeFile();

    // Remove lock file
    lockFile.delete();
  }
}

