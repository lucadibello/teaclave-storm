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

import java.util.Locale;
import java.util.Map;

public class SplitSentenceBolt extends BaseRichBolt {
    private OutputCollector collector;
    private int boltId;
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public void prepare(Map<String, Object> topoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.boltId = context.getThisTaskId();
    }

    @Override
    public void execute(Tuple input) {
        // we get a sentence from the input tuple -> we split it int words
        String jokeBody = input.getStringByField("body");
        String[] words = jokeBody.split("\\W+");
        LOG.info("[SplitSentenceBolt {}]: Found {} words in sentence.", boltId, words.length);
        for (String word : words) {
            word = word.toLowerCase(Locale.ROOT).trim();
            if (!word.isEmpty()) {
                collector.emit(input, new Values(word));
            }
        }
        // acknowledge that the input has been consumed correctly!
        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word"));
    }
}