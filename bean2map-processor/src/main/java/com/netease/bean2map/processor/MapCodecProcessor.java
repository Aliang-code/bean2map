package com.netease.bean2map.processor;

import com.netease.bean2map.codec.IMapCodec;
import com.netease.bean2map.codec.Ignore;
import com.netease.bean2map.codec.MapCodecRegister;
import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

@SupportedAnnotationTypes("com.netease.bean2map.codec.MapCodec")
public class MapCodecProcessor extends AbstractProcessor {
    private final List<String> codecNames = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            try {
                FileObject codecManifestFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                        "", "META-INF/" + MapCodecRegister.class.getName());
                try (OutputStream codecManifestOutput = codecManifestFile.openOutputStream()) {
                    for (String codecName : codecNames) {
                        codecManifestOutput.write((codecName + "\r\n").getBytes(StandardCharsets.UTF_8));
                    }
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                            "create MapCodec manifest file success:" + codecManifestFile.toUri());
                } catch (Exception e) {
                    e.printStackTrace();
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "create MapCodec manifest file failed:" + e.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "create MapCodec manifest file failed:" + e.getMessage());
            }
        }

        TypeElement supportAnnotation = null;
        Iterator<? extends TypeElement> iterator = annotations.iterator();
        //获取annotations的type
        if (iterator.hasNext()) {
            supportAnnotation = iterator.next();
        }
        if (supportAnnotation == null) {
            return false;
        }
        //做一个类型判断是否为MapCodec注解类型，这里我们只添加了MapCodec注解支持
        if ("com.netease.bean2map.codec.MapCodec".equals(supportAnnotation.getQualifiedName().toString())) {
            Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(supportAnnotation);
            elementsAnnotatedWith.forEach(new Consumer<Element>() {
                @Override
                public void accept(Element element) {
                    TypeElement typeElem = (TypeElement) element;
                    generateFile(typeElem);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns {@code true} when the {@link ExecutableElement} is a getter method. A method is a getter when it
     * has no parameters, starts
     * with 'get' and the return type is any type other than {@code void}, OR the getter starts with 'is' and the type
     * returned is a primitive or the wrapper for {@code boolean}. NOTE: the latter does strictly not comply to the bean
     * convention. The remainder of the name is supposed to reflect the property name.
     * <p>
     *
     * @param method to be analyzed
     * @return {@code true} when the method is a getter.
     */
    public boolean isGetterMethod(ExecutableElement method) {
        if (!method.getParameters().isEmpty()) {
            // If the method has parameters it can't be a getter
            return false;
        }
        String methodName = method.getSimpleName().toString();

        boolean isNonBooleanGetterName = methodName.startsWith("get") && methodName.length() > 3 &&
                method.getReturnType().getKind() != TypeKind.VOID && method.getParameters().isEmpty();

        boolean isBooleanGetterName = methodName.startsWith("is") && methodName.length() > 2;
        TypeElement paramType = getTypeElement(method.getReturnType());
        boolean returnTypeIsBoolean = method.getReturnType().getKind() == TypeKind.BOOLEAN ||
                (paramType != null && "java.lang.Boolean".equals(paramType.getQualifiedName().toString()));

        return isNonBooleanGetterName || (isBooleanGetterName && returnTypeIsBoolean);
    }

    /**
     * Returns {@code true} when the {@link ExecutableElement} is a setter method. A setter starts with 'set'. The
     * remainder of the name is supposed to reflect the property name.
     * <p>
     *
     * @param method to be analyzed
     * @return {@code true} when the method is a setter.
     */
    public boolean isSetterMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return methodName.startsWith("set") && methodName.length() > 3 && !method.getParameters().isEmpty();
    }

    /**
     * Analyzes the method (getter or setter) and derives the property name.
     * See {@link #isGetterMethod(ExecutableElement)} {@link #isSetterMethod(ExecutableElement)}. The first three
     * ('get' / 'set' scenario) characters are removed from the simple name, or the first 2 characters ('is' scenario).
     * From the remainder the first character is made into small case (to counter camel casing) and the result forms
     * the property name.
     *
     * @param getterOrSetterMethod getter or setter method.
     * @return the property name.
     */
    public String getPropertyName(ExecutableElement getterOrSetterMethod) {
        String methodName = getterOrSetterMethod.getSimpleName().toString();
        return decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
    }

    /**
     * Helper method, to obtain the type.
     *
     * @param type input type
     * @return fully qualified name of type when the type is a {@link DeclaredType}, null when otherwise.
     */
    protected static TypeElement getTypeElement(TypeMirror type) {
        DeclaredType declaredType = type.accept(
                new SimpleTypeVisitor6<DeclaredType, Void>() {
                    @Override
                    public DeclaredType visitDeclared(DeclaredType t, Void p) {
                        return t;
                    }
                },
                null
        );

        if (declaredType == null) {
            return null;
        }

        TypeElement typeElement = declaredType.asElement().accept(
                new SimpleElementVisitor6<TypeElement, Void>() {
                    @Override
                    public TypeElement visitType(TypeElement e, Void p) {
                        return e;
                    }
                },
                null
        );

        return typeElement != null ? typeElement : null;
    }


    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     *
     * @param name The string to be decapitalized.
     * @return The decapitalized version of the string.
     */
    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private void generateFile(TypeElement element) {
        String clazzName = element.getQualifiedName().toString();
        TypeMirror typeMirror = element.asType();
        TypeName typeName = TypeName.get(typeMirror);
        int lastIndex = clazzName.lastIndexOf('.');
        String _package = clazzName.substring(0, lastIndex);
        String _entity = clazzName.substring(lastIndex + 1);
        String _codec = _entity + "_MapCodec";
        try {
            MethodSpec.Builder codeBuild = MethodSpec.methodBuilder("code")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(typeName, "entity").build())
                    .returns(ParameterizedTypeName.get(Map.class, String.class, Object.class))
                    .addStatement("$T map = new $T<>()",
                            ParameterizedTypeName.get(Map.class, String.class, Object.class),
                            HashMap.class);

            MethodSpec.Builder decodeBuild = MethodSpec.methodBuilder("decode")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec
                            .builder(ParameterizedTypeName
                                    .get(Map.class, String.class, Object.class), "map")
                            .build())
                    .returns(typeName)
                    .addStatement("$T entity = new $T()", typeMirror, typeMirror);

            MethodSpec.Builder filterBuild = MethodSpec.methodBuilder("filter")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec
                            .builder(ParameterizedTypeName
                                    .get(Map.class, String.class, Object.class), "map")
                            .build())
                    .returns(ParameterizedTypeName.get(Map.class, String.class, Object.class))
                    .addStatement("$T result = new $T<>()",
                            ParameterizedTypeName.get(Map.class, String.class, Object.class),
                            HashMap.class);

            //获取所有公共方法，包括继承
            List<ExecutableElement> methods = ExecutableUtils.getAllEnclosedExecutableElements(processingEnv.getElementUtils(), element);
            for (ExecutableElement method : methods) {
                String methodName = method.getSimpleName().toString();
                Ignore ignore = method.getAnnotation(Ignore.class);
                if (ignore == null) {
                    if (isGetterMethod(method)) {
                        codeBuild.beginControlFlow("if(entity.$L()!=null)", methodName);
                        codeBuild.addStatement("map.put($S, entity.$L())", getPropertyName(method), methodName);
                        codeBuild.endControlFlow();
                        filterBuild.addStatement("result.put($S, map.get($S))", getPropertyName(method), getPropertyName(method));
                    } else if (isSetterMethod(method)) {
                        decodeBuild.addStatement("entity.$L(($T) map.get($S))",
                                methodName, method.getParameters().get(0).asType(), getPropertyName(method));
                    }
                }
            }
            codeBuild.addStatement("return map");
            decodeBuild.addStatement("return entity");
            filterBuild.addStatement("return result");

            TypeSpec helloWorld = TypeSpec.classBuilder(_codec)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(ParameterizedTypeName.get(
                            ClassName.get(IMapCodec.class), typeName))
                    .addMethod(codeBuild.build())
                    .addMethod(decodeBuild.build())
                    .addMethod(filterBuild.build())
                    .build();

            JavaFile javaFile = JavaFile.builder(_package, helloWorld)
                    .build();
            //生成文件
            javaFile.writeTo(processingEnv.getFiler());
            codecNames.add(_package + "." + _codec);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "process codec success:" + _codec, element);
        } catch (Exception e) {
            e.printStackTrace();
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "process codec error:" + e.getMessage(), element);
        }
    }
}