package org.vinayak;

import org.junit.jupiter.api.Test;

public class CheckingException {
  @Test
  public void testAnalyzeBinaryAsmOld() {
    String pathToBinary = "resources/asm-5.1.jar";
    String classTypeStr = "asm-5.1";
    Main.analyzeJAR(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeBinaryAsmNew() {
    String pathToBinary = "resources/asm-7.2.jar";
    String classTypeStr = "asm-7.2";
    Main.analyzeJAR(pathToBinary, classTypeStr);
  }
}
