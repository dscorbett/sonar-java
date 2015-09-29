/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2391",
  name = "JUnit framework methods should be declared properly",
  priority = Priority.CRITICAL,
  tags = {"bug", "junit"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("5min")
public class JunitMethodDeclarationCheck extends SubscriptionBaseVisitor {

  private static final String JUNIT_SETUP = "setUp";
  private static final String JUNIT_TEARDOWN = "tearDown";
  private static final String JUNIT_SUITE = "suite";
  private static final int MAX_STRING_DISTANCE = 3;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isJunit3Class((ClassTree) tree)) {
      for (Tree member : ((ClassTree) tree).members()) {
        if (member.is(Tree.Kind.METHOD)) {
          visitMethod((MethodTree) member);
        }
      }
    }
  }

  private void visitMethod(MethodTree methodTree) {
    String name = methodTree.simpleName().name();
    TypeTree returnType = methodTree.returnType();
    if (JUNIT_SETUP.equals(name) || JUNIT_TEARDOWN.equals(name)) {
      checkSetupTearDownSignature(methodTree);
    } else if (JUNIT_SUITE.equals(name)) {
      checkSuiteSignature(methodTree);
    } else if ((returnType != null && returnType.symbolType().isSubtypeOf("junit.framework.Test")) || areVerySimilarStrings(JUNIT_SUITE, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_SUITE, name);
    } else if (areVerySimilarStrings(JUNIT_SETUP, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_SETUP, name);
    } else if (areVerySimilarStrings(JUNIT_TEARDOWN, name)) {
      addIssueForMethodBadName(methodTree, JUNIT_TEARDOWN, name);
    }
  }

  @VisibleForTesting
  protected boolean areVerySimilarStrings(String expected, String actual) {
    // cut complexity when the strings length difference is bigger than the accepted threshold
    return (Math.abs(expected.length() - actual.length()) <= MAX_STRING_DISTANCE) && StringUtils.getLevenshteinDistance(expected, actual) <= MAX_STRING_DISTANCE;
  }

  private void checkSuiteSignature(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (!symbol.isPublic()) {
      addIssue(methodTree, "Make this method \"public\".");
    } else if (!symbol.isStatic()) {
      addIssue(methodTree, "Make this method \"static\".");
    } else if (!methodTree.parameters().isEmpty()) {
      addIssue(methodTree, "This method does not accept parameters.");
    } else {
      TypeTree returnType = methodTree.returnType();
      if (returnType != null && !returnType.symbolType().isSubtypeOf("junit.framework.Test")) {
        addIssue(methodTree, "This method should return either a \"junit.framework.Test\" or a \"junit.framework.TestSuite\".");
      }
    }
  }

  private void checkSetupTearDownSignature(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    if (!symbol.isPublic()) {
      addIssue(methodTree, "Make this method \"public\".");
    } else if (!methodTree.parameters().isEmpty()) {
      addIssue(methodTree, "This method does not accept parameters.");
    } else {
      TypeTree returnType = methodTree.returnType();
      if (returnType != null && !returnType.symbolType().isVoid()) {
        addIssue(methodTree, "Make this method return \"void\".");
      }
    }
  }

  private void addIssueForMethodBadName(MethodTree methodTree, String expected, String actual) {
    addIssue(methodTree, "This method should be named \"" + expected + "\" not \"" + actual + "\".");
  }

  private static boolean isJunit3Class(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("junit.framework.TestCase");
  }

}
