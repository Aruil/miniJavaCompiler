package imcodegen;

import java.util.*;
import java.io.*;
import syntaxtree.*;
import visitor.*;
import staticChecker.*;

public class SpigletWriter extends GJDepthFirst<String, SpigletScope>{
    BufferedWriter                          bw;
    Map<Iden, VTableObj>                    vtablesMap;
    Map<SpigletScope, Map<Iden, VTTemp>>    scopeVariables;
    int                                     labels;
    boolean                                 flag;
    ArrayList<ArrayList<String>>            methodArgs;
    VTableObj                               allocatedClass;

    public SpigletWriter(BufferedWriter bw, Map<Iden, VTableObj> vtablesMap){
        this.bw = bw;
        this.vtablesMap = vtablesMap;
        scopeVariables = new LinkedHashMap<SpigletScope, Map<Iden, VTTemp>>();
        labels = 0;
        methodArgs = new ArrayList<ArrayList<String>>();
        flag = false;
        allocatedClass = null;
    }

    public void emit(String toWrite) throws Exception{
        bw.write(toWrite);
        bw.newLine();
    }

    @Override
    public String visit(Goal n, SpigletScope scope) throws Exception{
        n.f0.accept(this, null);
        if(n.f1.size() != 0){
            n.f1.accept(this, null);
        }

        return null;
    }

    @Override
    public String visit(MainClass n, SpigletScope scope) throws Exception{
        SpigletScope newScope = new SpigletScope("main", n.f1.f0.toString());
        Map<Iden, VTTemp> newMap = new LinkedHashMap<Iden, VTTemp>();
        scopeVariables.put(newScope, newMap);

        if(n.f15.size() != 0){
            n.f15.accept(this, newScope);
        }
        emit("END");

        return null;
    }

    @Override
    public String visit(TypeDeclaration n, SpigletScope scope) throws Exception{
        n.f0.accept(this, null);
        return null;
    }

    @Override
    public String visit(ClassDeclaration n, SpigletScope scope) throws Exception{
        SpigletScope newScope = new SpigletScope(null, n.f1.f0.toString());
        if(n.f4.size() != 0){
            n.f4.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(ClassExtendsDeclaration n, SpigletScope scope) throws Exception{
        SpigletScope newScope = new SpigletScope(null, n.f1.f0.toString());
        if(n.f6.size() != 0){
            n.f6.accept(this, newScope);
        }

        return null;
    }

    @Override
    public String visit(MethodDeclaration n, SpigletScope scope) throws Exception{
        SpigletScope newScope = new SpigletScope(n.f2.f0.toString(), scope.className);
        VTableObj curClass = vtablesMap.get(new Iden(scope.className));
        VTMethodObj curMethod = curClass.methods.get(new Iden(newScope.methodName));
        Map<Iden, VTTemp> newMap = new LinkedHashMap<Iden, VTTemp>();
        scopeVariables.put(newScope, newMap);
        newScope.tempCount = curMethod.args.size() + 1;

        emit(newScope.className + "_" + newScope.methodName + " [" +
             (curMethod.args.size() + 1) + "]");
        emit("BEGIN");
        if(n.f8.size() != 0){
            n.f8.accept(this, newScope);
        }
        String result = n.f10.accept(this, newScope);
        emit("RETURN");
        emit(result);
        emit("END");

        return null;
    }

    @Override
    public String visit(Block n, SpigletScope scope) throws Exception{
        if(n.f1.size() != 0){
            n.f1.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(AssignmentStatement n, SpigletScope scope) throws Exception{
        String identifier = n.f0.f0.toString();
        String tempNo;
        VTTemp temp;
        Iden iden = new Iden(identifier);
        Variable var;
        VTField field;
        VTableObj curClass = vtablesMap.get(new Iden(scope.className));
        VTMethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        Map<Iden, VTTemp> tempValues = scopeVariables.get(scope);
        String result = n.f2.accept(this, scope);

        if((var = curMethod.variables.get(iden)) != null){
            if((temp = tempValues.get(iden)) == null){
                tempNo = "TEMP " + scope.tempCount;
                scope.tempCount++;
                emit("MOVE " + tempNo + " " + result);
                tempValues.put(iden, new VTTemp(tempNo, allocatedClass));
            }else{
                emit("MOVE " + temp.tempName + " " + result);
            }
        }else{
            int i = 1, pos = 1;
            boolean flag = false;
            VTIden vtiden = null;
            for(Map.Entry<VTIden, VTField> vEntry : curClass.fields.entrySet()){
                VTField v = vEntry.getValue();
                if(v.iden.iden.equals(identifier)){
                    vtiden = v.iden;
                    flag = true;
                    pos = i;
                    if(v.belongs.equals(curMethod.belongs)){
                        pos = i;
                        break;
                    }
                }
                i++;
            }
            if(flag){
                if((temp = tempValues.get(vtiden)) == null){
                    tempNo = "TEMP " + scope.tempCount;
                    scope.tempCount++;
                    emit("MOVE " + tempNo + " " + result);
                    tempValues.put(iden, new VTTemp(tempNo, allocatedClass));
                }else{
                    emit("MOVE " + temp.tempName + " " + result);
                }
                emit("MOVE TEMP " + (scope.tempCount + 1) + " PLUS TEMP 0 " + (4 * pos));
                emit("HSTORE TEMP " + (scope.tempCount + 1) + " 0 " + result);
            }else{
                i = 1;
                for(Variable v : curMethod.args){
                    if(v.iden.iden.equals(identifier)){
                        emit("MOVE TEMP " + i + " " + result);
                        break;
                    }
                    i++;
                }
            }
        }

        return null;
    }

    @Override
    public String visit(ArrayAssignmentStatement n, SpigletScope scope)
        throws Exception{
        String identifier = n.f0.f0.toString();
        String tempNo = null;
        VTTemp temp;
        Iden iden = new Iden(identifier);
        Variable var;
        VTField field;
        VTableObj curClass = vtablesMap.get(new Iden(scope.className));
        VTMethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        Map<Iden, VTTemp> tempValues = scopeVariables.get(scope);
        String index = n.f2.accept(this, scope);
        String result = n.f5.accept(this, scope);
        String endLabel = "L" + labels;
        String errorLabel = "L" + (labels + 1);

        labels += 2;
        if((var = curMethod.variables.get(iden)) != null){
            if((temp = tempValues.get(iden)) == null){
                tempNo = "TEMP " + scope.tempCount;
                scope.tempCount++;
                tempValues.put(iden, new VTTemp(tempNo, null));
            }else{
                tempNo = temp.tempName;
            }
        }else{
            int i = 1, pos = 1;
            boolean flag = false;
            VTIden vtiden = null;
            for(Map.Entry<VTIden, VTField> vEntry : curClass.fields.entrySet()){
                VTField v = vEntry.getValue();
                if(v.iden.iden.equals(identifier)){
                    vtiden = v.iden;
                    flag = true;
                    pos = i;
                    if(v.belongs.equals(curMethod.belongs)){
                        pos = i;
                        break;
                    }
                }
                i++;
            }
            if(flag){
                if((temp = tempValues.get(vtiden)) == null){
                    tempNo = "TEMP " + scope.tempCount;
                    scope.tempCount++;
                    tempValues.put(iden, new VTTemp(tempNo, null));
                }else{
                    tempNo = temp.tempName;
                }
                emit("MOVE TEMP " + (scope.tempCount + 1) + " PLUS TEMP 0 " + (4 * pos));
                emit("HLOAD " + tempNo + " TEMP " + (scope.tempCount + 1) + " 0");
            }else{
                i = 1;
                for(Variable v : curMethod.args){
                    if(v.iden.iden.equals(identifier)){
                        tempNo = "TEMP " + i;
                        break;
                    }
                    i++;
                }
            }
        }
        String temp1 = "TEMP " + (scope.tempCount + 1);
        String temp2 = "TEMP " + (scope.tempCount + 2);
        emit("MOVE " + temp1 + " 0");
        emit("MOVE " + temp2 + " PLUS " + index + " 1");
        emit("MOVE " + temp1 + " LT " + temp1 + " " + temp2);
        emit("CJUMP " + temp1 + " " + errorLabel);
        emit("HLOAD " + temp1 + " " + tempNo + " 0");
        emit("MOVE " + temp1 + " LT " + index + " " + temp1);
        emit("CJUMP " + temp1 + " " + errorLabel);
        emit("MOVE " + temp2 + " TIMES " + temp2 + " 4");
        emit("MOVE " + temp2 + " PLUS " + tempNo + " " + temp2);
        emit("HSTORE " + temp2 + " 0 " + result);
        emit("JUMP " + endLabel);
        emit(errorLabel + " NOOP");
        emit("ERROR");
        emit(endLabel + " NOOP");

        return null;
    }

    @Override
    public String visit(IfStatement n, SpigletScope scope) throws Exception{
        String result = n.f2.accept(this, scope);
        String elseLabel = "L" + labels;
        String fiLabel = "L" + (labels + 1);
        labels += 2;

        emit("CJUMP " + result + " " + elseLabel);
        n.f4.accept(this, scope);
        emit("JUMP " + fiLabel);
        emit(elseLabel + " NOOP");
        n.f6.accept(this, scope);
        emit(fiLabel + " NOOP");

        return null;
    }

    @Override
    public String visit(WhileStatement n, SpigletScope scope) throws Exception{
        String result;
        String startLabel = "L" + labels;
        String endLabel = "L" + (labels + 1);
        labels += 2;

        emit(startLabel + " NOOP");
        result = n.f2.accept(this, scope);
        emit("CJUMP " + result + " " + endLabel);
        n.f4.accept(this, scope);
        emit("JUMP " + startLabel);
        emit(endLabel + " NOOP");

        return null;
    }

    @Override
    public String visit(PrintStatement n, SpigletScope scope) throws Exception{
        String result = n.f2.accept(this, scope);
        emit("PRINT " + result);

        return null;
    }

    @Override
    public String visit(Expression n, SpigletScope scope) throws Exception{
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
    public String visit(AndExpression n, SpigletScope scope) throws Exception{
        String temp = "TEMP " + scope.tempCount;
        String trueLabel = "L" + labels;
        String falseLabel = "L" + (labels + 1);

        labels += 2;
        scope.tempCount++;
        String leftOperand = n.f0.accept(this, scope);
        emit("CJUMP " + leftOperand + " " + falseLabel);
        scope.tempCount++;
        String rightOperand = n.f2.accept(this, scope);
        emit("CJUMP " + rightOperand + " " + falseLabel);
        emit("MOVE " + temp + " 1");
        emit("JUMP " + trueLabel);
        emit(falseLabel + " NOOP");
        emit("MOVE " + temp + " 0");
        emit(trueLabel + " NOOP");

        return temp;
    }

    @Override
    public String visit(CompareExpression n, SpigletScope scope) throws Exception{
        String temp = "TEMP " + scope.tempCount;

        scope.tempCount++;
        String leftOperand = n.f0.accept(this, scope);
        scope.tempCount++;
        String rightOperand = n.f2.accept(this, scope);

        emit("MOVE " + temp + " LT " + leftOperand + " " + rightOperand);

        return temp;
    }

    @Override
    public String visit(PlusExpression n, SpigletScope scope) throws Exception{
        String result = "TEMP " + scope.tempCount;
        scope.tempCount++;
        String term1 = n.f0.accept(this, scope);
        scope.tempCount++;
        String term2 = n.f2.accept(this, scope);

        emit("MOVE " + result + " PLUS " + term1 + " " + term2);

        return result;
    }

    @Override
    public String visit(MinusExpression n, SpigletScope scope) throws Exception{
        String result = "TEMP " + scope.tempCount;
        scope.tempCount++;
        String term1 = n.f0.accept(this, scope);
        scope.tempCount++;
        String term2 = n.f2.accept(this, scope);

        emit("MOVE " + result + " MINUS " + term1 + " " + term2);

        return result;
    }

    @Override
    public String visit(TimesExpression n, SpigletScope scope) throws Exception{
        String result = "TEMP " + scope.tempCount;
        scope.tempCount++;
        String term1 = n.f0.accept(this, scope);
        scope.tempCount++;
        String term2 = n.f2.accept(this, scope);

        emit("MOVE " + result + " TIMES " + term1 + " " + term2);

        return result;
    }

    @Override
    public String visit(ArrayLookup n, SpigletScope scope) throws Exception{
        String primary1 = n.f0.accept(this, scope);
        String primary2 = n.f2.accept(this, scope);
        String result = "TEMP " + scope.tempCount;
        String temp = "TEMP " + (scope.tempCount + 1);
        String endLabel = "L" + labels;
        String errorLabel = "L" + (labels + 1);

        labels += 2;
        emit("MOVE " + result + " 0");
        emit("MOVE " + temp + " PLUS " + primary2 + " 1");
        emit("MOVE " + result + " LT " + result + " " + temp);
        emit("CJUMP " + result + " " + errorLabel);
        emit("HLOAD " + result + " " + primary1 + " 0");
        emit("MOVE " + result + " LT " + primary2 + " " + result);
        emit("CJUMP " + result + " " + errorLabel);
        emit("MOVE " + temp + " TIMES " + temp + " 4");
        emit("MOVE " + temp + " PLUS " + temp + " " + primary1);
        emit("HLOAD " + temp + " " + temp + " 0");
        emit("JUMP " + endLabel);
        emit(errorLabel + " NOOP");
        emit("ERROR");
        emit(endLabel + " NOOP");

        return temp;
    }

    @Override
    public String visit(ArrayLength n, SpigletScope scope) throws Exception{
        String result = "TEMP " + scope.tempCount;
        String primary = n.f0.accept(this, scope);

        emit("HLOAD " + result + " " + primary + " 0");

        return result;
    }

    @Override
    public String visit(MessageSend n, SpigletScope scope) throws Exception{
        VTableObj targetClass;
        String targetMethod = n.f2.f0.toString();
        String primary = n.f0.accept(this, scope);
        targetClass = allocatedClass;
        String result = "TEMP " + scope.tempCount;
        String temp1 = "TEMP " + scope.tempCount + 1;
        String temp2 = "TEMP " + scope.tempCount + 2;
        String args;
        int i;

        scope.tempCount++;
        emit("HLOAD " + temp1 + " " + primary + " 0");
        i = 0;
        for(Map.Entry<Iden, VTMethodObj> mEntry : targetClass.methods.entrySet()){
            VTMethodObj m = mEntry.getValue();
            if(m.name.equals(targetMethod)) break;
            i++;
        }
        emit("HLOAD " + temp2 + " " + temp1 + " " + (i * 4));
        methodArgs.add(new ArrayList<String>());
        ArrayList<String> temp = methodArgs.get(methodArgs.size() - 1);
        flag = true;
        n.f4.accept(this, scope);
        bw.write("MOVE " + result + " CALL " + temp2 + "(" + primary + " ");
        for(String s : temp){
            bw.write(s + " ");
        }
        bw.write(")");
        bw.newLine();

        methodArgs.remove(methodArgs.size() - 1);
        flag = false;
        return result;
    }

    @Override
    public String visit(Clause n, SpigletScope scope) throws Exception{
        return n.f0.accept(this, scope);
    }

    @Override
    public String visit(NotExpression n, SpigletScope scope) throws Exception{
        String expr = n.f1.accept(this, scope);
        String tempNo = expr.substring(5);
        int tempNoint = Integer.parseInt(tempNo);
        tempNoint++;
        tempNo = "TEMP " + tempNoint;
        String result = "MOVE " + tempNo + " MINUS " + tempNo + " " + expr;
        emit("MOVE " + tempNo + " 1");
        emit(result);

        return tempNo;
    }

    @Override
    public String visit(PrimaryExpression n, SpigletScope scope) throws Exception{
        return n.f0.accept(this, scope);
    }

    @Override
    public String visit(IntegerLiteral n, SpigletScope scope) throws Exception{
        String intLiteral = n.f0.toString();
        String temp = "TEMP " + scope.tempCount;

        scope.tempCount++;
        emit("MOVE " + temp + " " + intLiteral);

        return temp;
    }

    @Override
    public String visit(TrueLiteral n, SpigletScope scope) throws Exception{
        String temp = "TEMP " + scope.tempCount;

        scope.tempCount++;
        emit("MOVE " + temp + " 1");

        return temp;
    }

    @Override
    public String visit(FalseLiteral n, SpigletScope scope) throws Exception{
        String temp = "TEMP " + scope.tempCount;

        scope.tempCount++;
        emit("MOVE " + temp + " 0");

        return temp;
    }

    @Override
    public String visit(Identifier n, SpigletScope scope) throws Exception{
        String identifier = n.f0.toString();
        String tempNo = null;
        VTTemp temp;
        Iden iden = new Iden(identifier);
        Variable var;
        VTField field;
        VTableObj curClass = vtablesMap.get(new Iden(scope.className));
        VTableObj targetClass = null;
        VTMethodObj curMethod = curClass.methods.get(new Iden(scope.methodName));
        Map<Iden, VTTemp> tempValues = scopeVariables.get(scope);

        if((var = curMethod.variables.get(iden)) != null){
            if((temp = tempValues.get(iden)) == null){
                tempNo = "TEMP " + scope.tempCount;
                scope.tempCount++;
                tempValues.put(iden, new VTTemp(tempNo, allocatedClass));
            }else{
                tempNo = temp.tempName;
            }
            targetClass = vtablesMap.get(new Iden(var.stringType));
        }else{
            int i = 1, pos = i;
            boolean flag = false;
            VTIden vtiden = null;
            VTField found = null;
            for(Map.Entry<VTIden, VTField> vEntry : curClass.fields.entrySet()){
                VTField v = vEntry.getValue();
                if(v.iden.iden.equals(identifier)){
                    vtiden = v.iden;
                    flag = true;
                    found = v;
                    pos = i;
                    if(v.belongs.equals(curMethod.belongs)){
                        pos = i;
                        break;
                    }
                }
                i++;
            }
            if(flag){
                if((temp = tempValues.get(vtiden)) == null){
                    tempNo = "TEMP " + scope.tempCount;
                    scope.tempCount++;
                    tempValues.put(iden, new VTTemp(tempNo, allocatedClass));
                }else{
                    tempNo = temp.tempName;
                }
                targetClass = vtablesMap.get(new Iden(found.type));
                emit("MOVE TEMP " + (scope.tempCount + 1) + " PLUS TEMP 0 " + (4 * pos));
                emit("HLOAD " + tempNo + " TEMP " + (scope.tempCount + 1) + " 0");
            }else{
                i = 1;
                for(Variable v : curMethod.args){
                    if(v.iden.iden.equals(identifier)){
                        tempNo = "TEMP " + i;
                        targetClass = vtablesMap.get(new Iden(v.stringType));
                        break;
                    }
                    i++;
                }
            }
        }

        allocatedClass = targetClass;
        return tempNo;
    }

    @Override
    public String visit(ThisExpression n, SpigletScope scope) throws Exception{
        VTableObj curClass = vtablesMap.get(new Iden(scope.className));
        allocatedClass = curClass;
        String temp = "TEMP 0";

        return temp;
    }

    @Override
    public String visit(ArrayAllocationExpression n, SpigletScope scope) throws
        Exception{
        String expr = n.f3.accept(this, scope);
        String endLabel = "L" + labels;
        String errorLabel = "L" + (labels + 1);
        String loopLabel = "L" + (labels + 2);
        String temp1 = "TEMP " + scope.tempCount;
        String temp2 = "TEMP " + (scope.tempCount + 1);
        String temp3 = "TEMP " + (scope.tempCount + 2);
        String temp4 = "TEMP " + (scope.tempCount + 3);

        labels += 3;
        emit("MOVE " + temp1 + " 0");
        emit("MOVE " + temp1 + " LT " + temp1 + " " + expr);
        emit("CJUMP " + temp1 + " " + errorLabel);
        emit("MOVE " + temp2 + " PLUS " + expr + " 1");
        emit("MOVE " + temp2 + " TIMES " + temp2 + " 4");
        emit("MOVE " + temp2 + " HALLOCATE " + temp2);
        emit("HSTORE " + temp2 + " 0 " + expr);
        emit("MOVE " + temp1 + " 0");
        emit("MOVE " + temp4 + " 0");
        emit(loopLabel + " NOOP");
        emit("MOVE " + temp3 + " LT " + temp1 + " " + expr);
        emit("CJUMP " + temp3 + " " + endLabel);
        emit("MOVE " + temp3 + " PLUS " + temp1 + " 1");
        emit("MOVE " + temp3 + " TIMES " + temp3 + " 4");
        emit("MOVE " + temp3 + " PLUS " + temp3 + " " + temp2);
        emit("HSTORE " + temp3 + " 0 " + temp4);
        emit("MOVE " + temp1 + " PLUS " + temp1 + " 1");
        emit("JUMP " + loopLabel);
        emit(errorLabel + " NOOP");
        emit("ERROR");
        emit(endLabel + " NOOP");

        return temp2;
    }

    @Override
    public String visit(AllocationExpression n, SpigletScope scope) throws Exception{
        String identifier = n.f1.f0.toString();
        Iden iden = new Iden(identifier);
        VTableObj targetClass = vtablesMap.get(iden);
        int objectSize = targetClass.fields.size() + 1;
        String tempNo = "TEMP " + scope.tempCount;
        String temp1 = "TEMP " + (scope.tempCount + 1);
        String temp2 = "TEMP " + (scope.tempCount + 2);

        scope.tempCount++;
        emit("MOVE " + tempNo + " HALLOCATE " + (objectSize * 4));
        emit("MOVE " + temp1 + " " + identifier + "_vTable");
        emit("HLOAD " + temp2 + " " + temp1 +" 0");
        emit("HSTORE " + tempNo + " 0 " + temp2);
        for(int i = 1; i < objectSize; i++){
            emit("MOVE " + temp1 + " 0");
            emit("HSTORE " + tempNo + " " + (i * 4) + " " + temp1);
        }
        allocatedClass = targetClass;

        return tempNo;
    }

    @Override
    public String visit(BracketExpression n, SpigletScope scope) throws Exception{
        return n.f1.accept(this, scope);
    }

    @Override
    public String visit(ExpressionList n, SpigletScope scope) throws Exception{
        n.f0.accept(this, scope);
        n.f1.accept(this, scope);
        return null;
    }

    @Override
    public String visit(ExpressionTail n, SpigletScope scope) throws Exception{
        if(n.f0.size() != 0){
            n.f0.accept(this, scope);
        }

        return null;
    }

    @Override
    public String visit(ExpressionTerm n, SpigletScope scope) throws Exception{
        flag = true;
        n.f1.accept(this, scope);

        return null;
    }
}

