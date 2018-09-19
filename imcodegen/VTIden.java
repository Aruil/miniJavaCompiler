package imcodegen;

import java.util.*;

public class VTIden{
    public String iden;
    public String belongs;

    public VTIden(String iden, String belongs){
        this.iden = iden;
        this.belongs = belongs;
    }

    @Override
    public boolean equals(Object obj){
        VTIden temp = (VTIden) obj;
        return iden.equals(temp.iden) && belongs.equals(temp.belongs);
    }

    @Override
    public int hashCode(){
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.iden) +
               Objects.hashCode(this.belongs);

        return hash;
    }
}
