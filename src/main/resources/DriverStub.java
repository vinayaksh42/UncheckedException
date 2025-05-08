public class DriverStub {

    public static int source0() {
        return 0;
    }

    public static void run() {
        new com.esotericsoftware.kryo.io.Output().writeInt(source0());
    }
}
