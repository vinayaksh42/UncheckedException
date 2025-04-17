package org.vinayak;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import soot.jimple.infoflow.results.InfoflowResults;

public class PathConditionFinderTest {

  @Test
  public void simpleSourceToSink() {
    String methodSignature = "<FlowDroidExampleCode: void testArray1()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertNotNull(results);
  }

  @Test
  public void simpleSourceToSinkNegative() {
    String methodSignature = "<FlowDroidExampleCode: void testArray2()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertTrue(results.isEmpty());
  }

  @Test
  public void ifCondSourceToSink() {
    String methodSignature = "<FlowDroidExampleCode: void testArray3()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertNotNull(results);
  }

  @Test
  public void ifCondSourceToSinkNegative() {
    String methodSignature = "<FlowDroidExampleCode: void testArray4()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertTrue(results.isEmpty());
  }

  @Test
  public void elseCondSourceToSink() {
    String methodSignature = "<FlowDroidExampleCode: void testArray5()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertNotNull(results);
  }

  @Test
  public void elseCondSourceToSinkNegative() {
    String methodSignature = "<FlowDroidExampleCode: void testArray6()>";
    InfoflowResults results = FlowDroidDemo.pathConditionFinder(methodSignature);
    assertTrue(results.isEmpty());
  }
}
