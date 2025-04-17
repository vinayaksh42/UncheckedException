package org.vinayak;

import com.google.common.collect.ImmutableList;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sootup.callgraph.CallGraph;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
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
import sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class Main {
  static final String JAVA_LIBRARY_PATH =
      "/Users/vinayaksh42/Desktop/Research/BBC Research/unexpectedException/resources/rt.jar";
  static final String JAVA_JCE_PATH =
      "/Users/vinayaksh42/Desktop/Research/BBC Research/unexpectedException/resources/jce.jar";

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println(
          "Specify mode: analyzeLibraryMethods | analyzeClient | callgraphBasedLibraryAnalysis");
      System.exit(1);
    }

    String mode = args[0];

    switch (mode) {
      case "analyzeLibraryMethods":
        analyzeLibraryMethods(Arrays.copyOfRange(args, 1, args.length));
        break;

      case "analyzeClient":
        analyzeClient(Arrays.copyOfRange(args, 1, args.length));
        break;

      case "callgraphBasedLibraryAnalysis":
        if (args.length < 4) {
          System.err.println(
              "Usage: callgraphBasedLibraryAnalysis <path-to-JAR> <library-name> <MatchedMethods.json File> <additionalJars>");
          System.exit(1);
        }
        callgraphBasedLibraryAnalysis(
            args[1], args[2], Arrays.asList(Arrays.copyOfRange(args, 4, args.length)), args[3]);
        break;

      default:
        System.err.println("Unknown mode: " + mode);
        System.exit(1);
    }
  }

  public static void analyzeClient(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: analyzeClient <path-to-JAR> <client-name>");
      System.exit(1);
    }
    analyzeClientJAR(args[0], args[1]);
  }

  public static void analyzeLibraryMethods(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: analyzeLibraryMethods <path-to-JAR> <library-name>");
      System.exit(1);
    }
    recordMethodSignaturesForJar(args[0], args[1]);
  }

  public static void recordMethodSignaturesForJar(String pathToJAR, String libraryName) {
    Path path = Paths.get(pathToJAR);
    AnalysisInputLocation inputLocation =
        PathBasedAnalysisInputLocation.create(path, SourceType.Application);
    View view = new JavaView(inputLocation);
    JSONArray methodNameArray = new JSONArray();
    for (SootClass sootClass : view.getClasses()) {
      // save the package name in json
      for (SootMethod method : sootClass.getMethods()) {
        methodNameArray.put(method.getSignature());
      }
    }
    try (FileWriter file = new FileWriter("../client/temp/" + libraryName + ".json")) {
      file.write(methodNameArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
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

    try (FileWriter file = new FileWriter("../client/client_results/" + clientName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void analyzeLibraryJAR(String pathToJAR, String libraryName) {
    JSONArray classArray = new JSONArray();

    String jarFile = pathToJAR;
    String rtJarFile = JAVA_LIBRARY_PATH;

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
        if (method
            .getSignature()
            .toString()
            .contains(
                "<com.esotericsoftware.kryo.serializers.ClosureSerializer: java.lang.invoke.SerializedLambda toSerializedLambda(java.lang.Object)>")) {
          System.out.println("Method: " + method.getSignature());
        }
        Body body;
        try {
          body = method.getBody();
        } catch (Exception e) {
          continue;
        }

        Body.BodyBuilder bodyBuilder = Body.builder(body, Collections.emptySet());
        if (method
            .getSignature()
            .toString()
            .contains(
                "<com.esotericsoftware.kryo.serializers.ClosureSerializer: java.lang.invoke.SerializedLambda toSerializedLambda(java.lang.Object)>")) {
          System.out.println("Method: " + method.getSignature());
          System.out.println("-----------------------------------------");

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
            System.out.println(stmt);
            stmt.accept(stmtVisitor);
          }
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

  // public static void methodBodyAnalysis(String pathToJAR) {
  // Path path = Paths.get(pathToJAR);
  // AnalysisInputLocation inputLocation =
  // PathBasedAnalysisInputLocation.create(path, SourceType.Application);
  // View view = new JavaView(inputLocation);
  // final ViewTypeHierarchy typeHierarchy = new ViewTypeHierarchy(view);
  // for (SootClass sootClass : view.getClasses()) {
  // List<ClassType> uncheckedExceptions = new ArrayList<>();
  // for (SootMethod method : sootClass.getMethods()) {
  // if (method.isAbstract() || method.isNative()) {
  // continue;
  // }
  // if (method
  // .getSignature()
  // .toString()
  // .contains(
  // "<com.esotericsoftware.kryo.serializers.ClosureSerializer:
  // java.lang.invoke.SerializedLambda toSerializedLambda(java.lang.Object)>")) {
  // Body body = method.getBody();
  // Body.BodyBuilder bodyBuilder = Body.builder(body, Collections.emptySet());
  // System.out.println("Method: " + method.getSignature());
  // List<Stmt> stmts = body.getStmts();
  // StmtCallGraphVisitor stmtVisitor = new StmtCallGraphVisitor(typeHierarchy,
  // bodyBuilder, uncheckedExceptions);
  // for (Stmt stmt : stmts) {
  // System.out.println(stmt);
  // stmt.accept(stmtVisitor);
  // }
  // System.out.println("-----------------------------------------");
  // }
  // }
  // }
  // }

  public static void printCallGraphForMethod(String pathToJAR, String MethodToSearch) {
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JavaClassPathAnalysisInputLocation(pathToJAR, SourceType.Application));

    JavaView view = new JavaView(inputLocations);

    for (SootClass sootClass : view.getClasses()) {
      if (sootClass.isLibraryClass() == true) {
        continue;
      }

      for (SootMethod method : sootClass.getMethods()) {
        List<String> uncheckedExceptions = new ArrayList<>();

        // check if method is part of matchedMethodsList
        if (!MethodToSearch.equalsIgnoreCase(method.getSignature().toString())
            || method.isAbstract()
            || method.isNative()) {
          continue;
        }

        // Create type hierarchy and CHA
        RapidTypeAnalysisAlgorithm cha = new RapidTypeAnalysisAlgorithm(view);

        // Create CG by initializing CHA with entry method(s)
        MethodSignature entryMethodSignature = method.getSignature();

        CallGraph cg;
        try {
          cg = cha.initialize(Collections.singletonList(entryMethodSignature));
        } catch (Exception e) {
          continue;
        }

        System.out.println("CallGraph for " + MethodToSearch + ": " + cg.exportAsDot());
      }
    }
  }

  public static void callgraphBasedLibraryAnalysis(
      String pathToJAR, String library, String MatchedMethods) {
    List<String> additonalJars = new ArrayList<>();
    callgraphBasedLibraryAnalysis(pathToJAR, library, additonalJars, MatchedMethods);
  }

  public static void callgraphBasedLibraryAnalysis(
      String pathToJAR, String libraryName, List<String> additionalJars, String MatchedMethods) {
    JSONArray classArray = new JSONArray();

    // Read the JSON file and store the contents of the array in a list of strings
    List<String> matchedMethodsList = new ArrayList<>();
    try {
      String content =
          new String(Files.readAllBytes(Paths.get(MatchedMethods)), StandardCharsets.UTF_8);
      JSONArray matchedMethodsArray = new JSONArray(content);
      for (int i = 0; i < matchedMethodsArray.length(); i++) {
        matchedMethodsList.add(matchedMethodsArray.getString(i));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      System.err.println("Error parsing JSON content: " + e.getMessage());
    }

    AnalysisInputLocation inputlocationJARToAnalyze =
        new JavaClassPathAnalysisInputLocation(pathToJAR, SourceType.Application);

    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(inputlocationJARToAnalyze);
    inputLocations.add(new JrtFileSystemAnalysisInputLocation(SourceType.Library));

    // for (String extraJar : additionalJars) {
    // inputLocations.add(new JavaClassPathAnalysisInputLocation(extraJar,
    // SourceType.Library));
    // }

    JavaView view = new JavaView(inputLocations);
    View viewJar = new JavaView(inputlocationJARToAnalyze);

    for (SootClass sootClass : viewJar.getClasses()) {
      JSONArray methodsArray = new JSONArray();

      if (sootClass.isLibraryClass() == true) {
        continue;
      }

      for (SootMethod method : sootClass.getMethods()) {
        List<String> uncheckedExceptions = new ArrayList<>();

        // check if method is part of matchedMethodsList
        if (!matchedMethodsList.contains(method.getSignature().toString())) {
          continue;
        }

        if (method.isAbstract() || method.isNative()) {
          continue;
        }

        try {
          Body bodychecker = method.getBody();
        } catch (Exception e) {
          continue;
        }

        // Create type hierarchy and RTA
        final ViewTypeHierarchy typeHierarchy = new ViewTypeHierarchy(view);
        RapidTypeAnalysisAlgorithm rta = new RapidTypeAnalysisAlgorithm(viewJar);

        // Create CG by initializing CHA with entry method(s)
        MethodSignature entryMethodSignature = method.getSignature();

        CallGraph cg;
        try {
          cg = rta.initialize(Collections.singletonList(entryMethodSignature));
        } catch (Exception e) {
          continue;
        }

        for (MethodSignature methodSignature : cg.getMethodSignatures()) {
          Optional<? extends SootMethod> calledMethod = viewJar.getMethod(methodSignature);
          Optional<JavaSootMethod> actualMethod = view.getMethod(methodSignature);
          // iterate over all the methods in calledMethod
          if (calledMethod.isPresent()) {
            SootMethod sootMethod = calledMethod.get();
            if (sootMethod.isAbstract() || sootMethod.isNative()) {
              continue;
            }
            if (actualMethod.isPresent()) {
              SootMethod methodToCheck = actualMethod.get();
              if (methodToCheck.isAbstract() || methodToCheck.isNative()) {
                continue;
              }

              Body body;

              try {
                body = methodToCheck.getBody();
              } catch (Exception e) {
                continue;
              }

              Body.BodyBuilder bodyBuilder = Body.builder(body, Collections.emptySet());

              List<Stmt> stmts = body.getStmts();
              StmtCallGraphVisitor stmtVisitor =
                  new StmtCallGraphVisitor(
                      typeHierarchy, bodyBuilder, uncheckedExceptions, methodSignature.toString());
              for (Stmt stmt : stmts) {
                stmt.accept(stmtVisitor);
              }
            }
          }
        }

        JSONObject methodObject = new JSONObject();
        methodObject.put("methodSignature", method.getSignature());
        methodObject.put("unchecked_exceptions", uncheckedExceptions.stream().toArray());
        methodsArray.put(methodObject);
      }
      classArray.put(new JSONObject().put(sootClass.getName(), methodsArray));
    }

    try (FileWriter file = new FileWriter("../LibraryResult/" + libraryName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
