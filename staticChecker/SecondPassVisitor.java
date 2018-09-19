package staticChecker;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class SecondPassVisitor extends GJDepthFirst<String, Scope>{
    public Map<Iden, ClassObj> classMap;

    public SecondPassVisitor(Map<Iden, ClassObj> classMap){
        this.classMap = classMap;
    }

    @Override
    public String visit(Goal n, Scope scope) throws Exception{
        n.f0.accept(this, null);
        if(n.f1.size() != 0){
            n.f1.accept(this, null);
        }

        return null;
    }

    @Override
    public String visit(MainClass n, Scope scope) throws Exception{
        Scope mainScope = new Scope("main", n.f1.f0.toString());
        ClassObj mainClass = classMap.get(new Iden(mainScope.className));
        MethodObj mainMethod = new MethodObj("main", "void");
        mainMethod.args.add(new Variable(new Iden(n.f11.f0.toString()), null,
                            "String"));
        mainClass.methods.put(new Iden("main"), mainMethod);
        if(n.f14.size() != 0){
            n.f14.accept(this, mainScope);
        }

        return null;
    }

    @Override
    public String visit(TypeDeclaration n, Scope scope) throws Exception{
        n.f0.accept(this, null);
        return null;
    }

    @Override
    public String visit(ClassDeclaration n, Scope scope) throws Exception{
        Scope newScope = new Scope(null, n.f1.f0.toString());
        if(n.f3.size() != 0){
            n.f3.accept(this, newScope);
        }
        if(n.f4.size() != 0){
            n.f4.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, Scope scope) throws Exception{
        Scope newScope = new Scope(null, n.f1.f0.toString());
        if(n.f5.size() != 0){
            n.f5.accept(this, newScope);
        }
        if(n.f6.size() != 0){
            n.f6.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(VarDeclaration n, Scope scope) throws Exception{
        String varType = n.f0.f0.accept(this, null);
        Variable var = new Variable(new Iden(n.f1.f0.toString()), n.f0, varType);
        ClassObj curClass = classMap.get(new Iden(scope.className));
        if(varType.equals("int") || varType.equals("int[]") ||
           varType.equals("boolean") || classMap.containsKey(new Iden(varType))){
            if(scope.methodName != null){
                MethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
                for(Variable v : curMethod.args){
                    if(v.iden.iden.equals(var.iden.iden)){
                        throw new Exception();
                    }
                }
                if(!curMethod.variables.containsKey(var.iden)){
                    curMethod.variables.put(var.iden, var);
                }else{
                    throw new Exception();
                }
            }else{
                if(!curClass.fields.containsKey(var.iden)){
                    curClass.fields.put(var.iden, var);
                }else{
                    throw new Exception();
                }
            }
        }else{
            throw new Exception();
        }

        return null;
    }

    public boolean checkInheritanceChainVar(String className, Variable var){
        ClassObj curClass = classMap.get(new Iden(className));
        while(curClass.parent != null){
            curClass = classMap.get(new Iden(curClass.parent));
            if(curClass.fields.containsKey(var.iden)){
                return false;
            }
        }

        return true;
    }

    @Override
    public String visit(MethodDeclaration n, Scope scope) throws Exception{
        ClassObj curClass = classMap.get(new Iden(scope.className));
        String type = n.f1.f0.accept(this, null);
        String methodName = n.f2.f0.toString();
        if(type.equals("int") || type.equals("int[]") || type.equals("boolean")
           || classMap.containsKey(new Iden(type))){
            if(!curClass.methods.containsKey(new Iden(methodName))){
                MethodObj curMethod = new MethodObj(methodName, type);
                curClass.methods.put(new Iden(methodName), curMethod);
                Scope newScope = new Scope(methodName, scope.className);
                n.f4.accept(this, newScope);
                n.f7.accept(this, newScope);
                if(!checkInheritanceChain(scope.className, curMethod)){
                    throw new Exception();
                }
            }else{
                throw new Exception();
            }
        }else{
            throw new Exception();
        }

        return null;
    }

    public boolean checkInheritanceChain(String className, MethodObj method){
        ClassObj curClass = classMap.get(new Iden(className));
        Iden tempIden = new Iden(method.name);
        while(curClass.parent != null){
            curClass = classMap.get(new Iden(curClass.parent));
            if(curClass.methods.containsKey(tempIden)){
                MethodObj tempMethod = curClass.methods.get(tempIden);
                if(!method.type.equals(tempMethod.type) ||
                   !checkMethodArgs(method, tempMethod)){
                    return false;
                }
            }
        }

        return true;
    }

    public boolean checkMethodArgs(MethodObj method1, MethodObj method2){
        ArrayList<Variable> list1 = method1.args;
        ArrayList<Variable> list2 = method2.args;
        int index;
        int i = 0;

        if(list1.size() != list2.size()){
            return false;
        }

        for(Variable v : list1){
            index = list2.indexOf(v);
            if(index == -1 || index != i ||
               !list2.get(index).stringType.equals(v.stringType)){
                return false;
            }
            i++;
        }

        return true;
    }

    @Override
    public String visit(FormalParameterList n, Scope scope) throws Exception{
        n.f0.accept(this, scope);
        n.f1.accept(this, scope);
        return null;
    }

    @Override
    public String visit(FormalParameterTail n, Scope scope) throws Exception{
        n.f0.accept(this, scope);
        return null;
    }

    @Override
    public String visit(FormalParameterTerm n, Scope scope) throws Exception{
        n.f1.accept(this, scope);
        return null;
    }

    @Override
    public String visit(FormalParameter n, Scope scope) throws Exception{
        String varType = n.f0.f0.accept(this, null);
        if(varType.equals("int") || varType.equals("int[]") ||
           varType.equals("boolean") || classMap.containsKey(new Iden(varType))){
            Variable var = new Variable(new Iden(n.f1.f0.toString()), n.f0,
                                        n.f0.f0.accept(this, null));
            ClassObj curClass = classMap.get(new Iden(scope.className));
            if(curClass.fields.containsKey(var.iden) ||
               !checkInheritanceChainVar(scope.className, var)){
                throw new Exception();
            }else{
                MethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
                for(Variable v : curMethod.args){
                    if(v.equals(var)){
                        throw new Exception();
                    }
                }
                curMethod.args.add(var);
            }
        }else{
            throw new Exception();
        }

        return null;
    }

    @Override
    public String visit(IntegerType n, Scope scope) throws Exception{
        return n.f0.toString();
    }

    @Override
    public String visit(ArrayType n, Scope scope) throws Exception{
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, Scope scope) throws Exception{
        return n.f0.toString();
    }

    @Override
    public String visit(Identifier n, Scope scope) throws Exception{
        return n.f0.toString();
    }
}
