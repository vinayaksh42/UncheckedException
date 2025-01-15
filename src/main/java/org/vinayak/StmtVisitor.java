package org.vinayak;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.visitor.*;
import sootup.core.model.Body;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.jimple.basic.JavaLocal;

class StmtVisitor extends AbstractStmtVisitor<StmtVisitor> {
    private final Body body;
    private final View view;
    private final Map<ClassType, JavaLocal> methodExceptionMap;
    private final List<ClassType> checkedExceptions;
    private final List<ClassType> uncheckedExceptions;
    private final List<String> internalMethodCalls;
    private final List<String> externalMethodCalls;

    public StmtVisitor(
            Body body,
            View view,
            Map<ClassType, JavaLocal> methodExceptionMap,
            List<ClassType> checkedExceptions,
            List<ClassType> uncheckedExceptions,
            List<String> internalMethodCalls,
            List<String> externalMethodCalls) {
        this.body = body;
        this.view = view;
        this.methodExceptionMap = methodExceptionMap;
        this.checkedExceptions = checkedExceptions;
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
            JNewExpr exception = (JNewExpr) rhs;
            JavaLocal stackTrace = (JavaLocal) lhs;
            methodExceptionMap.put(exception.getType(), stackTrace);
        }
    }

    @Override
    public void caseThrowStmt(@Nonnull JThrowStmt stmt) {
        JThrowStmt throwStmt = stmt;
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
