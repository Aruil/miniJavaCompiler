package staticChecker;

public class Scope{
    public String methodName;
    public String className;

    public Scope(String methodName, String className){
        this.methodName = methodName;
        this.className = className;
    }
}
