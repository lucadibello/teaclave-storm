package ch.usi.inf.confidentialstorm.bolts;

import org.apache.storm.Config;
import org.apache.storm.Constants;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistogramBolt extends BaseRichBolt {
    private OutputCollector collector;
    private Map<String, Long> hist;
    private Logger LOG;
    private ExecutorService io;
    private final String OUTPUT_FILE = "data/histogram.txt";

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.hist = new HashMap<>();
        this.io = Executors.newSingleThreadExecutor();
        this.LOG = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 5); // update histogram every second
        return config;
    }

    private boolean isTickTuple(Tuple tuple) {
        return tuple.getSourceComponent().equals(Constants.SYSTEM_COMPONENT_ID)
                && tuple.getSourceStreamId().equals(Constants.SYSTEM_TICK_STREAM_ID);
    }

    private void writeSnapshot(Map<String, Long> snap) {
        File file = new File(OUTPUT_FILE);
        // Ensure the output directory exists
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs() && !parent.exists()) {
                LOG.error("Could not create output directory: {}", parent.getAbsolutePath());
                return;
            }
        }
        try {
            // Create the file if it does not exist
            if (!file.exists() && !file.createNewFile()) {
                LOG.error("Could not create output file: {}", file.getAbsolutePath());
                return;
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(file, false))) {
                out.println("=== " + Instant.now() + " ===");
                snap.forEach((k, v) -> out.println(k + ":" + v));
            }
        } catch (IOException e) {
            LOG.error("Error while exporting histogram snapshot:", e);
        }
    }

    @Override
    public void execute(Tuple input) {
        if (isTickTuple(input)) {
            LOG.info("[HistogramBolt] Received tick tuple. Exporting histogram snapshot with {} entries.", hist.size());
            io.submit(() -> writeSnapshot(new HashMap<>(hist)));
            return;
        }
        String word = input.getStringByField("word");
        Long newCount = input.getLongByField("count");
        // update the histogram!
        hist.put(word, newCount);
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // NOTE: not output. this is a sink!
    }

    @Override
    public void cleanup() {
        io.shutdown();
        super.cleanup();
    }
}
