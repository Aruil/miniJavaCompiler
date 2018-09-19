package staticChecker;

import java.util.*;

public class ClassObj{
    public String               name;
    public String               parent;
    public Map<Iden, Variable>  fields;
    public Map<Iden, MethodObj> methods;

    public ClassObj(String name, String parent){
        this.name = name;
        this.parent = parent;
        fields = new LinkedHashMap<Iden, Variable>();
        methods = new LinkedHashMap<Iden, MethodObj>();
    }
}
