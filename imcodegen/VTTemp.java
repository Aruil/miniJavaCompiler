package imcodegen;

import java.util.*;

public class VTTemp{
    public String tempName;
    public String type;

    public VTTemp(String tempName, VTableObj curClass){
        this.tempName = tempName;
        if(curClass != null){
            this.type = curClass.name;
        }else{
            this.type = null;
        }
    }
}
