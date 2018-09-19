package staticChecker;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class Variable{
    public Iden     iden;
    public Type     type;
    public String   stringType;

    public Variable(Iden iden, Type type, String stringType){
        this.iden = iden;
        this.type = type;
        this.stringType = stringType;
    }

    @Override
    public boolean equals(Object o){
        Variable toCompare = (Variable) o;
        return iden.iden.equals(toCompare.iden.iden);
    }

    @Override
    public int hashCode(){
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.iden);
        return hash;
    }
}
