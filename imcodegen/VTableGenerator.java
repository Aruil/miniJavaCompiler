package imcodegen;

import java.io.*;
import java.util.*;
import staticChecker.*;

public class VTableGenerator{
    public Map<Iden, VTableObj> vtablesMap;

    public VTableGenerator(){
        vtablesMap = new LinkedHashMap<Iden, VTableObj>();
    }

    public void generate(Map<Iden, ClassObj> classMap){
        VTableObj vtable;
        for(Map.Entry<Iden, ClassObj> cEntry : classMap.entrySet()){
            ClassObj c = cEntry.getValue();
            vtable = new VTableObj(c.name, c.parent);
            if(c.parent != null){
                VTableObj pVTable = vtablesMap.get(new Iden(c.parent));
                for(Map.Entry<VTIden, VTField> vEntry : pVTable.fields.entrySet()){
                    VTField v = vEntry.getValue();
                    VTField field = new VTField(v.iden, v.belongs, v.type);
                    vtable.fields.put(v.iden, field);
                }
                for(Map.Entry<Iden, VTMethodObj> mEntry : pVTable.methods.entrySet()){
                    VTMethodObj m = mEntry.getValue();
                    VTMethodObj method = new VTMethodObj(m.name, m.belongs,
                                                         m.args, m.variables);
                    vtable.methods.put(new Iden(m.name), method);
                }
            }
            for(Map.Entry<Iden, Variable> vEntry : c.fields.entrySet()){
                Variable v = vEntry.getValue();
                VTIden vtiden = new VTIden(v.iden.iden, c.name);
                VTField field = new VTField(vtiden, c.name, v.stringType);
                vtable.fields.put(vtiden, field);
            }
            for(Map.Entry<Iden, MethodObj> mEntry : c.methods.entrySet()){
                MethodObj m = mEntry.getValue();
                VTMethodObj method = new VTMethodObj(m.name, c.name, m.args,
                                                     m.variables);
                vtable.methods.put(new Iden(m.name), method);
            }
            vtablesMap.put(new Iden(c.name), vtable);
        }
    }

    public void print(){
        for(Map.Entry<Iden, VTableObj> cEntry : vtablesMap.entrySet()){
            VTableObj c = cEntry.getValue();
            System.out.println(c.name);
            for(Map.Entry<VTIden, VTField> fEntry : c.fields.entrySet()){
                VTField f = fEntry.getValue();
                System.out.println("F\t" + f.belongs + "." + f.iden.iden +
                                   " [" + f.type + "]");
            }
            for(Map.Entry<Iden, VTMethodObj> mEntry : c.methods.entrySet()){
                VTMethodObj m = mEntry.getValue();
                System.out.println("M\t" + m.belongs + "." + m.name);
            }
        }
    }

    public void write(BufferedWriter bw) throws Exception{
        int allocate, i;
        Iden iden = new Iden("main");
        bw.write("MAIN");
        bw.newLine();
        for(Map.Entry<Iden, VTableObj> cEntry : vtablesMap.entrySet()){
            VTableObj c = cEntry.getValue();
            boolean flag = true;
            if(c.methods.containsKey(iden)){
                VTMethodObj mainMethod = c.methods.get(iden);
                if(mainMethod.belongs.equals(c.name)) flag = false;
            }
            if(flag){
                bw.write("MOVE TEMP 20" + " " + c.name + "_vTable");
                bw.newLine();
                allocate = c.methods.size();
                bw.write("MOVE TEMP 21 HALLOCATE " + (allocate * 4));
                bw.newLine();
                bw.write("HSTORE TEMP 20 0 TEMP 21");
                bw.newLine();
                i = 0;
                for(Map.Entry<Iden, VTMethodObj> mEntry : c.methods.entrySet()){
                    VTMethodObj m = mEntry.getValue();
                    bw.write("MOVE TEMP 23 " + m.belongs + "_" + m.name);
                    bw.newLine();
                    bw.write("HSTORE TEMP 21 " + (i * 4) + " TEMP 23");
                    bw.newLine();
                    i++;
                }
            }
        }
    }
}
