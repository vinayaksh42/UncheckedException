package org.vinayak;

import com.google.common.collect.ImmutableList;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.typehierarchy.ViewTypeHierarchy;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class Main {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println(
          "Usage: java -jar unexpectedException-1.0-SNAPSHOT.jar <path-to-JAR> <library-name> <library/client>");
      System.exit(1);
    }
    String pathToJAR = args[0];
    String libraryName = args[1];
    boolean isLibrary = args[2].contains("library");
    if (isLibrary) {
      callgraphBasedLibraryAnalysis(pathToJAR, libraryName);
    } else {
      analyzeClientJAR(pathToJAR, libraryName);
    }
  }

  public static void analyzeClientJAR(String pathToJAR, String clientName) {
    JSONArray classArray = new JSONArray();

    Path path = Paths.get(pathToJAR);
    AnalysisInputLocation inputLocation =
        PathBasedAnalysisInputLocation.create(path, SourceType.Application);
    View view = new JavaView(inputLocation);

    for (SootClass sootClass : view.getClasses()) {
      JSONArray methodsArray = new JSONArray();

      for (SootMethod method : sootClass.getMethods()) {
        List<String> externalMethodCalls = new ArrayList<>();

        if (method.isAbstract() || method.isNative()) {
          continue;
        }

        Body body = method.getBody();

        List<Stmt> stmts = body.getStmts();
        StmtClientVisitor stmtVisitor = new StmtClientVisitor(view, externalMethodCalls);

        for (Stmt stmt : stmts) {
          stmt.accept(stmtVisitor);
        }

        JSONObject methodObject = new JSONObject();
        methodObject.put("methodSignature", method.getSignature());
        methodObject.put("external_method_calls", externalMethodCalls.toArray());
        methodsArray.put(methodObject);
      }
      classArray.put(new JSONObject().put(sootClass.getName(), methodsArray));
    }

    try (FileWriter file = new FileWriter("results/" + clientName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void analyzeLibraryJAR(String pathToJAR, String libraryName) {
    JSONArray classArray = new JSONArray();

    String jarFile = pathToJAR;
    String rtJarFile = "resources/rt.jar";

    AnalysisInputLocation inputlocationJARToAnalyze =
        new JavaClassPathAnalysisInputLocation(jarFile, SourceType.Application);
    AnalysisInputLocation inputlocationRTJAR =
        new JavaClassPathAnalysisInputLocation(rtJarFile, SourceType.Library);

    List<AnalysisInputLocation> inputLocations =
        ImmutableList.of(inputlocationJARToAnalyze, inputlocationRTJAR);

    JavaView view = new JavaView(inputLocations);

    TypeHierarchy typehierarchy = view.getTypeHierarchy();

    for (SootClass sootClass : view.getClasses()) {
      JSONArray methodsArray = new JSONArray();

      if (sootClass.isLibraryClass() == true) {
        continue;
      }

      for (SootMethod method : sootClass.getMethods()) {
        List<ClassType> uncheckedExceptions = new ArrayList<>();
        List<String> internalMethodCalls = new ArrayList<>();
        List<String> externalMethodCalls = new ArrayList<>();

        if (method.isAbstract() || method.isNative()) {
          continue;
        }

        Body body = method.getBody();

        Body.BodyBuilder bodyBuilder = Body.builder(body, Collections.emptySet());

        List<Stmt> stmts = body.getStmts();
        StmtVisitor stmtVisitor =
            new StmtVisitor(
                typehierarchy,
                view,
                bodyBuilder,
                uncheckedExceptions,
                internalMethodCalls,
                externalMethodCalls);
        for (Stmt stmt : stmts) {
          stmt.accept(stmtVisitor);
        }

        JSONObject methodObject = new JSONObject();
        methodObject.put("methodSignature", method.getSignature());
        methodObject.put(
            "unchecked_exceptions",
            uncheckedExceptions.stream().map(ClassType::toString).toArray());
        methodObject.put("internal_method_calls", internalMethodCalls.toArray());
        methodObject.put("external_method_calls", externalMethodCalls.toArray());
        methodsArray.put(methodObject);
      }
      classArray.put(new JSONObject().put(sootClass.getName(), methodsArray));
    }

    try (FileWriter file = new FileWriter("results/" + libraryName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void callgraphBasedLibraryAnalysis(String pathToJAR, String libraryName) {
    JSONArray classArray = new JSONArray();

    String jarFile = pathToJAR;
    String rtJarFile = "resources/rt.jar";
    String javaxCrypto = "resources/javax.crypto-1.0.2.jar";

    AnalysisInputLocation inputlocationJARToAnalyze =
        new JavaClassPathAnalysisInputLocation(jarFile, SourceType.Application);
    AnalysisInputLocation inputlocationRTJAR =
        new JavaClassPathAnalysisInputLocation(rtJarFile, SourceType.Library);
    AnalysisInputLocation inputlocationJavaxCrypto =
        new JavaClassPathAnalysisInputLocation(javaxCrypto, SourceType.Library);

    List<AnalysisInputLocation> inputLocations =
        ImmutableList.of(inputlocationJARToAnalyze, inputlocationRTJAR, inputlocationJavaxCrypto);

    JavaView view = new JavaView(inputLocations);

    for (SootClass sootClass : view.getClasses()) {
      JSONArray methodsArray = new JSONArray();

      if (sootClass.isLibraryClass() == true) {
        continue;
      }

      for (SootMethod method : sootClass.getMethods()) {
        List<ClassType> uncheckedExceptions = new ArrayList<>();

        if (method.isAbstract() || method.isNative()) {
          continue;
        }

        // Create type hierarchy and CHA
        final ViewTypeHierarchy typeHierarchy = new ViewTypeHierarchy(view);
        CallGraphAlgorithm cha = new ClassHierarchyAnalysisAlgorithm(view);

        // Create CG by initializing CHA with entry method(s)
        MethodSignature entryMethodSignature = method.getSignature();
        CallGraph cg = cha.initialize(Collections.singletonList(entryMethodSignature));

        for (MethodSignature methodSignature : cg.getMethodSignatures()) {
          Optional<JavaSootMethod> calledMethod = view.getMethod(methodSignature);
          // iterate over all the methods in calledMethod
          if (calledMethod.isPresent()) {
            JavaSootMethod sootMethod = calledMethod.get();
            if (sootMethod.isAbstract() || sootMethod.isNative()) {
              continue;
            }
            Body body = sootMethod.getBody();

            Body.BodyBuilder bodyBuilder = Body.builder(body, Collections.emptySet());

            List<Stmt> stmts = body.getStmts();
            StmtCallGraphVisitor stmtVisitor =
                new StmtCallGraphVisitor(typeHierarchy, bodyBuilder, uncheckedExceptions);
            for (Stmt stmt : stmts) {
              stmt.accept(stmtVisitor);
            }
          }
        }

        JSONObject methodObject = new JSONObject();
        methodObject.put("methodSignature", method.getSignature());
        methodObject.put(
            "unchecked_exceptions",
            uncheckedExceptions.stream().map(ClassType::toString).toArray());
        methodsArray.put(methodObject);
      }
      classArray.put(new JSONObject().put(sootClass.getName(), methodsArray));
    }

    try (FileWriter file = new FileWriter("results/" + libraryName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
