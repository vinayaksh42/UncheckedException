package org.vinayak;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.jimple.basic.JavaLocal;
import sootup.java.core.views.JavaView;

public class Main {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println(
          "Usage: java -jar unexpectedException-1.0-SNAPSHOT.jar <directory-path> <output-folder>");
      System.exit(1);
    }
    String pathToJAR = args[0];
    String libraryName = args[1];
    analyzeJAR(pathToJAR, libraryName);
  }

  public static void analyzeJAR(String pathToJAR, String libraryName) {
    JSONArray classArray = new JSONArray();
    Path path = Paths.get(pathToJAR);
    AnalysisInputLocation inputLocation =
        PathBasedAnalysisInputLocation.create(path, SourceType.Application);
    View view = new JavaView(inputLocation);
    for (SootClass sootClass : view.getClasses()) {
      JSONArray methodsArray = new JSONArray();
      for (SootMethod method : sootClass.getMethods()) {
        Map<ClassType, JavaLocal> methodExceptionMap = new HashMap<>();
        List<ClassType> checkedExceptions = method.getExceptionSignatures();
        List<ClassType> uncheckedExceptions = new ArrayList<>();
        List<String> internalMethodCalls = new ArrayList<>();
        List<String> externalMethodCalls = new ArrayList<>();

        System.out.println(method.getSignature());
        System.out.println(checkedExceptions);
        method
            .getBody()
            .getStmts()
            .forEach(
                stmt -> {
                  if (stmt instanceof JAssignStmt) {
                    JAssignStmt assignStmt = (JAssignStmt) stmt;
                    if (assignStmt.getRightOp() instanceof JNewExpr) {
                      JNewExpr exception = (JNewExpr) assignStmt.getRightOp();
                      JavaLocal stackTrace = (JavaLocal) assignStmt.getLeftOp();
                      methodExceptionMap.put(exception.getType(), stackTrace);
                    }
                  }
                  if (stmt instanceof JInvokeStmt) {
                    JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
                    ClassType classType =
                        invokeStmt.getInvokeExpr().getMethodSignature().getDeclClassType();
                    boolean isInternal = view.getClass(classType).isPresent();
                    if (isInternal) {
                      internalMethodCalls.add(
                          invokeStmt.getInvokeExpr().getMethodSignature().toString());
                    } else {
                      externalMethodCalls.add(
                          invokeStmt.getInvokeExpr().getMethodSignature().toString());
                    }
                  }
                  if (stmt instanceof JThrowStmt) {
                    JThrowStmt throwStmt = (JThrowStmt) stmt;
                    JavaLocal stackName = (JavaLocal) throwStmt.getOp();
                    if (methodExceptionMap.containsValue(stackName)) {
                      for (Map.Entry<ClassType, JavaLocal> entry : methodExceptionMap.entrySet()) {
                        if (entry.getValue().equals(stackName)) {
                          if (!checkedExceptions.contains(entry.getKey())) {
                            uncheckedExceptions.add(entry.getKey());
                          }
                        }
                      }
                    }
                  }
                });

        JSONObject methodObject = new JSONObject();
        methodObject.put("methodName", method.getName());
        methodObject.put("methodSignature", method.getSignature());
        methodObject.put(
            "checked_exceptions", checkedExceptions.stream().map(ClassType::toString).toArray());
        methodObject.put(
            "unchecked_exceptions",
            uncheckedExceptions.stream().map(ClassType::toString).toArray());
        methodObject.put("internal_method_calls", internalMethodCalls.toArray());
        methodObject.put("external_method_calls", externalMethodCalls.toArray());
        methodsArray.put(methodObject);
      }
      classArray.put(new JSONObject().put(sootClass.getName(), methodsArray));
    }

    // write the output to a file
    try (FileWriter file = new FileWriter(libraryName + ".json")) {
      file.write(classArray.toString(4));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
