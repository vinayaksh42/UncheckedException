package org.vinayak;

import com.google.common.collect.ImmutableList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

public class Main {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println(
          "Usage: java -jar unexpectedException-1.0-SNAPSHOT.jar <path-to-JAR> <library-name>");
      System.exit(1);
    }
    String pathToJAR = args[0];
    String libraryName = args[1];
    analyzeJAR(pathToJAR, libraryName);
  }

  public static void analyzeJAR(String pathToJAR, String libraryName) {
    JSONArray classArray = new JSONArray();

    String jarFile = pathToJAR;
    String rtJarFile = "resources/rt.jar";

    AnalysisInputLocation inputlocationJARToAnalyze = new JavaClassPathAnalysisInputLocation(jarFile,
        SourceType.Application);
    AnalysisInputLocation inputlocationRTJAR = new JavaClassPathAnalysisInputLocation(rtJarFile, SourceType.Library);

    List<AnalysisInputLocation> inputLocations = ImmutableList.of(inputlocationJARToAnalyze, inputlocationRTJAR);

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
        // using the visitor for the stmts
        StmtVisitor stmtVisitor = new StmtVisitor(
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

    try (FileWriter file = new FileWriter(libraryName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
