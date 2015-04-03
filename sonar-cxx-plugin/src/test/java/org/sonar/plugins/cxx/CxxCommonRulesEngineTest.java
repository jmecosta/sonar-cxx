/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010 Neticoa SAS France
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
package org.sonar.plugins.cxx;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.commonrules.api.CommonRulesRepository;

/**
 *
 * @author Jorge Costa
 */
public class CxxCommonRulesEngineTest {

  @Test
  public void provide_extensions() {
    CxxCommonRulesEngine engine = new CxxCommonRulesEngine();
    
    RulesDefinitionAnnotationLoader loader = new RulesDefinitionAnnotationLoader();
    CxxRuleRepository def = new CxxRuleRepository(loader);
    RulesDefinition.Context context = new RulesDefinition.Context();
    def.define(context);
    
    assertThat(engine.provide()).isNotEmpty();
  }

  @Test
  public void enable_common_rules() {
    CxxCommonRulesEngine engine = new CxxCommonRulesEngine();
    
    RulesDefinitionAnnotationLoader loader = new RulesDefinitionAnnotationLoader();
    CxxRuleRepository def = new CxxRuleRepository(loader);
    RulesDefinition.Context context = new RulesDefinition.Context();
    def.define(context);    
   
    CommonRulesRepository repo = engine.newRepository();
    assertThat(repo.rules()).hasSize(6);
    assertThat(repo.rule(CommonRulesRepository.RULE_INSUFFICIENT_COMMENT_DENSITY)).isNotNull();
  }
}
