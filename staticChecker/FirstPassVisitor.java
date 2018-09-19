package staticChecker;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class FirstPassVisitor extends DepthFirstVisitor{
    public Map<Iden, ClassObj> classMap;

    public FirstPassVisitor(){
        classMap = new LinkedHashMap<Iden, ClassObj>();
    }

    @Override
    public void visit(Goal n) throws Exception{
        n.f0.accept(this);
        if(n.f1.size() != 0){
            n.f1.accept(this);
        }
    }

    @Override
    public void visit(MainClass n) throws Exception{
        ClassObj temp = new ClassObj(n.f1.f0.toString(), null);
        classMap.put(new Iden(temp.name), temp);
    }

    @Override
    public void visit(ClassDeclaration n) throws Exception{
        String className = n.f1.f0.toString();
        if(classMap.containsKey(new Iden(className))){
            throw new Exception();
        }else{
            ClassObj temp = new ClassObj(n.f1.f0.toString(), null);
            classMap.put(new Iden(temp.name), temp);
        }
    }

    @Override
    public void visit(ClassExtendsDeclaration n) throws Exception{
        String name = n.f1.f0.toString();
        String parent = n.f3.f0.toString();
        if(classMap.containsKey(new Iden(name)) ||
           !classMap.containsKey(new Iden(parent))){
            throw new Exception();
        }else{
            ClassObj temp = new ClassObj(name, parent);
            classMap.put(new Iden(name), temp);
        }
    }
}
