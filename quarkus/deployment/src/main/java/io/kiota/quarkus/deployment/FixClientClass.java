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
        //        cu.addImport("jakarta.enterprise.context.ApplicationScoped");

        //        clientClass.addAnnotation("ApplicationScoped");

        //        cu.addImport("jakarta.enterprise.context.Dependent");
        //
        //        clientClass.addAnnotation("Dependent");

        cu.addImport("io.kiota.serialization.json.quarkus.JsonMapper");
        //        clientClass
        //                .addField("JsonSerializationWriterFactory",
        // "jsonSerializationWriterFactory")
        //                .addAnnotation("Inject");
        //        clientClass
        //                .addField("JsonParseNodeFactory", "jsonParseNodeFactory")
        //                .addAnnotation("Inject");
        clientClass.addField("JsonMapper", "mapper").addAnnotation("Inject");

        var statements = constructorBody.getStatements();

        for (int i = 0; i < statements.size(); i++) {
            var stmt = statements.get(i);
            if (stmt.toString().contains("JsonSerializationWriterFactory")) {
                constructorBody.setStatement(
                        i,
                        new ExpressionStmt(
                                new NameExpr(
                                        "SerializationWriterFactoryRegistry.defaultInstance.contentTypeAssociatedFactories.put(mapper.jsonSerializationWriterFactory().getValidContentType(),"
                                            + "  mapper.jsonSerializationWriterFactory())")));
            } else if (stmt.toString().contains("JsonParseNodeFactory")) {
                constructorBody.setStatement(
                        i,
                        new ExpressionStmt(
                                new NameExpr(
                                        "ParseNodeFactoryRegistry.defaultInstance.contentTypeAssociatedFactories.put(mapper.jsonParseNodeFactory().getValidContentType(),"
                                            + " mapper.jsonParseNodeFactory())")));
            }
        }

        source.saveAll();
    }
}
