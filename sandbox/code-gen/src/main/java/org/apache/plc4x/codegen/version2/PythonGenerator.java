package org.apache.plc4x.codegen.version2;

import java.util.List;

public class PythonGenerator implements Generator {

    private final CodeWriter writer;

    public PythonGenerator(CodeWriter writer) {
        this.writer = writer;
    }

    @Override public void generate(ConstantNode constantNode) {
        writer.write(constantNode.getValue().toString());
    }

    @Override public void generateDeclarationWithInitializer(DeclarationStatement declarationStatement) {
        declarationStatement.getParameterExpression().write(this);
        writer.write(": ");
        declarationStatement.getParameterExpression().getType().write(this);
        writer.write(" = ");
        declarationStatement.getInitializer().write(this);
    }

    @Override public void generateDeclaration(DeclarationStatement declarationStatement) {
        declarationStatement.getParameterExpression().write(this);
        writer.write(": ");
        declarationStatement.getParameterExpression().getType().write(this);
        writer.write(" = None");
    }

    @Override public void generate(ParameterExpression parameterExpression) {
        writer.write(parameterExpression.getName());
    }

    @Override public void generate(Primitive primitive) {
        writer.write(primitive.getTypeString());
    }

    @Override public void generate(IfStatement ifStatement) {
        writer.startLine("if ");
        ifStatement.getCondition().write(this);
        writer.write(":\n");
        writeBlock(ifStatement.getBody());
        if (ifStatement.getOrElse() != null) {
            writer.writeLine("else:");
            writeBlock(ifStatement.getOrElse());
        }
    }

    @Override public void writeBlock(Block statements) {
        writer.startBlock();
        for (Node statement : statements.getStatements()) {
            // Dont to the wrapping for If Statements
            if (statement instanceof IfStatement) {
                statement.write(this);
            } else {
                writer.startLine("");
                statement.write(this);
                writer.endLine();
            }
        }
        writer.endBlock();
    }

    @Override public void generate(BinaryExpression binaryExpression) {
        binaryExpression.getLeft().write(this);
        writer.write(" ");
        writer.write(getOperator(binaryExpression.getOp()));
        writer.write(" ");
        binaryExpression.getRight().write(this);
    }

    @Override public void generate(AssignementExpression assignementExpression) {
        assignementExpression.getTarget().write(this);
        writer.write(" = ");
        assignementExpression.getValue().write(this);
    }

    @Override public void generateStaticCall(Method method, List<Node> arguments) {
        writer.write(method.getType().getTypeString());
        writer.write(".");
        writer.write(method.getName());
        writer.write("(");
        generateArgumentList(arguments);
        writer.write(")");
    }

    private void generateArgumentList(List<Node> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).write(this);
            if (i < arguments.size() - 1) {
                writer.write(", ");
            }
        }
    }

    @Override public void generateCall(Node target, Method method, List<Node> arguments) {
        target.write(this);
        writer.write(".");
        writer.write(method.getName());
        writer.write("(");
        generateArgumentList(arguments);
        writer.write(")");
    }

    @Override public void generate(NewExpression newExpression) {
        newExpression.getType().write(this);
        writer.write("(");
        generateArgumentList(newExpression.getArguments());
        writer.write(")");
    }

    @Override public void generate(MethodDefinition methodDefinition) {
        writer.startLine("def ");
        writer.write(" ");
        writer.write(methodDefinition.getName());
        writer.write("(");
        for (int i = 0; i < methodDefinition.getParameters().size(); i++) {
            methodDefinition.getParameters().get(i).getType().write(this);
            writer.write(" ");
            methodDefinition.getParameters().get(i).write(this);
            if (i < methodDefinition.getParameters().size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(") -> ");
        // Special handling of VOID is necessary
        if (methodDefinition.getResultType() == Primitive.VOID) {
            writer.write("None");
        } else {
            methodDefinition.getResultType().write(this);
        }
        writer.write(":");
        writer.endLine();
        methodDefinition.getBody().write(this);
    }

    @Override public void generateReturn(Expression value) {
        writer.write("return ");
        value.write(this);
    }

    @Override public void generateClass(String namespace, String className, List<FieldDeclaration> fields, List<ConstructorDeclaration> constructors, List<MethodDefinition> methods, List<ClassDefinition> innerClasses, boolean mainClass) {
        // Add static?!
        // Own File?
        writer.startLine("class ");
        writer.write(className);
        // TODO extends / implements
        writer.write(":");
        writer.endLine();
        writer.startBlock();

        // Insert a pass if there are no fields or methods
        if (fields.size() == 0 && methods.size() == 0) {
            writer.writeLine("pass");
        }

        writer.writeLine("");

        // Fields
        for (FieldDeclaration field : fields) {
            field.write(this);
            writer.writeLine("");
        }

        // Constructors
        if (constructors != null) {
            for (ConstructorDeclaration constructor : constructors) {
                this.generateConstructor(className, constructor.getParameters(), constructor.getBody());
                writer.writeLine("");
            }
        }

        // Methods
        for (MethodDefinition method : methods) {
            method.write(this);
            writer.writeLine("");
        }

        // If there are inner classes, implement them
        if (innerClasses != null) {
            for (ClassDefinition innerClass : innerClasses) {
                this.generateClass(innerClass.getNamespace(), innerClass.getClassName(), innerClass.getFields(), innerClass.getConstructors(), innerClass.getMethods(), innerClass.getInnerClasses(), false);
            }
        }

        writer.endBlock();
    }

    @Override public void generateFieldDeclaration(TypeNode type, String name) {
        writer.startLine("self.");
        writer.write(name);
        writer.write(": ");
        type.write(this);
        writer.endLine();
    }

    @Override public void generateFieldReference(TypeNode type, String name) {
        writer.write("self.");
        writer.write(name);
    }

    @Override public void generateConstructor(String className, List<ParameterExpression> parameters, Block body) {
        writer.startLine("def __init__(");
        for (int i = 0; i < parameters.size(); i++) {
            parameters.get(i).getType().write(this);
            writer.write(" ");
            parameters.get(i).write(this);
            if (i < parameters.size() - 1) {
                writer.write(", ");
            }
        }
        writer.write("):");
        writer.endLine();
        body.write(this);
    }

    @Override public void generateFile(ClassDefinition mainClass, List<ClassDefinition> innerClasses) {
        generateClass(mainClass.getNamespace(), mainClass.getClassName(), mainClass.getFields(), mainClass.getConstructors(), mainClass.getMethods(), innerClasses, true);
    }

    private String getOperator(BinaryExpression.Operation op) {
        switch (op) {
            case EQ:
                return "==";
            case PLUS:
                return "+";
        }
        throw new UnsupportedOperationException("The Operator " + op + " is currently not implemented!");
    }
}