package imcodegen;

import java.util.*;
import staticChecker.*;

public class VTableObj{
    public String                   name;
    public Map<VTIden, VTField>     fields;
    public Map<Iden, VTMethodObj>   methods;

    public VTableObj(String name, String parent){
        this.name = name;
        fields = new LinkedHashMap<VTIden, VTField>();
        methods = new LinkedHashMap<Iden, VTMethodObj>();
    }
}
