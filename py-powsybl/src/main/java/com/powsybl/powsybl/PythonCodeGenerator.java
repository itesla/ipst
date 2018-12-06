/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.powsybl;

import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class PythonCodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonCodeGenerator.class);

    private static final Set<String> SIMPLE_TYPE = new HashSet<>(Arrays.asList("int", "boolean", "float", "double", "long", "class java.lang.String"));
    private static final Set<String> SKIP_METHODS_NAME = new HashSet<>(Arrays.asList("export", "merge", "visit", "remove"));
    private static final Set<String> SKIP_PARA_TYPE = new HashSet<>(Arrays.asList("Country", "Side", "Class"));
    private static final Set<String> MUTABLE_COLLECTIONS_METHOD = new HashSet<>(Arrays.asList("getBranches", "getLines", "getHvdcConverterStations", "getVoltageLevels"));

    private static final String BLANK_LINE = "";
    private static final String A1_DEF = "    def ";
    private static final String A2_RETURN = "        return ";
    private static final String A2 = "        ";
    private static final String A3 = "            ";
    private static final String SELF_ARG = "(self):";

    private PythonCodeGenerator() {

    }

    public static void main(String... args) {
//        Class clazz = Bus.class;
//        Class clazz = Branch.class;
//        Class clazz = BusbarSection.class;
//        Class clazz = Component.class;
//        Class clazz = Connectable.class;
//        Class clazz = Container.class;
//        Class clazz = CurrentLimits.class;
//        Class clazz = DanglingLine.class;
//        Class clazz = LccConverterStation.class;
//        Class clazz = Line.class;
//        Class clazz = Generator.class;
//        Class clazz = HvdcConverterStation.class;
//        Class clazz = HvdcLine.class;
//        Class clazz = Load.class;
        Class clazz = Network.class;
//        Class clazz = ReactiveLimits.class;
//        Class clazz = ShuntCompensator.class;
//        Class clazz = StaticVarCompensator.class;
//        Class clazz = Switch.class;
//        Class clazz = Terminal.class;
//        Class clazz = ThreeWindingsTransformer.class;
//        Class clazz = TwoWindingsTransformer.class;
//        Class clazz = VoltageLevel.class;
//        Class clazz = VscConverterStation.class;

        List<Class> superInterfaces = Arrays.asList(clazz.getInterfaces());
        System.out.println("----super interfaces----");
        for (Class c : superInterfaces) {
            System.out.println(c);
        }


        List<String> codes = new ArrayList<>();
        codes.add("# Auto generated python wrapper for java class: " + clazz.getCanonicalName());
        if (clazz.getSuperclass() == null && superInterfaces.isEmpty()) {
            codes.add("class " + clazz.getSimpleName() + ":");
            codes.add(BLANK_LINE);
            String arg = "j_" + clazz.getSimpleName().toLowerCase();
            codes.add(A1_DEF + "__init__(self, " + arg + "):");
            codes.add(A2 + "self.j_instance = " + arg);
        } else {
            String superClazz = superInterfaces.get(0).getSimpleName();
            if (superClazz.equals("Container")) {
                superClazz = "Identifiable";
            }
            codes.add("class " + clazz.getSimpleName() + "(" + superClazz + "):");
            codes.add(BLANK_LINE);
            String arg = "j_" + clazz.getSimpleName().toLowerCase();
            codes.add(A1_DEF + "__init__(self, " + arg + "):");
            codes.add(A2 + superClazz + ".__init__(self, " + arg + ")");
            codes.add(A2 + "self.j_instance = " + arg);
        }
        codes.add(BLANK_LINE);


        List<Method> methods = Arrays.asList(clazz.getMethods());
        methods.stream().sorted(Comparator.comparing(Method::getName)
        ).collect(Collectors.toList()).stream().sequential().forEach(m -> {
            String methodName = m.getName();
            if (!skip(m, clazz)) {
                String returnType = m.getReturnType().toString();
                if (SIMPLE_TYPE.contains(returnType)) {
                    codes.add(A1_DEF + to_python_style(methodName) + argsDef(m));
                    codes.add(A2_RETURN + "self.j_instance." + methodName + argsInvocation(m));
                    codes.add(BLANK_LINE);
                } else if (returnType.equals("interface java.lang.Iterable")) {
                    codes.add(A1_DEF + to_python_style(methodName) + SELF_ARG);
                    String eleClazzName = getReturnElementClassName(methodName);
                    String eleListVar = "l_" + eleClazzName.toLowerCase();
                    codes.add(A2 + eleListVar + " = []");
                    codes.add(A2 + pythonForStatement(methodName, clazz));

                    codes.add(A3 + eleListVar + ".append(" + eleClazzName + "(j_e))");
                    codes.add(A2_RETURN + eleListVar);
                    codes.add(BLANK_LINE);
                } else if (returnType.equals("interface java.util.List")) {
                    codes.add(A1_DEF + to_python_style(methodName) + argsDef(m));
                    String eleClazzName = getReturnElementClassName(methodName);
                    String eleListVar = "l_" + eleClazzName.toLowerCase();
                    codes.add(A2 + eleListVar + " = []");
                    codes.add(A2 + "for j_e in self.j_instance." + methodName + argsInvocation(m) + ":");

                    codes.add(A3 + eleListVar + ".append(" + eleClazzName + "(j_e))");
                    codes.add(A2_RETURN + eleListVar);
                    codes.add(BLANK_LINE);
                } else if (returnType.startsWith("interface com.powsybl.iidm.network")) {
                    String returnTypeSimpleName = m.getReturnType().getSimpleName();
                    if (!skipByReturnType(m)) {
                        codes.add(A1_DEF + to_python_style(methodName) + argsDef(m));
                        codes.add(A2_RETURN + returnTypeSimpleName + "(self.j_instance." + methodName + argsInvocation(m) + ")");
                        codes.add(BLANK_LINE);
                    }
                } else if (returnType.equals("void")) {
                    codes.add(A1_DEF + to_python_style(methodName) + argsDef(m));
                    codes.add(A2 + "self.j_instance." + methodName + argsInvocation(m));
                    codes.add(BLANK_LINE);
                } else {
                    System.out.println(returnType);
                    System.out.println(m + " not implemented".toUpperCase());
                }
            }
        });
        System.out.println("-------------------------");
        codes.stream().forEach(l -> System.out.println(l));
    }

    // TODO init a real network in jvm and check instance type
    static String pythonForStatement(String methodName, Class c) {
        String collection = "Array";
        // TODO Bus.getFoos()
        if (c.getSimpleName().equals("Network") && MUTABLE_COLLECTIONS_METHOD.contains(methodName)) {
            collection = "List";
        }
        return "for j_e in self.j_instance." + methodName + "().to" + collection + "():";
    }

    static String argsDef(Method m) {
        List<Class> classes = Arrays.asList(m.getParameterTypes());
        if (classes.isEmpty()) {
            return SELF_ARG;
        } else {
            String paras = "(self,";
            for (int i = 0; i < classes.size() - 1; i++) {
                paras += " var" + i + ",";
            }
            paras += " var" + (classes.size() - 1) + "):";
            return paras;
        }
    }

    static String argsInvocation(Method m) {
        List<Class> classes = Arrays.asList(m.getParameterTypes());
        if (classes.isEmpty()) {
            return "()";
        } else {
            if (classes.size() == 1) {
                return "(var0)";
            }
            String paras = "(";
            for (int i = 0; i < classes.size() - 1; i++) {
                paras += " var" + i + ",";
            }
            paras += " var" + (classes.size() - 1) + ")";
            return paras;
        }
    }

    static String getReturnElementClassName(String methodName) {
        System.out.println(methodName);
        String returnElementName = methodName.substring(3);
        String removeS = returnElementName.substring(0, returnElementName.length() - 1);
        if (removeS.equals("Buse") || removeS.equals("Switche") || removeS.equals("Branche")) {
            return returnElementName.substring(0, returnElementName.length() - 2);
        }
        if (removeS.equals("Shunt")) {
            return "ShuntCompensator";
        }
        return removeS;
    }

    static boolean skipByReturnType(Method m) {
        String returnTypeSimpleName = m.getReturnType().getSimpleName();
        if (m.getReturnType().toString().contains("$")) {
            System.out.println(m + " TODO nested class");
            return true;
        }
        if (returnTypeSimpleName.endsWith("TapChanger")) {
            System.out.println(m + " TODO tapchanger");
            return true;
        }
        if (returnTypeSimpleName.endsWith("StateManager")) {
            System.out.println(m + " TODO StateManager");
            return true;
        }
        return false;
    }

    static boolean skip(Method m, Class clazz) {
        String methodName = m.getName();
        List<Annotation> annotations = Arrays.asList(m.getDeclaredAnnotations());
        for (Annotation anno : annotations) {
            if (anno.annotationType().getSimpleName().equals("Deprecated")) {
                System.out.println(m + " skipped(Deprecated)");
                return true;
            }
        }
        if (methodName.startsWith("new")) {
            System.out.println(m + " skipped(new)");
            return true;
        }
        if (methodName.endsWith("Stream")) {
            System.out.println(m + " skipped(Stream)");
            return true;
        }
        if (methodName.startsWith("set") && !clazz.getSimpleName().equals("Switch")) {
            System.out.println(m + " skipped(set)");
            return true;
        }
        List<Class> parameterClass = Arrays.asList(m.getParameterTypes());
        for (Class c : parameterClass) {
            if (SKIP_PARA_TYPE.contains(c.getSimpleName())) {
                System.out.println(m + " skipped(parameters type)");
                return true;
            }
        }
        for (String mn : SKIP_METHODS_NAME) {
            if (methodName.startsWith(mn)) {
                System.out.println(m + " skipped(mapbe to implement later)");
                return true;
            }
        }

        List<Class> superInterfaces = Arrays.asList(clazz.getInterfaces());
        if (!superInterfaces.isEmpty()) {
            List<Method> methodsInSuperClass = Arrays.asList(superInterfaces.get(0).getMethods());
            if (methodsInSuperClass.contains(m)) {
                System.out.println(m + " skipped(super method)");
                return true;
            }
        }
        return false;
    }

    static String to_python_style(String javaMethodName) {
        if (javaMethodName.equals("getbPerSection")) {
            return "get_b_per_section";
        }
        String[] r = javaMethodName.split("(?=\\p{Upper}|\\d)");
//        Arrays.asList(r).stream().forEach(l -> System.out.println(l));
        String python = "";
        for (int i = 0; i < r.length - 1; i++) {
            python = python + r[i].toLowerCase() + "_";
        }
        python = python + r[r.length - 1].toLowerCase();
        return python;
    }

}
