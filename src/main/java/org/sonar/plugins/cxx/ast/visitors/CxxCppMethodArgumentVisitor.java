/*
 * Sonar Cxx Plugin, open source software quality management tool.
 * Copyright (C) 2010 - 2011, Neticoa SAS France - Tous droits reserves.
 * Author(s) : Franck Bonin, Neticoa SAS France.
 *
 * Sonar Cxx Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar Cxx Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar Cxx Plugin; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cxx.ast.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.sonar.plugins.cxx.ast.cpp.CxxClassMethod;
import org.sonar.plugins.cxx.ast.cpp.CxxMethodArgument;
import org.sonar.plugins.cxx.ast.cpp.impl.CppMethodArgument;
import org.sonar.plugins.cxx.utils.CxxUtils;

/**
 * Visits all method arguments and adds them to the method
 *
 * @author Przemyslaw Kociolek
 */
public class CxxCppMethodArgumentVisitor extends ASTVisitor {

  private CxxClassMethod visitedMethod;
  private String parameterType = null;
  private String parameterName = null;

  public CxxCppMethodArgumentVisitor(CxxClassMethod visitingMethod) {
    if (visitingMethod == null) {
      throw new IllegalArgumentException("Method argument visitor can't visit null method.");
    }
    this.visitedMethod = visitingMethod;
    this.shouldVisitParameterDeclarations = true;
    this.shouldVisitDeclSpecifiers = true;
    this.shouldVisitNames = true;
  }

  public int visit(IASTDeclSpecifier node) {
    parameterType = node.getRawSignature();
    return ASTVisitor.PROCESS_SKIP;
  }

  public int visit(IASTName node) {
    parameterName = node.getRawSignature();
    return ASTVisitor.PROCESS_CONTINUE;
  }

  public int leave(IASTParameterDeclaration node) {
    try {
      if (parameterName.equals("") && !parameterType.equals("void")) {
        parameterName = "dummypar";
      }

      CxxMethodArgument argument = new CppMethodArgument(parameterName, parameterType);
      visitedMethod.addArgument(argument);
    } catch (IllegalArgumentException e) {
      CxxUtils.LOG.error("Argument Exception, ignore it: " + e.getMessage());
      CxxUtils.LOG.error("File Name: " + node.getContainingFilename()
              + " ParType: \"" + parameterType + "\""
              + " ParName: \"" + parameterName + "\"");
    }
    return ASTVisitor.PROCESS_ABORT;
  }
}
