package staticChecker;

import java.util.*;

public class Iden{
    public String iden;

    public Iden(String iden){
        this.iden = iden;
    }

    @Override
    public boolean equals(Object obj){
        Iden temp = (Iden) obj;
        return iden.equals(temp.iden);
    }

    @Override
    public int hashCode(){
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.iden);
        return hash;
    }
}
