package io.kiota.quarkus.deployment;

import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class FixClientClass {

    private final String clientName;
    private final String packageName;
    private final Path generatedSourceFolder;

    public FixClientClass(String clientName, String packageName, Path generatedSourceFolder) {
        this.clientName = clientName;
        this.packageName = packageName;
        this.generatedSourceFolder = generatedSourceFolder;
    }

    public void fix() throws IOException {
        final SourceRoot source = new SourceRoot(generatedSourceFolder);
        final SourceRoot dest = new SourceRoot(generatedSourceFolder);

        var parsed = source.tryToParse(packageName, clientName + ".java");

        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            throw new RuntimeException(
                    "Found issues while parsing the Client library, problems:\n"
                            + parsed.getProblems().stream()
                                    .map(p -> p.getVerboseMessage())
                                    .collect(Collectors.joining("\n")));
        }

        var cu = parsed.getResult().get();
        var clientClass = parsed.getResult().get().getClassByName(clientName).get();
        var constructorBody = clientClass.getConstructors().get(0).getBody();

        cu.addImport("com.microsoft.kiota.serialization.SerializationWriterFactoryRegistry");
        cu.addImport("com.microsoft.kiota.serialization.ParseNodeFactoryRegistry");
        cu.addImport("jakarta.inject.Inject");
        cu.addImport("com.fasterxml.jackson.databind.ObjectMapper");

        // Add the object mapper to the contructor arguments
        clientClass.getConstructors().get(0).addParameter("ObjectMapper", "mapper");

        var statements = constructorBody.getStatements();

        for (int i = 0; i < statements.size(); i++) {
            var stmt = statements.get(i);

            // Fix up the reflective instantiation
            if (stmt.toString().contains("JsonSerializationWriterFactory")) {
                constructorBody.setStatement(
                        i,
                        new ExpressionStmt(
                                new NameExpr(
                                        "SerializationWriterFactoryRegistry.defaultInstance.contentTypeAssociatedFactories.put(jsonSerializationWriterFactory.getValidContentType(),"
                                            + "  jsonSerializationWriterFactory)")));
            } else if (stmt.toString().contains("JsonParseNodeFactory")) {
                constructorBody.setStatement(
                        i,
                        new ExpressionStmt(
                                new NameExpr(
                                        "ParseNodeFactoryRegistry.defaultInstance.contentTypeAssociatedFactories.put(jsonParseNodeFactory.getValidContentType(),"
                                            + " jsonParseNodeFactory)")));
            } else if (stmt.toString().contains("registerDefaultSerializer")) {
                // TODO: we need to avoid reflection instantiating those
            }
        }

        // right after the `super` call
        constructorBody.addStatement(
                1,
                new ExpressionStmt(
                        new NameExpr(
                                "JsonSerializationWriterFactory jsonSerializationWriterFactory ="
                                        + " new JsonSerializationWriterFactory(mapper)")));
        constructorBody.addStatement(
                2,
                new ExpressionStmt(
                        new NameExpr(
                                "JsonParseNodeFactory jsonParseNodeFactory = new"
                                        + " JsonParseNodeFactory(mapper)")));

        source.saveAll();
    }
}
