/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.JWarning;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1656")
public class SelfAssignementCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Remove or correct this useless self-assignment.";
  private final Set<JWarning> warnings = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.COMPILATION_UNIT, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings.clear();
      warnings.addAll(((JavaTree.CompilationUnitTreeImpl) tree).warnings(JWarning.Type.ASSIGNMENT_HAS_NO_EFFECT));
      return;
    }
    AssignmentExpressionTree node = (AssignmentExpressionTree) tree;
    if (SyntacticEquivalence.areEquivalent(node.expression(), node.variable())) {
      SyntaxToken reportTree = node.operatorToken();
      reportIssue(reportTree, ISSUE_MESSAGE);
      updateWarnings(reportTree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      warnings.forEach(warning -> ((DefaultJavaFileScannerContext) context).reportIssue(this, warning, ISSUE_MESSAGE));
    }
  }

  private void updateWarnings(SyntaxToken reportTree) {
    for (Iterator<JWarning> iterator = warnings.iterator(); iterator.hasNext();) {
      JWarning warning = iterator.next();
      if (warning.contains(reportTree)) {
        iterator.remove();
      }
    }
  }
}
