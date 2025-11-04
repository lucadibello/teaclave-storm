package ch.usi.inf.confidentialstorm;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.example.bolts.HistogramBolt;
import org.apache.storm.example.bolts.SplitSentenceBolt;
import org.apache.storm.example.bolts.WordCounterBolt;
import org.apache.storm.example.spouts.RandomJokeSpout;
import org.apache.storm.topology.ConfigurableTopology;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WordCountTopology extends ConfigurableTopology {
    private static final String PROD_SYSTEM_PROPERTY = "storm.prod";
    private static final String PROD_ENV_VAR = "STORM_PROD";

    public static void main(String[] args) {
        ConfigurableTopology.start(new WordCountTopology(), args);
    }

    @Override
    public int run(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();
        Logger LOG = LoggerFactory.getLogger(WordCountTopology.class);
    boolean isProd = isProdEnvironment(args);
    LOG.info("Starting WordCountTopology in {} mode", isProd ? "PROD" : "LOCAL");

        // WordSpout: stream of phrases from a book
        builder.setSpout("random-joke-spout", new RandomJokeSpout(), 2);
        // SplitSentenceBolt: splits each sentence into a stream of words
        builder.setBolt("sentence-split", new SplitSentenceBolt(), 3).shuffleGrouping("random-joke-spout");
        // WordCountBolt: counts the words that are emitted
        builder.setBolt("word-count", new WordCounterBolt(), 3).fieldsGrouping("sentence-split", new Fields("word"));
        // HistogramBolt: merges partial counters into a single (global) histogram
        builder.setBolt("histogram-global", new HistogramBolt(), 1).globalGrouping("word-count");

        // configure spout wait strategy to avoid starving other bolts
        // NOTE: learn more here https://storm.apache.org/releases/current/Performance.html
        conf.put(Config.TOPOLOGY_BACKPRESSURE_WAIT_STRATEGY,
                "org.apache.storm.policy.WaitStrategyProgressive");
        conf.put(Config.TOPOLOGY_BACKPRESSURE_WAIT_PROGRESSIVE_LEVEL1_COUNT, 1); // wait after 1 consecutive empty emit
        conf.put(Config.TOPOLOGY_BACKPRESSURE_WAIT_PROGRESSIVE_LEVEL2_COUNT, 100); // wait after 100 consecutive empty emits
        conf.put(Config.TOPOLOGY_BACKPRESSURE_WAIT_PROGRESSIVE_LEVEL3_SLEEP_MILLIS, 1);

        // run the topology (locally if not production, otherwise submit to nimbus)
        conf.setDebug(false);
        if (!isProd) {
            // submit topology
            LOG.warn("Running in local mode");
            try (LocalCluster cluster = new LocalCluster()) {
                cluster.submitTopology("WordCountTopology", conf, builder.createTopology());
                try {
                    Thread.sleep(60000);
                } catch (Exception exception) {
                    System.out.println("Thread interrupted exception : " + exception);
                    LOG.error("Thread interrupted exception : ", exception);
                }
                cluster.killTopology("WordCountTopology");
                return 0;
            } catch (Exception e) {
                return 1;
            }
        } else {
            // submit topology
            return submit("WordCountTopology", conf, builder);
        }
    }

  private boolean isProdEnvironment(String[] args) {
    if (args != null) {
      for (String arg : args) {
        if ("--prod".equalsIgnoreCase(arg) || "--production".equalsIgnoreCase(arg)) {
          return true;
        }
        if ("--local".equalsIgnoreCase(arg)) {
          return false;
        }
      }
    }
    String sysProp = System.getProperty(PROD_SYSTEM_PROPERTY);
    if (sysProp != null) {
      return Boolean.parseBoolean(sysProp);
    }
        String envVar = System.getenv(PROD_ENV_VAR);
        if (envVar != null) {
            return Boolean.parseBoolean(envVar);
        }
        return false;
    }
}
