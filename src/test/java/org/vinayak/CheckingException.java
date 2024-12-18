package org.vinayak;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

public class CheckingException {
  @Test
  public void createByteCodeProject() {
    Path pathToBinary = Paths.get("target/test-classes");
    AnalysisInputLocation inputLocation =
        PathBasedAnalysisInputLocation.create(pathToBinary, SourceType.Application);

    View view = new JavaView(inputLocation);

    ClassType classType = view.getIdentifierFactory().getClassType("UncheckedException");

    System.out.println(view.getClass(classType));

    SootClass sootClass = view.getClass(classType).get();

    for (SootMethod method : sootClass.getMethods()) {
        System.out.println(method.getName());
        System.out.println(method.getSignature());

        method.getBody().getStmts().forEach(stmt -> {
            if (stmt instanceof JThrowStmt) {
                JThrowStmt throwStmt = (JThrowStmt) stmt;
                System.out.println("Found unchecked exception in method " + method.getName() + ": " + throwStmt.getOp());
            }
        });
    }
  }
}
