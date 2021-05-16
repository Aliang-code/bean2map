package com.netease.bean2map.processor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

/**
 * Provides functionality around {@link ExecutableElement}s.
 *
 * @author zhangsiliang
 */
public class ExecutableUtils {

    private static final Method DEFAULT_METHOD;

    static {
        Method method;
        try {
            method = ExecutableElement.class.getMethod("isDefault");
        } catch (NoSuchMethodException e) {
            method = null;
        }
        DEFAULT_METHOD = method;
    }

    private ExecutableUtils() {
    }

    static boolean isPublicNotStatic(ExecutableElement method) {
        return isPublic(method) && isNotStatic(method);
    }

    static boolean isPublic(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC);
    }

    private static boolean isNotStatic(ExecutableElement method) {
        return !method.getModifiers().contains(Modifier.STATIC);
    }

    public static boolean isDefaultMethod(ExecutableElement method) {
        try {
            return DEFAULT_METHOD != null && Boolean.TRUE.equals(DEFAULT_METHOD.invoke(method));
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    /**
     * @param mirror the type positionHint
     * @return the corresponding type element
     */
    private static TypeElement asTypeElement(TypeMirror mirror) {
        return (TypeElement) ((DeclaredType) mirror).asElement();
    }

    /**
     * Finds all executable elements within the given type element, including executable elements defined in super
     * classes and implemented interfaces. Methods defined in {@link java.lang.Object},
     * implementations of {@link java.lang.Object#equals(Object)} and private methods are ignored
     *
     * @param elementUtils element helper
     * @param element      the element to inspect
     * @return the executable elements usable in the type
     */
    public static List<ExecutableElement> getAllEnclosedExecutableElements(Elements elementUtils, TypeElement element) {
        List<ExecutableElement> enclosedElements = new ArrayList<>();
        element = replaceTypeElementIfNecessary(elementUtils, element);
        addEnclosedElementsInHierarchy(elementUtils, enclosedElements, element, element);

        return enclosedElements;
    }

    private static void addEnclosedElementsInHierarchy(Elements elementUtils, List<ExecutableElement> alreadyAdded,
                                                       TypeElement element, TypeElement parentType) {
        if (element != parentType) { // otherwise the element was already checked for replacement
            element = replaceTypeElementIfNecessary(elementUtils, element);
        }

        if (element.asType().getKind() == TypeKind.ERROR) {
            throw new IllegalArgumentException("referred types not available (yet), deferring mapper:" + element.getQualifiedName());
        }

        addNotYetOverridden(elementUtils, alreadyAdded, ElementFilter.methodsIn(element.getEnclosedElements()), parentType);

        if (hasNonObjectSuperclass(element)) {
            addEnclosedElementsInHierarchy(
                    elementUtils,
                    alreadyAdded,
                    asTypeElement(element.getSuperclass()),
                    parentType
            );
        }

        for (TypeMirror interfaceType : element.getInterfaces()) {
            addEnclosedElementsInHierarchy(
                    elementUtils,
                    alreadyAdded,
                    asTypeElement(interfaceType),
                    parentType
            );
        }

    }

    /**
     * When running during Eclipse Incremental Compilation, we might get a TypeElement that has an UnresolvedTypeBinding
     * and which is not automatically resolved. In that case, getEnclosedElements returns an empty list. We take that as
     * a hint to check if the TypeElement resolved by FQN might have any enclosed elements and, if so, return the
     * resolved element.
     *
     * @param elementUtils element utils
     * @param element      the original element
     * @return the element freshly resolved using the qualified name, if the original element did not return any
     * enclosed elements, whereas the resolved element does return enclosed elements.
     */
    public static TypeElement replaceTypeElementIfNecessary(Elements elementUtils, TypeElement element) {
        if (element.getEnclosedElements().isEmpty()) {
            TypeElement resolvedByName = elementUtils.getTypeElement(element.getQualifiedName());
            if (resolvedByName != null && !resolvedByName.getEnclosedElements().isEmpty()) {
                return resolvedByName;
            }
        }
        return element;
    }

    /**
     * @param alreadyCollected methods that have already been collected and to which the not-yet-overridden methods will
     *                         be added
     * @param methodsToAdd     methods to add to alreadyAdded, if they are not yet overridden by an element in the list
     * @param parentType       the type for with elements are collected
     */
    private static void addNotYetOverridden(Elements elementUtils, List<ExecutableElement> alreadyCollected,
                                            List<ExecutableElement> methodsToAdd, TypeElement parentType) {
        List<ExecutableElement> safeToAdd = new ArrayList<>(methodsToAdd.size());
        for (ExecutableElement toAdd : methodsToAdd) {
            if (isNotPrivate(toAdd) && isNotObjectEquals(toAdd)
                    && wasNotYetOverridden(elementUtils, alreadyCollected, toAdd, parentType)) {
                safeToAdd.add(toAdd);
            }
        }

        alreadyCollected.addAll(0, safeToAdd);
    }

    /**
     * @param executable the executable to check
     * @return {@code true}, iff the executable does not represent {@link java.lang.Object#equals(Object)} or an
     * overridden version of it
     */
    private static boolean isNotObjectEquals(ExecutableElement executable) {
        if (executable.getSimpleName().contentEquals("equals") && executable.getParameters().size() == 1
                && asTypeElement(executable.getParameters().get(0).asType()).getQualifiedName().contentEquals(
                "java.lang.Object"
        )) {
            return false;
        }
        return true;
    }

    /**
     * @param executable the executable to check
     * @return {@code true}, iff the executable does not have a private modifier
     */
    private static boolean isNotPrivate(ExecutableElement executable) {
        return !executable.getModifiers().contains(Modifier.PRIVATE);
    }

    /**
     * @param elementUtils     the elementUtils
     * @param alreadyCollected the list of already collected methods of one type hierarchy (order is from sub-types to
     *                         super-types)
     * @param executable       the method to check
     * @param parentType       the type for which elements are collected
     * @return {@code true}, iff the given executable was not yet overridden by a method in the given list.
     */
    private static boolean wasNotYetOverridden(Elements elementUtils, List<ExecutableElement> alreadyCollected,
                                               ExecutableElement executable, TypeElement parentType) {
        for (ListIterator<ExecutableElement> it = alreadyCollected.listIterator(); it.hasNext(); ) {
            ExecutableElement executableInSubtype = it.next();
            if (executableInSubtype == null) {
                continue;
            }
            if (elementUtils.overrides(executableInSubtype, executable, parentType)) {
                return false;
            } else if (elementUtils.overrides(executable, executableInSubtype, parentType)) {
                // remove the method from another interface hierarchy that is overridden by the executable to add
                it.remove();
                return true;
            }
        }

        return true;
    }

    /**
     * @param element the type element to check
     * @return {@code true}, iff the type has a super-class that is not java.lang.Object
     */
    private static boolean hasNonObjectSuperclass(TypeElement element) {
        if (element.getSuperclass().getKind() == TypeKind.ERROR) {
            throw new IllegalArgumentException("referred types not available (yet), deferring mapper:" + element.getQualifiedName());
        }

        return element.getSuperclass().getKind() == TypeKind.DECLARED
                && !asTypeElement(element.getSuperclass()).getQualifiedName().toString().equals("java.lang.Object");
    }
}
