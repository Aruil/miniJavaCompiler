package staticChecker;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class MethodObj{
    public String               name;
    public String               type;
    public ArrayList<Variable>  args;
    public Map<Iden, Variable>  variables;

    public MethodObj(String name, String type){
        this.name = name;
        this.type = type;
        args = new ArrayList<Variable>();
        variables = new LinkedHashMap<Iden, Variable>();
    }
}
