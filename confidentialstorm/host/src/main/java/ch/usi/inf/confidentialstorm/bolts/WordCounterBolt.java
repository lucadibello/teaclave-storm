package ch.usi.inf.confidentialstorm.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WordCounterBolt extends BaseRichBolt {
    private OutputCollector collector;
    private Map<String, Long> counter;
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.counter = new HashMap<>();
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        String word = input.getStringByField("word");
        // update hashmap + emit current count
        long count = counter.merge(word, 1L, Long::sum);
        collector.emit(input, new Values(word, count));
        collector.ack(input);
        LOG.info("[WordCounterBolt] Word: {} Current count: {}", word, count);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word", "count"));
    }
}
