/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2011 Waleri Enns and CONTACT Software GmbH
 * dev@sonar.codehaus.org
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
package org.sonar.cxx.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.check.Cardinality; //@todo: deprecated, see http://javadocs.sonarsource.org/4.5.2/apidocs/deprecated-list.html
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squidbridge.checks.AbstractXPathCheck;

import com.sonar.sslr.api.Grammar;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.WildcardPattern;

@Rule(
  key = "XPath",
  priority = Priority.MAJOR,
  cardinality = Cardinality.MULTIPLE)
public class XPathCheck extends AbstractXPathCheck<Grammar> {

  private static final String DEFAULT_MATCH_FILE_PATTERN = "";
  private static final String DEFAULT_XPATH_QUERY = "";
  private static final String DEFAULT_MESSAGE = "The XPath expression matches this piece of code";

  @RuleProperty(
    key = "matchFilePattern",
    defaultValue = DEFAULT_MATCH_FILE_PATTERN)
  public String matchFilePattern = DEFAULT_MATCH_FILE_PATTERN;

  @RuleProperty(
    key = "xpathQuery",
    type = "TEXT",
    defaultValue = DEFAULT_XPATH_QUERY)
  public String xpathQuery = DEFAULT_XPATH_QUERY;

  @RuleProperty(
    key = "message",
    defaultValue = DEFAULT_MESSAGE)
  public String message = DEFAULT_MESSAGE;

  @Override
  public String getXPathQuery() {
    return xpathQuery;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public void visitFile(AstNode fileNode) {
    if (fileNode != null) {
      if (!matchFilePattern.isEmpty()) {
        WildcardPattern pattern = WildcardPattern.create(matchFilePattern);
        String path = PathUtils.sanitize(getContext().getFile().getPath());
        if (!pattern.match(path)) {
          return;
        }
      }
      super.visitFile(fileNode);
    }
  }

}
