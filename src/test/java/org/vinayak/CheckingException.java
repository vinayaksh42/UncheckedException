package org.vinayak;

import java.util.ArrayList;
import java.util.List;
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
    String pathToBinary = "resources/kryo-5.6.2.jar";
    String classTypeStr = "kryo-5.6.2";
    List<String> additonalJars = new ArrayList<>();
    additonalJars.add("resources/minlog-1.3.1.jar");
    additonalJars.add("resources/objenesis-3.4.jar");
    additonalJars.add("resources/reflectasm-1.11.9.jar");
    Main.callgraphBasedLibraryAnalysis(pathToBinary, classTypeStr, additonalJars);
  }

  @Test
  public void testAnalyzeBinaryHttpclient5() {
    String pathToBinary = "resources/rtree-0.8.6.jar";
    String classTypeStr = "rtree-0.8.6";
    Main.analyzeClientJAR(pathToBinary, classTypeStr);
  }

  @Test
  public void testAnalyzeBinaryReflectasm() {
    String pathToBinary = "client_jar/reflectasm-1.11.10-SNAPSHOT.jar";
    String classTypeStr = "reflectasm-1.11.10-SNAPSHOT";
    Main.main(new String[] {pathToBinary, classTypeStr, "client"});
  }
}
