// "Move 'return' closer to computation of the value of 'n'" "true"
class T {
    int f(boolean b) {
        int n = -1;
        try {
            n = 1;
            if (b) {
                throw new RuntimeException();
            }
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        re<caret>turn n;
    }
}