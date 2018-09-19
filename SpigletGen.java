import java.io.*;
import java.util.*;
import parser.*;
import syntaxtree.*;
import staticChecker.*;
import imcodegen.*;

public class SpigletGen{
    public static void main(String[] args) throws Exception{
        if (args.length < 1) {
            System.err.println("Not enough arguments.");
            System.exit(1);
        }

        FileInputStream istream = null;

        for(String arg : args){
            try{
                istream = new FileInputStream(arg);
                File spigletFile = new File(arg.replaceAll("(?i).java", ".spg"));
                FileOutputStream ostream = new FileOutputStream(spigletFile);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ostream));
                MiniJavaParser parser = new MiniJavaParser(istream);
                Goal parserTree = parser.Goal();
                FirstPassVisitor fpvisitor = new FirstPassVisitor();
                parserTree.accept(fpvisitor);
                SecondPassVisitor spvisitor = new SecondPassVisitor(fpvisitor.classMap);
                parserTree.accept(spvisitor, null);
                VTableGenerator vtgen = new VTableGenerator();
                vtgen.generate(spvisitor.classMap);
                vtgen.write(bw);
                SpigletWriter writer = new SpigletWriter(bw, vtgen.vtablesMap);
                parserTree.accept(writer, null);
                bw.close();
                System.out.println("Spiglet code for " + arg +
                                   " generated successfully.");
            }catch(Exception ex){
                System.out.println("Failed to generate Spiglet code for " + arg);
            }
        }
    }
}
