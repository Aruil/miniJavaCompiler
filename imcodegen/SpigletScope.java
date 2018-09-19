package imcodegen;

import java.util.*;

public class SpigletScope{
    public String   className;
    public String   methodName;
    int             tempCount;

    public SpigletScope(String methodName, String className){
        this.className = className;
        this.methodName = methodName;
        tempCount = 0;
    }

    @Override
    public boolean equals(Object obj){
        SpigletScope temp = (SpigletScope) obj;
        return className.equals(temp.className) &&
               methodName.equals(temp.methodName);
    }

    @Override
    public int hashCode(){
        int hash = 3;
        hash = 69 * hash + Objects.hashCode(this.className) +
               Objects.hashCode(this.methodName);
        return hash;
    }
}
