import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class JSONDynamicTest {

    @TestFactory
    Stream<DynamicContainer> runTests() throws IOException {
        List<DynamicContainer> dynamicContainers = new ArrayList<>();
        List<DynamicTest> dynamicTests = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new File("/Users/avinash/Repos/junit_scenario/src/main/resources/weblogic.simple.json"));
        Iterator<JsonNode> testScenario = node.findValue("test_scenario").elements();
        List<String> scenarios = new ArrayList<>();
        while (testScenario.hasNext()) {
            scenarios.add(testScenario.next().toPrettyString());
        }

        scenarios.stream().forEach(s -> {
            System.out.println("Test Scenario " + s);
            String test_scenario = null;
            JsonNode test_scenario_Node = null;
            try {
                test_scenario_Node = mapper.readTree(s);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            test_scenario = test_scenario_Node.get("name").asText();
            test_scenario_Node.get("socket_endpoint").elements().forEachRemaining(socket_endpoint -> {
                System.out.println(socket_endpoint);
                final String hostname = socket_endpoint.get("host").asText();
                final String proto = socket_endpoint.get("proto").asText();
                final String port = socket_endpoint.get("port").asText();
                final String request = socket_endpoint.get("request").asText();
                final String expects = socket_endpoint.get("expects").asText();
                Executable executable = () -> {
                    String output = null;
                    try {
                        output = SocketUtils.testSSL(hostname, Integer.parseInt(port), false, request);
                        System.out.println(output);
                        Assertions.assertTrue(output.contains(expects), "Contains " + expects);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assertions.fail("Error " + e.getMessage());
                    }
                };
                DynamicTest dynamicTest = DynamicTest.dynamicTest(hostname + ":" + port + "->" + proto, executable);
                dynamicTests.add(dynamicTest);
            });


            dynamicContainers.add(DynamicContainer.dynamicContainer(test_scenario, dynamicTests));


        });
        return dynamicContainers.stream();
    }
}

