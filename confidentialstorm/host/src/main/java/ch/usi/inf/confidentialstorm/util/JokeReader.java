package ch.usi.inf.confidentialstorm.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class JokeReader {

    public static final class Joke {
        @JsonProperty("body")
        public String body;

        @JsonProperty("category")
        public String category;

        @JsonProperty("id")
        public int id;

        @JsonProperty("rating")
        public double rating;

        @Override
        public String toString() {
            return "Joke{id=" + id + ", category='" + category + "', rating=" + rating + "}";
        }
    }

    private final ObjectMapper mapper;

    public JokeReader() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Reads all objects from a JSON file that is either:
     * - a JSON array: [ {..}, {..}, ... ]
     * - or NDJSON (JSON Lines): one object per line
     */
    public List<Joke> readAll(String jsonResourceName) throws IOException {
        // Read from resources
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classloader.getResourceAsStream(jsonResourceName);
        if (resourceStream == null) {
            throw new FileNotFoundException("Resource not found: " + jsonResourceName);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            // Peek first non-whitespace char to decide format
            br.mark(8192);
            int ch;
            do {
                ch = br.read();
            } while (ch != -1 && Character.isWhitespace(ch));
            br.reset();

            // read array
            return mapper.readValue(br, new TypeReference<List<Joke>>() {
            });
        }
    }

    // Tiny demo
    public static void main(String[] args) throws Exception {
        JokeReader reader = new JokeReader();
        List<Joke> jokes = reader.readAll( "jokes.json");
        System.out.println("Loaded " + jokes.size() + " jokes");
        // Print first one (optional)
        if (!jokes.isEmpty()) {
            System.out.println(jokes.get(0));
        }
    }
}
