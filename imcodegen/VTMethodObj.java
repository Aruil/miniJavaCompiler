package imcodegen;

import java.util.*;
import staticChecker.*;

public class VTMethodObj{
    public String               name;
    public String               belongs;
    public ArrayList<Variable>  args;
    public Map<Iden, Variable>  variables;

    public VTMethodObj(String name, String belongs, ArrayList<Variable> args,
                       Map<Iden, Variable> variables){
        this.name = name;
        this.belongs = belongs;
        this.args = args;
        this.variables = variables;
    }
}
