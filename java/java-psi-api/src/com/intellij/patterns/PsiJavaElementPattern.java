// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.patterns;

import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiJavaElementPattern<T extends PsiElement,Self extends PsiJavaElementPattern<T,Self>> extends PsiElementPattern<T,Self> {
  private static final @NonNls String VALUE = "value";

  public PsiJavaElementPattern(final Class<T> aClass) {
    super(aClass);
  }

  public PsiJavaElementPattern(final @NotNull InitialPatternCondition<T> condition) {
    super(condition);
  }

  public Self annotationParam(final @NonNls String annotationQualifiedName, final @NonNls String parameterName) {
    return withParent(
      PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
          PsiJavaPatterns.psiAnnotation().qName(annotationQualifiedName))));
  }

  public Self annotationParam(final @NonNls String annotationQualifiedName) {
    return annotationParam(annotationQualifiedName, VALUE);
  }

  public Self annotationParam(final ElementPattern<String> annotationQualifiedName, final @NonNls String parameterName) {
    return withParent(
      PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
          PsiJavaPatterns.psiAnnotation().qName(annotationQualifiedName))));
  }
  public Self annotationParam(@NotNull ElementPattern<? extends PsiAnnotation> annotation) {
    return withParent(
      PsiJavaPatterns.psiNameValuePair().withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
          annotation)));
  }

  public Self annotationParam(String parameterName, @NotNull ElementPattern<? extends PsiAnnotation> annotation) {
    return withParent(
      PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(annotation)));
  }

  public Self insideAnnotationParam(final ElementPattern<String> annotationQualifiedName, final @NonNls String parameterName) {
    return withAncestor(3,   // can be array initializer
                        PsiJavaPatterns.psiNameValuePair().withName(parameterName).withParent(
        PlatformPatterns.psiElement(PsiAnnotationParameterList.class).withParent(
          PsiJavaPatterns.psiAnnotation().qName(annotationQualifiedName))));
  }

  public Self insideAnnotationParam(final ElementPattern<String> annotationQualifiedName) {
    return insideAnnotationParam(annotationQualifiedName, VALUE);
  }

  public Self insideAnnotationParam(@NotNull String annotationQualifiedName) {
    return insideAnnotationParam(StandardPatterns.string().equalTo(annotationQualifiedName));
  }

  public Self nameIdentifierOf(final Class<? extends PsiMember> aClass) {
    return nameIdentifierOf(StandardPatterns.instanceOf(aClass));
  }

  public Self nameIdentifierOf(final ElementPattern<? extends PsiMember> pattern) {
    return with(new PatternCondition<T>("nameIdentifierOf") {
      @Override
      public boolean accepts(final @NotNull T t, final ProcessingContext context) {
        if (!(t instanceof PsiIdentifier)) return false;

        final PsiElement parent = t.getParent();
        if (parent instanceof PsiClass && t != ((PsiClass) parent).getNameIdentifier()) return false;
        if (parent instanceof PsiMethod && t != ((PsiMethod) parent).getNameIdentifier()) return false;
        if (parent instanceof PsiVariable && t != ((PsiVariable) parent).getNameIdentifier()) return false;

        return pattern.accepts(parent, context);
      }
    });
  }

  public Self methodCallParameter(final int index, final ElementPattern<? extends PsiMethod> methodPattern) {
    final PsiNamePatternCondition nameCondition = ContainerUtil.findInstance(methodPattern.getCondition().getConditions(), PsiNamePatternCondition.class);

    return with(new PatternCondition<T>("methodCallParameter") {
      @Override
      public boolean accepts(final @NotNull T literal, final ProcessingContext context) {
        final PsiElement parent = literal.getParent();
        if (parent instanceof PsiExpressionList) {
          return hasIndex(literal, index) && checkCall(context, (PsiExpressionList)parent, methodPattern, nameCondition);
        }
        return false;
      }

      private boolean hasIndex(@NotNull T literal, int index) {
        int currentIndex = 0;
        PsiElement each = literal;
        while (each != null) {
          each = each.getPrevSibling();
          if (each instanceof PsiExpression) {
            currentIndex++;
            if (currentIndex > index) return false;
          }
        }

        if (currentIndex != index) return false;
        return true;
      }
    });
  }

  public Self methodCallParameter(final @NotNull ElementPattern<? extends PsiMethod> methodPattern) {
    final PsiNamePatternCondition nameCondition = ContainerUtil.findInstance(methodPattern.getCondition().getConditions(), PsiNamePatternCondition.class);

    return with(new PatternCondition<T>("methodCallParameter") {
      @Override
      public boolean accepts(final @NotNull T literal, final ProcessingContext context) {
        final PsiElement parent = literal.getParent();
        return parent instanceof PsiExpressionList && checkCall(context, (PsiExpressionList)parent, methodPattern, nameCondition);
      }
    });
  }

  private static boolean checkCall(ProcessingContext context,
                                   PsiExpressionList psiExpressionList,
                                   ElementPattern<? extends PsiMethod> methodPattern,
                                   @Nullable PsiNamePatternCondition nameCondition) {
    final PsiElement element = psiExpressionList.getParent();
    if (element instanceof PsiMethodCallExpression) {
      PsiReferenceExpression methodExpression = ((PsiMethodCallExpression)element).getMethodExpression();
      if (nameCondition != null && !nameCondition.getNamePattern().accepts(methodExpression.getReferenceName())) {
        return false;
      }

      for (JavaResolveResult result : methodExpression.multiResolve(false)) {
        if (methodPattern.accepts(result.getElement(), context)) {
          return true;
        }
      }
    }
    return false;
  }

  public Self constructorParameter(final int index, final String... fqns) {
    return with(new PatternCondition<T>("constructorParameter") {
      @Override
      public boolean accepts(final @NotNull T literal, final ProcessingContext context) {
        final PsiElement parent = literal.getParent();
        if (parent instanceof PsiExpressionList) {
          final PsiExpressionList psiExpressionList = (PsiExpressionList)parent;
          final PsiExpression[] psiExpressions = psiExpressionList.getExpressions();
          if (!(psiExpressions.length > index && psiExpressions[index] == literal)) return false;

          final PsiElement element = psiExpressionList.getParent();
          if (element instanceof PsiNewExpression) {
            PsiJavaCodeReferenceElement reference = ((PsiNewExpression)element).getClassOrAnonymousClassReference();
            if (reference != null) {
              String qualifiedName = reference.getQualifiedName();
              return ArrayUtil.contains(qualifiedName, fqns);
            }
          }
        }
        return false;
      }
    });
  }

  public static class Capture<T extends PsiElement> extends PsiJavaElementPattern<T, Capture<T>> {
    public Capture(final Class<T> aClass) {
      super(aClass);
    }

    public Capture(final @NotNull InitialPatternCondition<T> condition) {
      super(condition);
    }
  }



}