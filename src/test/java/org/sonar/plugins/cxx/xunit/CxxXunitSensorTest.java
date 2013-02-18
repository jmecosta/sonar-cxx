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

package org.sonar.plugins.cxx.xunit;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.cxx.TestUtils;

public class CxxXunitSensorTest {
  private CxxXunitSensor sensor;
  private SensorContext context;
  private Project project;
      
  @Before
  public void setUp() {
    project = TestUtils.mockProject();
    context = mock(SensorContext.class);
  }

  @Test
  public void shouldReportCorrectViolations() {

    sensor = new CxxXunitSensor(new Settings(), TestUtils.mockCxxLanguage());
    sensor.analyse(project, context);

    verify(context, times(3)).saveMeasure((Resource) anyObject(),
                                          eq(CoreMetrics.TESTS), anyDouble());
    verify(context, times(3)).saveMeasure((Resource) anyObject(),
                                          eq(CoreMetrics.SKIPPED_TESTS), anyDouble());
    verify(context, times(3)).saveMeasure((Resource) anyObject(),
                                          eq(CoreMetrics.TEST_ERRORS), anyDouble());
    verify(context, times(3)).saveMeasure((Resource) anyObject(),
                                          eq(CoreMetrics.TEST_FAILURES), anyDouble());
    verify(context, times(2)).saveMeasure((Resource) anyObject(),
                                          eq(CoreMetrics.TEST_SUCCESS_DENSITY), anyDouble());
    verify(context, times(3)).saveMeasure((Resource) anyObject(), any(Measure.class));
  }

  @Test
  public void shouldReportZeroTestWhenNoReportFound() {
    Settings config = new Settings();
    config.setProperty(CxxXunitSensor.REPORT_PATH_KEY, "notexistingpath");
        
    sensor = new CxxXunitSensor(config, TestUtils.mockCxxLanguage());
    
    sensor.analyse(project, context);
    
    verify(context, times(1)).saveMeasure(eq(CoreMetrics.TESTS), eq(0.0));
  }

  @Test(expected=org.sonar.api.utils.SonarException.class)
  public void shouldThrowWhenGivenInvalidTime() {
    Settings config = new Settings();
    config.setProperty(CxxXunitSensor.REPORT_PATH_KEY, "xunit-reports/invalid-time-xunit-report.xml");    

    sensor = new CxxXunitSensor(config, TestUtils.mockCxxLanguage());   
    sensor.analyse(project, context);
  }
  
  @Test(expected=java.net.MalformedURLException.class)
  public void transformReport_shouldThrowWhenGivenNotExistingStyleSheet()
    throws java.io.IOException, javax.xml.transform.TransformerException 
  {
    Settings config = new Settings();
    config.setProperty(CxxXunitSensor.XSLT_URL_KEY, "whatever");      
    sensor = new CxxXunitSensor(config, TestUtils.mockCxxLanguage());
    
    sensor.transformReport(cppunitReport());
  }
  
  @Test
  public void transformReport_shouldTransformCppunitReport()
    throws java.io.IOException, javax.xml.transform.TransformerException 
  {
    Settings config = new Settings();
    config.setProperty(CxxXunitSensor.XSLT_URL_KEY, "cppunit-1.x-to-junit-1.0.xsl");      
      
    sensor = new CxxXunitSensor(config, TestUtils.mockCxxLanguage());
    File reportBefore = cppunitReport();
    
    File reportAfter = sensor.transformReport(reportBefore);
    
    assert(reportAfter != reportBefore);
  }
  
  File cppunitReport() {
    return new File(new File(project.getFileSystem().getBasedir().getPath(), "xunit-reports"),
                    "cppunit-report.xml");
  }
}
