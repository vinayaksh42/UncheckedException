package org.vinayak;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.visitor.*;
import sootup.core.model.Body;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.jimple.basic.JavaLocal;

class StmtVisitor extends AbstractStmtVisitor<StmtVisitor> {
  private final TypeHierarchy typeHierarchy;
  private final View view;
  private final Body.BodyBuilder bodyBuilder;
  private final List<ClassType> uncheckedExceptions;
  private final List<String> internalMethodCalls;
  private final List<String> externalMethodCalls;

  public StmtVisitor(
      TypeHierarchy typeHierarchy,
      View view,
      Body.BodyBuilder bodyBuilder,
      List<ClassType> uncheckedExceptions,
      List<String> internalMethodCalls,
      List<String> externalMethodCalls) {
    this.typeHierarchy = typeHierarchy;
    this.view = view;
    this.bodyBuilder = bodyBuilder;
    this.uncheckedExceptions = uncheckedExceptions;
    this.internalMethodCalls = internalMethodCalls;
    this.externalMethodCalls = externalMethodCalls;
  }

  @Override
  public void caseAssignStmt(@Nonnull JAssignStmt stmt) {
    JAssignStmt assignStmt = stmt;
    Value rhs = assignStmt.getRightOp();
    Value lhs = assignStmt.getLeftOp();
    if (rhs instanceof JNewExpr && lhs instanceof Local) {
      JNewExpr newExpr = (JNewExpr) rhs;
      JavaLocal stackTrace = (JavaLocal) lhs;
      Local local = new Local(stackTrace.getName(), newExpr.getType());
      bodyBuilder.replaceLocal(stackTrace, local);
    }
  }

  @Override
  public void caseThrowStmt(@Nonnull JThrowStmt stmt) {
    JThrowStmt throwStmt = stmt;
    JavaLocal stackName = (JavaLocal) throwStmt.getOp();

    Set<Local> locals = bodyBuilder.getLocals();
    for (Local local : locals) {
      if (local.getName().equals(stackName.getName())) {
        ClassType type = (ClassType) stackName.getType();
        ClassType superClass = typeHierarchy.superClassOf(type).get();
        if (type.toString().equals("java.lang.RuntimeException")) {
          uncheckedExceptions.add(type);
        }
        if (superClass.toString().equals("java.lang.RuntimeException")) {
          uncheckedExceptions.add(type);
        }
      }
    }
  }

  @Override
  public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
    JInvokeStmt invokeStmt = stmt;
    ClassType classType = invokeStmt.getInvokeExpr().getMethodSignature().getDeclClassType();
    boolean isInternal = view.getClass(classType).isPresent();
    if (isInternal) {
      internalMethodCalls.add(invokeStmt.getInvokeExpr().getMethodSignature().toString());
    } else {
      externalMethodCalls.add(invokeStmt.getInvokeExpr().getMethodSignature().toString());
    }
  }
}
