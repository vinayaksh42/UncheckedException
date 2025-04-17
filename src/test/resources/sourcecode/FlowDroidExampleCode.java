
public class FlowDroidExampleCode {
    public static String source() {
        return "secret";
    }

    private void throwException(String str) {
        throw new RuntimeException(str);
    }

    // Source is the SOURCE
    // throw new RuntimeException() is the SINK

    // The SOURCE reaches the SINK
    public void testArray1() {
        String[] arr = new String[2];
        arr[0] = source();
        arr[1] = "hello";
        throwException(arr[0]);
    }

    // The SOURCE does not reach the SINK
    public void testArray2() {
        String[] arr = new String[2];
        arr[0] = source();
        throwException("hello");
    }

    // The SOURCE is part of the conditional which cause the Exception
    public void testArray3() {
        String temp = source();
        String[] arr = new String[2];
        arr[0] = temp;
        arr[1] = "hello";
        if (arr[0] == "secret") {
            throwException("hello");
        }
    }

    // The SOURCE is not part of the conditional which cause the Exception
    public void testArray4() {
        String[] arr = new String[2];
        arr[0] = source();
        String Temp = "hello";
        if (Temp == "hello") {
            throwException("hello");
        }
    }

    // The SOURCE is part of the conditional which cause the Exception
    public void testArray5() {
        String[] arr = new String[2];
        arr[0] = source();
        if (arr[0] == "secret") {
            arr[1] = "hello";
        } else {
            throwException("hello");
        }
    }

    // The SOURCE is not part of the conditional which cause the Exception
    public void testArray6() {
        String[] arr = new String[3];
        arr[0] = source();
        String Temp = "hello";
        if (Temp == "hello") {
            arr[2] = "world";
        } else {
            throwException("hello");
        }
    }
}
