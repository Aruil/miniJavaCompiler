package staticChecker;

import java.util.*;
import syntaxtree.*;
import visitor.*;

public class FinalPassVisitor extends GJDepthFirst<String, Scope>{
    public Map<Iden, ClassObj>              classMap;
    public ArrayList<ArrayList<String>>     methodArgs;
    boolean                                 flag;

    public FinalPassVisitor(Map<Iden, ClassObj> classMap){
        this.classMap = classMap;
        methodArgs = new ArrayList<ArrayList<String>>();;
        flag = false;
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
        Scope newScope = new Scope("main", n.f1.f0.toString());
        if(n.f15.size() != 0){
            n.f15.accept(this, newScope);
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
        if(n.f4.size() != 0){
            n.f4.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, Scope scope) throws Exception{
        Scope newScope = new Scope(null, n.f1.f0.toString());
        if(n.f6.size() != 0){
            n.f6.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(MethodDeclaration n, Scope scope) throws Exception{
        Scope newScope = new Scope(n.f2.f0.toString(), scope.className);
        ClassObj curClass = classMap.get(new Iden(scope.className));
        MethodObj curMethod = curClass.methods.get(new Iden(newScope.methodName));

        if(curMethod.type.equals(n.f10.accept(this, newScope))){
            if(n.f8.size() != 0){
                n.f8.accept(this, newScope);
            }
            String returnType = n.f10.accept(this, newScope);
            if(!curMethod.type.equals(returnType)){
                throw new Exception();
            }
        }else{
            throw new Exception();
        }

        return null;
    }

    @Override
    public String visit(Block n, Scope scope) throws Exception{
        if(n.f1.size() != 0){
            n.f1.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(AssignmentStatement n, Scope scope) throws Exception{
        String identifier = n.f0.f0.toString();
        ClassObj curClass = classMap.get(new Iden(scope.className));
        MethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        String identifierType = null;
        Variable var = null;
        Iden iden = new Iden(identifier);


        if((var = curMethod.variables.get(iden)) != null){
            identifierType = var.stringType;
        }else if((var = curClass.fields.get(iden)) != null){
            identifierType = var.stringType;
        }else if((var = checkInheritanceChainVar(scope.className, identifier))
                 != null){
            identifierType = var.stringType;
        }else{
            for(Variable v : curMethod.args){
                if(v.iden.iden.equals(identifier)){
                    identifierType = v.stringType;
                }
            }
        }
        if(identifierType == null){
            throw new Exception();
        }
        String exprType = n.f2.accept(this, scope);
        if(!identifierType.equals(exprType)){
            if(identifierType.equals("int") || identifierType.equals("int[]") ||
               identifierType.equals("boolean")){
                throw new Exception();
            }else{
                if(!checkInheritanceChain(identifierType, exprType)){
                    throw new Exception();
                }
            }
        }

        return null;
    }

    public boolean checkInheritanceChain(String type1, String type2){
        ClassObj class2 = classMap.get(new Iden(type2));
        while(class2.parent != null){
            class2 = classMap.get(new Iden(class2.parent));
            if(type1.equals(class2.name)){
                return true;
            }
        }

        return false;
    }

    public Variable checkInheritanceChainVar(String className, String varName){
        ClassObj curClass = classMap.get(new Iden(className));
        Iden varIden = new Iden(varName);
        while(curClass.parent != null){
            curClass = classMap.get(new Iden(curClass.parent));
            if(curClass.fields.containsKey(varIden)){
                return curClass.fields.get(varIden);
            }
        }

        return null;
    }

    @Override
    public String visit(ArrayAssignmentStatement n, Scope scope) throws Exception{
        String identifier = n.f0.f0.toString();
        ClassObj curClass = classMap.get(new Iden(scope.className));
        MethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        String identifierType = null;
        Variable var = null;
        Iden iden = new Iden(identifier);


        if((var = curMethod.variables.get(iden)) != null){
            if(var.stringType.equals("int[]")){
                identifierType = var.stringType;
            }
        }else if((var = curClass.fields.get(iden)) != null){
            if(var.stringType.equals("int[]")){
                identifierType = var.stringType;
            }
        }else if((var = checkInheritanceChainVar(scope.className, identifier))
                 != null){
            if(var.stringType.equals("int[]")){
                identifierType = var.stringType;
            }
        }else{
            for(Variable v : curMethod.args){
                if(v.iden.iden.equals(identifier)){
                    if(v.stringType.equals("int[]")){
                        identifierType = v.stringType;
                    }
                }
            }
        }
        if(identifierType == null){
            throw new Exception();
        }
        if(n.f2.accept(this, scope).equals("int")){
            if(n.f5.accept(this, scope).equals("int")){
                return null;
            }else{
                throw new Exception();
            }
        }else{
            throw new Exception();
        }
    }

    @Override
    public String visit(IfStatement n, Scope scope) throws Exception{
        if(!n.f2.accept(this, scope).equals("boolean")){
            throw new Exception();
        }else{
            n.f4.accept(this, scope);
            n.f6.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(WhileStatement n, Scope scope) throws Exception{
        if(!n.f2.accept(this, scope).equals("boolean")){
            throw new Exception();
        }else{
            n.f4.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(PrintStatement n, Scope scope) throws Exception{
        String expr = n.f2.accept(this, scope);
        if(!expr.equals("int") && !expr.equals("boolean")){
            throw new Exception();
        }
        return null;
    }

    @Override
    public String visit(Expression n, Scope scope) throws Exception{
        String expr;
        if(flag){
            flag = false;
            ArrayList<String> temp = methodArgs.get(methodArgs.size() - 1);
            expr = n.f0.accept(this, scope);
            temp.add(expr);
        }else{
            expr = n.f0.accept(this, scope);
        }
        return expr;
    }

    @Override
    public String visit(AndExpression n, Scope scope) throws Exception{
        if(!n.f0.accept(this, scope).equals("boolean") ||
           !n.f2.accept(this, scope).equals("boolean")){
            throw new Exception();
        }

        return "boolean";
    }

    @Override
    public String visit(CompareExpression n, Scope scope) throws Exception{
        if(!n.f0.accept(this, scope).equals("int") ||
           !n.f2.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "boolean";
    }

    @Override
    public String visit(PlusExpression n, Scope scope) throws Exception{
        if(!n.f0.accept(this, scope).equals("int") ||
           !n.f2.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "int";
    }

    @Override
    public String visit(MinusExpression n, Scope scope) throws Exception{
        if(!n.f0.accept(this, scope).equals("int") ||
           !n.f2.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "int";
    }

    @Override
    public String visit(TimesExpression n, Scope scope) throws Exception{
        if(!n.f0.accept(this, scope).equals("int") ||
           !n.f2.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "int";
    }

    @Override
    public String visit(ArrayLookup n, Scope scope) throws Exception{
        if(n.f0.f0.which != 3 && n.f0.f0.which != 7 ||
           !n.f0.accept(this, scope).equals("int[]") ||
           !n.f2.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "int";
    }

    @Override
    public String visit(ArrayLength n, Scope scope) throws Exception{
        if(n.f0.f0.which != 3 && n.f0.f0.which != 7 && n.f0.f0.which != 5 ||
           !n.f0.accept(this, scope).equals("int[]")){
            throw new Exception();
        }

        return "int";
    }

    @Override
    public String visit(MessageSend n, Scope scope) throws Exception{
        String targetClassName = null;
        MethodObj targetMethod;

        if(n.f0.f0.which == 3 || n.f0.f0.which == 4 || n.f0.f0.which == 6 ||
           n.f0.f0.which == 7){
            if(n.f0.f0.which == 4){
                targetClassName = scope.className;
            }else if(n.f0.f0.which == 3){
                targetClassName = n.f0.accept(this, scope);
            }else{
                targetClassName = n.f0.accept(this, scope);
            }
            ClassObj targetClass = classMap.get(new Iden(targetClassName));
            if(targetClass == null){
                throw new Exception();
            }else{
                String targetMethodName = n.f2.f0.toString();;;
                targetMethod = targetClass.methods.get(new Iden(targetMethodName));
                if(targetMethod == null &&
                   !checkInheritanceChainMethod(targetClassName, targetMethodName)){
                    throw new Exception();
                }else{
                    if(targetMethod == null){
                        Iden tempIden = new Iden(targetMethodName);
                        while(targetClass.parent != null){
                            targetClass = classMap.get(new Iden(targetClass.parent));
                            if(targetClass.methods.containsKey(tempIden)){
                                targetMethod = targetClass.methods.get(tempIden);
                                break;
                            }
                        }
                    }
                    methodArgs.add(new ArrayList<String>());
                    ArrayList<String> temp = methodArgs.get(methodArgs.size() - 1);
                    flag = true;
                    n.f4.accept(this, scope);
                    if(!checkMethodArgs(targetMethod, temp)){
                        throw new Exception();
                    }
                }
            }
        }else{
            throw new Exception();
        }

        methodArgs.remove(methodArgs.size() - 1);
        flag = false;
        return targetMethod.type;
    }

    public boolean checkInheritanceChainMethod(String className, String method){
        ClassObj curClass = classMap.get(new Iden(className));
        Iden targetMethod = new Iden(method);
        while(curClass.parent != null){
            curClass = classMap.get(new Iden(curClass.parent));
            if(curClass.methods.containsKey(targetMethod)){
                return true;
            }
        }

        return false;
    }

    public boolean checkMethodArgs(MethodObj method, ArrayList<String> args){
        int index;
        int i = 0;
        if(method.args.size() != args.size()){
            return false;
        }

        for(Variable v : method.args){
            index = method.args.indexOf(v);
            if(!args.get(index).equals(v.stringType) && !
               checkInheritanceChain(v.stringType, args.get(index))){
                return false;
            }
        }

        return true;
    }

    @Override
    public String visit(Clause n, Scope scope) throws Exception{
        return n.f0.accept(this, scope);
    }

    @Override
    public String visit(NotExpression n, Scope scope) throws Exception{
        if(!n.f1.accept(this, scope).equals("boolean")){
            throw new Exception();
        }

        return "boolean";
    }

    @Override
    public String visit(PrimaryExpression n, Scope scope) throws Exception{
        return n.f0.accept(this, scope);
    }

    @Override
    public String visit(IntegerLiteral n, Scope scope) throws Exception{
        return "int";
    }

    @Override
    public String visit(TrueLiteral n, Scope scope) throws Exception{
        return "boolean";
    }

    @Override
    public String visit(FalseLiteral n, Scope scope) throws Exception{
        return "boolean";
    }

    @Override
    public String visit(Identifier n, Scope scope) throws Exception{
        String identifier = n.f0.toString();;
        ClassObj curClass = classMap.get(new Iden(scope.className));
        MethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        String identifierType = null;
        Variable var = null;
        Iden iden = new Iden(identifier);

        if((var = curMethod.variables.get(iden)) != null){
            identifierType = var.stringType;
        }else if((var = curClass.fields.get(iden)) != null){
            identifierType = var.stringType;
        }else if((var = checkInheritanceChainVar(scope.className, identifier))
                 != null){
            identifierType = var.stringType;
        }else{
            for(Variable v : curMethod.args){
                if(v.iden.iden.equals(identifier)){
                    identifierType = v.stringType;
                }
            }
        }
        if(identifierType == null){
            throw new Exception();
        }

        return identifierType;
    }

    @Override
    public String visit(ThisExpression n, Scope scope) throws Exception{
        return scope.className;
    }

    @Override
    public String visit(ArrayAllocationExpression n, Scope scope) throws Exception{
        if(!n.f3.accept(this, scope).equals("int")){
            throw new Exception();
        }

        return "int[]";
    }

    @Override
    public String visit(AllocationExpression n, Scope scope) throws Exception{
        String className = n.f1.f0.toString();
        if(!classMap.containsKey(new Iden(className))){
            throw new Exception();
        }

        return className;
    }

    @Override
    public String visit(BracketExpression n, Scope scope) throws Exception{
        return n.f1.accept(this, scope);
    }

    @Override
    public String visit(ExpressionList n, Scope scope) throws Exception{
        n.f0.accept(this, scope);
        n.f1.accept(this, scope);
        return null;
    }

    @Override
    public String visit(ExpressionTail n, Scope scope) throws Exception{
        if(n.f0.size() != 0){
            n.f0.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(ExpressionTerm n, Scope scope) throws Exception{
        flag = true;
        n.f1.accept(this, scope);
        return null;
    }
}
