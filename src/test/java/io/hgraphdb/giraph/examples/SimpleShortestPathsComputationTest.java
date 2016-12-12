package io.hgraphdb.giraph.examples;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.hgraphdb.HBaseGraphConfiguration;
import io.hgraphdb.HBaseGraphTest;
import io.hgraphdb.giraph.HBaseEdgeInputFormat;
import io.hgraphdb.giraph.HBaseVertexInputFormat;
import io.hgraphdb.giraph.InternalHBaseVertexRunner;
import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static io.hgraphdb.giraph.examples.SimpleShortestPathsComputation.SOURCE_ID;
import static org.junit.Assert.*;

/**
 * Contains a simple unit test for {@link SimpleShortestPathsComputation}
 */
public class SimpleShortestPathsComputationTest extends HBaseGraphTest {

    /**
     * A local integration test on toy data
     */
    @Ignore
    @Test
    public void testToyData() throws Exception {

        // a small four vertex graph
        String[] graphx = new String[]{
                "1 2:1.0 3:3.0",
                "2 3:1.0 4:10.0",
                "3 4:2.0",
                "4"
        };

        Vertex v1 = graph.addVertex(T.id, 1);
        Vertex v2 = graph.addVertex(T.id, 2);
        Vertex v3 = graph.addVertex(T.id, 3);
        Vertex v4 = graph.addVertex(T.id, 4);
        v1.addEdge("e", v2, "weight", 1.0);
        v1.addEdge("e", v3, "weight", 3.0);
        v2.addEdge("e", v3, "weight", 1.0);
        v2.addEdge("e", v4, "weight", 10.0);
        v3.addEdge("e", v4, "weight", 2.0);

        HBaseGraphConfiguration hconf = graph.configuration();
        GiraphConfiguration conf = new GiraphConfiguration(hconf.toHBaseConfiguration());
        // start from vertex 1
        SOURCE_ID.set(conf, 1);
        conf.setComputationClass(SimpleShortestPathsComputation.class);
        conf.setEdgeInputFormatClass(HBaseEdgeInputFormat.class);
        conf.setVertexInputFormatClass(HBaseVertexInputFormat.class);
        conf.setVertexOutputFormatClass(VertexWithDoubleValueNullEdgeTextOutputFormat.class);

        // run internally
        Iterable<String> results = InternalHBaseVertexRunner.run(conf);

        Map<Long, Double> distances = parseDistances(results);

        // verify results
        assertNotNull(distances);
        assertEquals(4, distances.size());
        assertEquals(0.0, distances.get(1L), 0d);
        assertEquals(1.0, distances.get(2L), 0d);
        assertEquals(2.0, distances.get(3L), 0d);
        assertEquals(4.0, distances.get(4L), 0d);
    }

    private Map<Long, Double> parseDistances(Iterable<String> results) {
        Map<Long, Double> distances =
                Maps.newHashMapWithExpectedSize(Iterables.size(results));

        Pattern separator = Pattern.compile("[\t]");

        for (String line : results) {
            String[] tokens = separator.split(line);
            distances.put(Long.parseLong(tokens[0]), Double.parseDouble(tokens[1]));
        }
        return distances;
    }
}
