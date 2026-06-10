/* ======================================
 * File: Parser.java
 * Date: 2026-06-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import static javax.xml.stream.XMLStreamConstants.*;

public class Parser {

    // this should prob be agraph
    private static final Map<String, Set<String>> states = Map.of(
            "project",               Set.of("project"),
            "parent",                Set.of("project"),
            "properties",            Set.of("project"),
            "dependencyManagement",  Set.of("project"),
            "dependencies",          Set.of("project", "dependencyManagement"),
            "dependency",            Set.of("dependencies")
            );

    public static void main(String[] args) throws Exception {
        Path path = Path.of(System.getProperty("user.home"), ".m2/repository/org/apache/maven/maven-parent/45/maven-parent-45.pom");

        Map<String, String> properties = new HashMap<>();
        List<Dependency> dependencies = new ArrayList<>();
        List<Dependency> depMgmt = new ArrayList<>();
        Dependency parent = null;

        XMLInputFactory f = XMLInputFactory.newInstance();
        XMLStreamReader reader = f.createXMLStreamReader(Files.newInputStream(path));

        Stack<String> stack = new Stack<>();
        stack.push("placeholder");

        Stack<Map<String, String>> data = new Stack<>();
        List<Dependency> deps = new ArrayList<>();
        String currentTag = null;
        boolean save = false;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case START_ELEMENT:
                    String name = reader.getLocalName();
                    currentTag = name;

                    Set<String> validParents = states.get(name);
                    String stackTop = stack.peek();
                    stack.push(name);

                    if (validParents == null || !validParents.contains(stackTop)) 
                        break;

                    data.push(new HashMap<>());
                    save = true;

                    break;
                case CHARACTERS:
                    if (!save) break;

                    String text = reader.getText().trim();
                    if (text.isEmpty()) break;

                    data.peek().put(currentTag, text);

                    break;
                case END_ELEMENT:
                    String end = stack.pop();
                    if (!states.containsKey(end)) break;
                    if (!states.get(end).contains(stack.peek())) break;

                    Map<String, String> d = data.pop();
                    save = false;

                    switch (end) {
                        case "parent":
                            parent = new Dependency(d.get("groupId"), d.get("artifactId"), d.get("version"));
                            break;
                        case "properties":
                            properties = new HashMap<>(d);
                            break;
                        case "dependency":
                            String optional = d.get("optional");
                            if (optional != null && optional.equals("true")) 
                                break;

                            String scope = d.get("scope");
                            if (scope != null && !scope.equals("compile"))
                                break;

                            deps.add(new Dependency(d.get("groupId"), d.get("artifactId"), d.get("version")));
                            break;
                        case "dependencies":
                            if (stack.peek().equals("dependencyManagement")) {
                                stack.pop();
                                depMgmt = deps;
                            } else {
                                dependencies = deps;
                            }
                            deps = new ArrayList<>();
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
        return PomData(parent, properties, deps, depMgmt);
    }
    record Dependency(String group, String artifact, String version) {}
    record PomData(Dependency parent, Map<String, String> properties, List<Dependency> deps, List<Dependency> managements) {}
}

