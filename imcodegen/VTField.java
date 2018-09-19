package imcodegen;

import java.util.*;
import staticChecker.*;

public class VTField{
    public VTIden           iden;
    public String           belongs;
    public String           type;

    public VTField(VTIden iden, String belongs, String type){
        this.iden = iden;
        this.belongs = belongs;
        this.type = type;
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
