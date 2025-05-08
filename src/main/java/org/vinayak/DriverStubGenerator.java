package org.vinayak;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.IdentifierFactory;

public class DriverStubGenerator {

  public static void generateDriverStub(String methodSignature, String outputFilePath, String jarPath) {
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();
    inputLocations.add(new JavaClassPathAnalysisInputLocation(jarPath, SourceType.Application));

    JavaView view = new JavaView(inputLocations);

    for (SootClass sootClass : view.getClasses()) {
      if (sootClass.isLibraryClass() == true) {
        continue;
      }

      for (SootMethod method : sootClass.getMethods()) {
        if (!methodSignature.equalsIgnoreCase(method.getSignature().toString())
            || method.isAbstract()
            || method.isNative()) {
          continue;
        }
        System.out.println("[INFO] Generating driver stub for: " + method.getSignature());
        // logic to generate the stub will go here ---->

        boolean isStatic = method.isStatic();
        boolean isAbstract = method.isAbstract();
        String returnType = method.getReturnType().toString();
        IdentifierFactory factory = view.getIdentifierFactory();
        boolean isConstructor = factory.isConstructorSignature(method.getSignature());
        String fqClassName = method.getDeclaringClassType().getFullyQualifiedName();

        StringBuilder sb = new StringBuilder();
        sb.append("public class DriverStub {\n\n");

        // Generate source methods
        for (int i = 0; i < method.getParameterCount(); i++) {
          String type = method.getParameterType(i).toString().replace('$', '.');
          sb.append("    public static ").append(type)
              .append(" source").append(i).append("() {\n")
              .append("        return ").append(dummyReturn(type)).append(";\n")
              .append("    }\n\n");
        }

        // Generate run method
        sb.append("    public static void run() {\n");
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < method.getParameterCount(); i++) {
          if (i > 0)
            args.append(", ");
          args.append("source").append(i).append("()");
        }

        if (isConstructor) {
          sb.append("        new ").append(fqClassName.replace('$', '.')).append("(").append(args).append(");\n");
        } else if (isStatic) {
          if (!"void".equals(returnType)) {
            sb.append("        ").append(returnType).append(" result = ");
          } else {
            sb.append("        ");
          }
          sb.append(fqClassName.replace('$', '.')).append(".").append(method.getName()).append("(").append(args)
              .append(");\n");
        } else {
          if (!"void".equals(returnType)) {
            sb.append("        ").append(returnType).append(" result = ");
          } else {
            sb.append("        ");
          }
          sb.append("new ").append(fqClassName.replace('$', '.')).append("().")
              .append(method.getName()).append("(").append(args).append(");\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        try (FileWriter writer = new FileWriter(outputFilePath)) {
          writer.write(sb.toString());
          System.out.println("[INFO] Driver stub written to: " + outputFilePath);
        } catch (Exception e) {
          e.printStackTrace();
        }
        // logic to generate the stub will end here <----
      }
    }
  }

  private static String dummyReturn(String type) {
    switch (type) {
      case "int":
      case "short":
      case "byte":
        return "0";
      case "long":
        return "0L";
      case "float":
        return "0.0f";
      case "double":
        return "0.0";
      case "boolean":
        return "false";
      case "char":
        return "'a'";
      default:
        return "null";
    }
  }
}
