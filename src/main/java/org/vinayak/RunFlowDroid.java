package org.vinayak;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.options.Options;

public class RunFlowDroid {

    static class SimpleSourceSinkManager extends BaseSourceSinkManager {
        public SimpleSourceSinkManager(
                Collection<? extends ISourceSinkDefinition> sources,
                Collection<? extends ISourceSinkDefinition> sinks,
                InfoflowConfiguration config) {
            super(sources, sinks, config);
        }

        @Override
        protected boolean isEntryPointMethod(soot.SootMethod method) {
            return false;
        }
    }

    private static String[] parseParamTypes(String methodSignature) {
        Pattern p = Pattern.compile("<(.+?): (.+?) (.+?)\\((.*?)\\)>");
        Matcher m = p.matcher(methodSignature);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid method signature: " + methodSignature);
        }
        String params = m.group(4);
        return params.isEmpty() ? new String[0] : params.split(",");
    }

    public static InfoflowResults flowDroidExceptionAnalysis(
            String MethodSignature,
            String LibraryJarPath,
            List<String> sinkSignatures) {

        String driverJavaPath = ConfigLoader.getProperty("driver_java_path");
        String driverClassFolder = ConfigLoader.getProperty("driver_class_folder");
        String xmlPath = ConfigLoader.getProperty("xml_path");

        String[] paramTypes = parseParamTypes(MethodSignature);

        DriverStubGenerator.generateDriverStub(MethodSignature, driverJavaPath, LibraryJarPath);
        System.out.println("Generated DriverStub.java");

        XMLGenerator.generateSourcesAndSinksXML(paramTypes, sinkSignatures, xmlPath);
        System.out.println("Generated SourcesAndSinks.xml");

        boolean compileSuccess = JavaCompilerUtil.compileJavaFile(driverJavaPath, driverClassFolder, LibraryJarPath);
        if (!compileSuccess) {
            System.out.println("Failed to compile DriverStub.java");
            return null;
        }
        System.out.println("Compiled DriverStub.class");

        Infoflow infoflow = new Infoflow();

        XMLSourceSinkParser parser;
        try {
            parser = XMLSourceSinkParser.fromFile(xmlPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse XML file: " + xmlPath, e);
        }
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(),
                infoflow.getConfig());

        DefaultEntryPointCreator entryCreator = new DefaultEntryPointCreator(
                Collections.singletonList("<DriverStub: void run()>"));

        infoflow
                .getConfig()
                .setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows);
        infoflow.getConfig().setInspectSinks(false);
        infoflow.getConfig().setOneSourceAtATime(false);
        infoflow.getConfig().setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.RTA);
        infoflow
                .getConfig()
                .setStaticFieldTrackingMode(
                        InfoflowConfiguration.StaticFieldTrackingMode.ContextFlowSensitive);

        infoflow.setSootConfig(
                new IInfoflowConfig() {
                    @Override
                    public void setSootOptions(Options options, InfoflowConfiguration config) {
                        options.set_prepend_classpath(true);
                        options.set_allow_phantom_refs(true);
                    }
                });

        infoflow.computeInfoflow(
                driverClassFolder,
                LibraryJarPath, entryCreator, ssm);

        InfoflowResults results = infoflow.getResults();
        return results;
    }
}
