package org.vinayak;

import org.junit.jupiter.api.Test;

public class CheckingException {
  @Test
  public void testAnalyzeBinaryAsmOld() {
    String pathToBinary = "resources/asm-5.1.jar";
    String classTypeStr = "asm-5.1";
    Main.analyzeLibraryJAR(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeBinaryAsmNew() {
    String pathToBinary = "resources/asm-7.2.jar";
    String classTypeStr = "asm-7.2";
    Main.analyzeLibraryJAR(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeBinaryKryoNew() {
    String pathToBinary = "resources/kryo-5.0.0-RC4.jar";
    String classTypeStr = "kryo-5.0.0-RC4";
    Main.analyzeLibraryJAR(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeJARCommonsCodec() {
    String pathToBinary = "resources/commons-codec-1.13.jar";
    String classTypeStr = "commons-codec-1.13-test";
    Main.callgraphBasedLibraryAnalysis(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeBinaryHttpclient5() {
    String pathToBinary = "resources/httpclient5-5.0-beta6.jar";
    String classTypeStr = "httpclient5-5.0-beta6";
    Main.analyzeClientJAR(pathToBinary, classTypeStr);
  }
}
