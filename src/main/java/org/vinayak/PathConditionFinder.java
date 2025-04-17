package org.vinayak;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import sootup.core.graph.BasicBlock;
import sootup.core.graph.DominanceFinder;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.views.View;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.views.JavaView;

public class PathConditionFinder {
  public static void pathConditionFinder(String str) {
    Path pathToBinary = Paths.get("src/test/resources/bytecode");
    AnalysisInputLocation inputLocation = PathBasedAnalysisInputLocation.create(pathToBinary, null);

    View view = new JavaView(inputLocation);
    for (SootClass sootClass : view.getClasses()) {
      for (SootMethod method : sootClass.getMethods()) {
        if (method.getSignature().toString().equals(str)) {
          System.out.println("Method: " + method.getSignature());
          Body body = method.getBody();
          StmtGraph stmtGraph = body.getStmtGraph();
          // PostDominanceFinder postDominanceFinder = new PostDominanceFinder(stmtGraph);
          DominanceFinder dominanceFinder = new DominanceFinder(stmtGraph);
          body.getStmtGraph()
              .<Stmt>getNodes()
              .forEach(
                  node -> {
                    if (node instanceof JThrowStmt) {
                      Stmt stmt = node;
                      System.out.println("Statement: " + stmt);

                      BasicBlock idForward =
                          dominanceFinder.getImmediateDominator(stmtGraph.getBlockOf(stmt));

                      while (idForward != null) {
                        System.out.println("Immediate Dominator: " + idForward);
                        idForward = dominanceFinder.getImmediateDominator(idForward);
                      }
                    }
                  });

          System.out.println("-------------------");

          for (Stmt stmt : body.getStmts()) {
            System.out.println("Statement: " + stmt);
          }
        }
      }
    }
  }
}
