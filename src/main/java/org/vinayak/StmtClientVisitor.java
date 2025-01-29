package org.vinayak;

import java.util.List;
import javax.annotation.Nonnull;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.visitor.*;
import sootup.core.types.ClassType;
import sootup.core.views.View;

class StmtClientVisitor extends AbstractStmtVisitor<StmtVisitor> {
  private final View view;
  private final List<String> externalMethodCalls;

  public StmtClientVisitor(View view, List<String> externalMethodCalls) {
    this.view = view;
    this.externalMethodCalls = externalMethodCalls;
  }

  @Override
  public void caseAssignStmt(@Nonnull JAssignStmt stmt) {
    JAssignStmt assignStmt = stmt;
    if (assignStmt.getRightOp() instanceof JStaticInvokeExpr) {
      JStaticInvokeExpr staticInvokeExpr = (JStaticInvokeExpr) assignStmt.getRightOp();
      ClassType classType = staticInvokeExpr.getMethodSignature().getDeclClassType();
      boolean isInternal = view.getClass(classType).isPresent();
      if (!isInternal) {
        externalMethodCalls.add(staticInvokeExpr.getMethodSignature().toString());
      }
    }
  }

  @Override
  public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
    JInvokeStmt invokeStmt = stmt;
    ClassType classType = invokeStmt.getInvokeExpr().getMethodSignature().getDeclClassType();
    boolean isInternal = view.getClass(classType).isPresent();
    if (!isInternal) {
      externalMethodCalls.add(invokeStmt.getInvokeExpr().getMethodSignature().toString());
    }
  }
}
