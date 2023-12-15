package io.kiota.quarkus.deployment;

import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

// This is a workaround until this Issue gets fixed: https://github.com/microsoft/kiota/issues/3796
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

        cu.addImport("io.quarkus.arc.Arc");
        var statements = constructorBody.getStatements();

        for (int i = 0; i < statements.size(); i++) {
            var stmt = statements.get(i);

            // Fix up the instantiation of the factories using Arc
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
            }
        }

        // right after the `super` call
        constructorBody.addStatement(
                1,
                new ExpressionStmt(
                        new NameExpr(
                                "JsonSerializationWriterFactory jsonSerializationWriterFactory ="
                                    + " Arc.container().instance(JsonSerializationWriterFactory.class).get()")));
        constructorBody.addStatement(
                2,
                new ExpressionStmt(
                        new NameExpr(
                                "JsonParseNodeFactory jsonParseNodeFactory ="
                                    + " Arc.container().instance(JsonParseNodeFactory.class).get()")));

        source.saveAll();
    }
}
