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
package org.sonar.plugins.cxx.bullseyecomplexity;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import static org.sonar.api.utils.ParsingUtils.scaleValue;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;
import org.sonar.plugins.cxx.utils.CxxReportSensor;
import org.sonar.plugins.cxx.utils.CxxUtils;

/**
 * {@inheritDoc}CxxBullseyeComplexitySensor
 */
public class CxxBullseyeComplexitySensor extends CxxReportSensor {

  private Settings conf = null;
  public static final String REPORT_PATH_KEY = "sonar.cxx.bulleyescomplexity.reportPath";
  public static final String DEFAULT_CALC_COMP = "false";
  private static final String DEFAULT_REPORT_PATH = "bullseyecoverage-reports/bullseyecoverage-result-*.xml";
  private static final Number[] METHODS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12};
  private static final Number[] FILE_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};
  private static final Number[] CLASS_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};
  Map<String, FileDataComplexity> filesCCN;

  public CxxBullseyeComplexitySensor(Settings conf) {
    this.conf = conf;
    filesCCN = new HashMap<String, FileDataComplexity>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void analyse(Project project, SensorContext context) {

    try {


      List<File> reports = getReports(conf, project.getFileSystem().getBasedir().getPath(),
          REPORT_PATH_KEY, DEFAULT_REPORT_PATH);
      for (File report : reports) {
        CxxUtils.LOG.debug("Parse Bullseye Report: '{}'", report);
        FileInputStream file = new FileInputStream(report);
        parseReport(project, file, context, false);
      }

      if (reports.size() > 0) {
        // save complexity measures - only for unit tests
        for (FileDataComplexity fileData : filesCCN.values()) {
          saveComplexityMetrics(project, context, fileData);
        }
      }
    } catch (IOException ex) {
      CxxUtils.LOG.error("Error e=", ex);
    }
  }

  private void saveComplexityMetrics(Project project, SensorContext context, FileDataComplexity fileData) {
    String filePath = fileData.getName().getKey();

    if (context.getResource(fileData.getName()) != null) {
      CxxUtils.LOG.debug("Saving complexity measures for file '{}'", filePath);

      RangeDistributionBuilder complexityMethodsDistribution =
          new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION,
          METHODS_DISTRIB_BOTTOM_LIMITS);
      RangeDistributionBuilder complexityFileDistribution =
          new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION,
          FILE_DISTRIB_BOTTOM_LIMITS);
      RangeDistributionBuilder complexityClassDistribution =
          new RangeDistributionBuilder(CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION,
          CLASS_DISTRIB_BOTTOM_LIMITS);

      complexityFileDistribution.add(fileData.getComplexity());
      for (ClassData classData : fileData.getClasses()) {
        complexityClassDistribution.add(classData.getComplexity());
        for (Integer complexity : classData.getMethodComplexities()) {
          complexityMethodsDistribution.add(complexity);
        }
      }

      try {
        context.saveMeasure(fileData.getName(), CoreMetrics.FUNCTIONS, (double) fileData.getNoMethods());
        context.saveMeasure(fileData.getName(), CoreMetrics.COMPLEXITY, (double) fileData.getComplexity());
        context.saveMeasure(fileData.getName(), complexityMethodsDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
        context.saveMeasure(fileData.getName(), complexityClassDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
        context.saveMeasure(fileData.getName(), complexityFileDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
      } catch (org.sonar.api.utils.SonarException e) {
        CxxUtils.LOG.debug("Already added - Ignoring complexity measures for file '{}'", filePath);
      }
    } else {
      CxxUtils.LOG.debug("Ignoring complexity measures for file '{}'", filePath);
    }
  }

  private double calculateCoverage(int coveredElements, int elements) {
    if (elements > 0) {
      return scaleValue(100.0 * ((double) coveredElements / (double) elements));
    }
    return 0.0;
  }

  private class FileData {

    CoverageMeasuresBuilder fileMeasuresBuilder = CoverageMeasuresBuilder.create();

    FileData(String name) {
      FileName = name;
    }

    private void CollectMeasureFromProbes(int lineId, List<Probe> SameLineProbes) {
      int totalconditions = 0;
      int totalcoveredconditions = 0;
      int totaldecisions = 0;
      int totalcovereddecisions = 0;
      // if total decision == 1then set coditions also to 2
      for (Probe p : SameLineProbes) {
        if (p.getKind().equalsIgnoreCase("function")
            || p.getKind().equalsIgnoreCase("decision")
            || p.getKind().equalsIgnoreCase("switch-label")) {

          if (p.getKind().equalsIgnoreCase("function") || p.getKind().equalsIgnoreCase("switch-label")) {
            if (p.getKind().equalsIgnoreCase("function")) { // to update global coverage
            }
            if (p.getEvent().equalsIgnoreCase("full")) {
              fileMeasuresBuilder.setHits(p.geLine(), 1);
              if (p.getKind().equalsIgnoreCase("function")) {  // to update global coverage
              }
            } else {
              fileMeasuresBuilder.setHits(p.geLine(), 0);
            }
          }
          if (p.getKind().equalsIgnoreCase("decision")) {
            totaldecisions += 1;
            if (p.getEvent().equalsIgnoreCase("full")) {
              totalcovereddecisions = 2;
            }
            if (p.getEvent().equalsIgnoreCase("true") || p.getEvent().equalsIgnoreCase("false")) {
              totalcovereddecisions = 1;
            }
            if (p.getEvent().equalsIgnoreCase("none")) {
              totalcovereddecisions = 0;
            }
          }
        } else {
          if (p.getKind().equalsIgnoreCase("condition")) {
            totalconditions += 2;
            if (p.getEvent().equalsIgnoreCase("full")) {
              totalcoveredconditions += 2;
            }
            if (p.getEvent().equalsIgnoreCase("true") || p.getEvent().equalsIgnoreCase("false")) {
              totalcoveredconditions += 1;
            }
            if (p.getEvent().equalsIgnoreCase("none")) {
              totalcoveredconditions += 0;
            }
          } else {
            CxxUtils.LOG.error("KIND NOT FOUND - Probe data : {} - {}", p.line, p.kind);
          }
        }
      }

      if (totaldecisions > 0) {
        if (totalcovereddecisions == 0 && totalcoveredconditions == 0) {
          fileMeasuresBuilder.setHits(lineId, 0);
        } else {
          fileMeasuresBuilder.setHits(lineId, 1);
        }

        if (totalconditions > 0) {
          fileMeasuresBuilder.setConditions(lineId, totalconditions, totalcoveredconditions);
        } else {
          fileMeasuresBuilder.setConditions(lineId, 2, totalcovereddecisions);
        }
      }
    }

    private class Probe {

      Integer line;
      String kind;
      String event;

      public Integer geLine() {
        return line;
      }

      public String getKind() {
        return kind;
      }

      public String getEvent() {
        return event;
      }

      Probe(Integer line, String kind, String event) {
        this.line = line;
        this.kind = kind;
        this.event = event;
      }
    }

    public void saveMetric(Project project, SensorContext context, boolean isIntCov) {

      org.sonar.api.resources.File file =
          org.sonar.api.resources.File.fromIOFile(new File(FileName), project);

      if (context.getResource(file) != null) {
        Object t[] = {FileName};

        Measure Lines = context.getMeasure(file, CoreMetrics.LINES);
        if (Lines != null) {
          CxxUtils.LOG.debug("Resource : {}", file);
          CxxUtils.LOG.debug("METRIC LINES : {}", Lines);
          CxxUtils.LOG.debug("METRIC LINES : {}", Lines.getData());
          CxxUtils.LOG.debug("METRIC LINES : {}", Lines.toString());

          Lines = context.getMeasure(file, CoreMetrics.NCLOC_DATA);
          if (Lines != null) {
            CxxUtils.LOG.debug("NCLOC_DATA LINES : {}", Lines);
            CxxUtils.LOG.debug("NCLOC_DATA LINES : {}", Lines.getData());
            CxxUtils.LOG.debug("NCLOC_DATA LINES : {}", Lines.toString());
          }
          Lines = context.getMeasure(file, CoreMetrics.COMMENT_LINES_DATA);
          if (Lines != null) {
            CxxUtils.LOG.debug("COMMENT_LINES_DATA LINES : {}", Lines);
            CxxUtils.LOG.debug("COMMENT_LINES_DATA LINES : {}", Lines.getData());
            CxxUtils.LOG.debug("COMMENT_LINES_DATA LINES : {}", Lines.toString());
          }

        }

        for (Map.Entry<String, List<Probe>> e : ComplexityPerMethod.entrySet()) { // per function

          List<SrcDecisionPoint> decisionPoints = new ArrayList<SrcDecisionPoint>();
          List<Probe> currentProbeList = e.getValue();
          List<Probe> SameLineProbes = new ArrayList<Probe>();

          Probe prev = currentProbeList.get(0);

          for (Probe pcurr : currentProbeList) { // per probe

            // start a new decision point
            SrcDecisionPoint decisionPoint = SrcDecisionPoint.createDecisionPoint(pcurr.kind, pcurr.event, pcurr.line);
            if (decisionPoint != null) {
              decisionPoints.add(decisionPoint);
            }

            // merge consecutive lines
            if (!prev.line.equals(pcurr.line) || currentProbeList.size() == 1 || pcurr == currentProbeList.get(currentProbeList.size() - 1)) {
              boolean currWasAdded = false;
              if (currentProbeList.size() == 1 || (pcurr == currentProbeList.get(currentProbeList.size() - 1) && prev.line.equals(pcurr.line))) {
                // add current element for analysis
                SameLineProbes.add(pcurr);
                currWasAdded = true;
              }
              CollectMeasureFromProbes(prev.geLine(), SameLineProbes);

              // reset lines
              SameLineProbes.clear();

              // check for the last element and see if it was added already
              if (pcurr == currentProbeList.get(currentProbeList.size() - 1) && !currWasAdded) {
                SameLineProbes.add(pcurr);
                CollectMeasureFromProbes(pcurr.geLine(), SameLineProbes);
                // reset lines
                SameLineProbes.clear();
              }
            }

            // add current element for analysis
            SameLineProbes.add(pcurr);

            // save previous                         
            prev = pcurr;

          } // end current function

          // ESTIMATE COMPLEXITY PER FUNCTION
          collectFunction(e.getKey(), getComplexity(decisionPoints), file);

        } // end current file

      } else {
        Object t[] = {FileName};
        CxxUtils.LOG.warn("File not managed : {}", t);
      }
    }

    public void addProbe(String funcName, Integer line, String kind, String event) {
      List<Probe> data = ComplexityPerMethod.get(funcName);
      if (data == null) {
        data = new LinkedList<Probe>();
        ComplexityPerMethod.put(funcName, data);
      }
      data.add(new Probe(line, kind, event));
    }

    public double getFnCov() {
      return fn_cov;
    }

    public void setFnCov(double fn_cov) {
      this.fn_cov = fn_cov;
    }

    public double getFnTotal() {
      return fn_total;
    }

    public void setFnTotal(double fn_total) {
      this.fn_total = fn_total;
    }

    public double getCdCov() {
      return cd_cov;
    }

    public void setCdCov(double cd_cov) {
      this.cd_cov = cd_cov;
    }

    public double getCdTotal() {
      return cd_total;
    }

    public void setCdTotal(double cd_total) {
      this.cd_total = cd_total;
    }
    private double fn_cov;
    private double fn_total;
    private double cd_cov;
    private double cd_total;
    private String FileName;
    private Map<String, List<Probe>> ComplexityPerMethod = new HashMap<String, List<Probe>>();
  }

  /**
   * Parse the stream of Bullseyes XML report
   *
   * @param project
   * @param xmlStream - This stream will be closed at the end of this method
   * @param context
   */
  private void parseReport(final Project project, InputStream xmlStream,
      final SensorContext context, final boolean isIntCov) {
    try {
      CxxUtils.LOG.info("parsing Bullseyes XML stream{}");
      StaxParser parser = new StaxParser(
          new StaxParser.XmlStreamHandler() {
        public void stream(SMHierarchicCursor rootCursor)
            throws XMLStreamException {
          try {
            Map<String, FileData> fileDataPerFilename = new HashMap<String, FileData>();
            rootCursor.advance();
            String refPath = rootCursor.getAttrValue("dir");
            collectCoverage2(project, refPath, rootCursor.childElementCursor("folder"), fileDataPerFilename, context);

            for (FileData d : fileDataPerFilename.values()) {
              d.saveMetric(project, context, isIntCov);
            }
          } catch (ParseException e) {
            throw new XMLStreamException(e);
          }
        }
      });
      parser.parse(xmlStream);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    } finally {
      try {
        if (xmlStream != null) {
          xmlStream.close();
        }
      } catch (IOException ex) {
        CxxUtils.LOG.error("Can't close the xml stream", ex);
      }
    }
  }

  private void collectCoverage2(Project project, String refPath, SMInputCursor folder, Map<String, FileData> fileDataPerFilename, SensorContext context) throws ParseException, XMLStreamException {
    LinkedList<String> path = new LinkedList<String>();
    while (folder.getNext() != null) {
      String folderName = folder.getAttrValue("name");
      path.add(folderName);
      recTreeWalk(project, refPath, folder, fileDataPerFilename, context, path);
      path.removeLast();
    }
  }

  private void probWalk(Project project, SMInputCursor prob, Map<String, FileData> fileDataPerFilename, FileData data, SensorContext context, LinkedList<String> path, String funcName)
      throws ParseException, XMLStreamException {
    String line = prob.getAttrValue("line");
    String kind = prob.getAttrValue("kind");
    String event = prob.getAttrValue("event");
    data.addProbe(funcName, Integer.parseInt(line), kind, event);
  }

  private void funcWalk(Project project, SMInputCursor func, Map<String, FileData> fileDataPerFilename, FileData data, SensorContext context, LinkedList<String> path, String funcName) throws ParseException, XMLStreamException {
    SMInputCursor prob = func.childElementCursor();
    while (prob.getNext() != null) {
      String probName = prob.getLocalName();
      if (probName.equalsIgnoreCase("probe")) {
        probWalk(project, prob, fileDataPerFilename, data, context, path, funcName);
      }
    }
  }

  private void fileWalk(Project project, SMInputCursor file, Map<String, FileData> fileDataPerFilename, FileData data, SensorContext context, LinkedList<String> path) throws ParseException, XMLStreamException {
    SMInputCursor func = file.childElementCursor();
    while (func.getNext() != null) {
      String funcName = func.getLocalName();
      String name = func.getAttrValue("name");
      if (funcName.equalsIgnoreCase("fn")) {
        funcWalk(project, func, fileDataPerFilename, data, context, path, name);
      }
    }
  }

  private void recTreeWalk(Project project, String refPath, SMInputCursor folder, Map<String, FileData> fileDataPerFilename, SensorContext context, LinkedList<String> path) throws ParseException, XMLStreamException {
    SMInputCursor child = folder.childElementCursor();
    while (child.getNext() != null) {
      String folderChildName = child.getLocalName();
      String name = child.getAttrValue("name");
      path.add(name);
      if (folderChildName.equalsIgnoreCase("src")) {
        String fileName = "";
        Iterator<String> iterator = path.iterator();
        while (iterator.hasNext()) {
          fileName += "/" + iterator.next();
        }

        String theFileName = refPath + fileName;
        FileData data = fileDataPerFilename.get(theFileName);
        if (data == null) {
          data = new FileData(theFileName);
          data.setFnCov(Double.parseDouble(child.getAttrValue("fn_cov")));
          data.setFnTotal(Double.parseDouble(child.getAttrValue("fn_total")));
          data.setCdCov(Double.parseDouble(child.getAttrValue("cd_cov")));
          data.setCdTotal(Double.parseDouble(child.getAttrValue("cd_total")));
          fileDataPerFilename.put(theFileName, data);
        }
        fileWalk(project, child, fileDataPerFilename, data, context, path);
      } else {
        recTreeWalk(project, refPath, child, fileDataPerFilename, context, path);
      }
      path.removeLast();
    }
  }

  public enum DecisionType {

    TRY("try", 0), CATCH("catch", 1), CASE("switch-label", 1), DECISION("decision", 1), FUNCTION("function", 0);
    public final String name;
    public final int complexity;

    private DecisionType(String name, int complexity) {
      this.name = name;
      this.complexity = complexity;
    }

    public static DecisionType getDecisionCoverType(String value) {
      for (DecisionType eachType : values()) {
        if (eachType.name.equals(value)) {
          return eachType;
        }
      }
      return null;
    }
  }

  public enum DecisionCoverType {

    NONE("none", "<span class='uncoveredmark'>TF</span>", "uncovered", 0, 0), TRUE("true",
    "<span class='coveredmark'>T</span><span class='uncoveredmark'>F</span>", "halfcovered", 1, 0), ONLY_TRUE("true",
    "<span class='coveredmark'>T</span>", "covered", 1, 0), ONLY_FALSE("none", "<span class='uncoveredmark'>T</span>", "uncovered", 0, 0), FALSE(
    "false", "<span class='uncoveredmark'>T</span><span class='coveredmark'>F</span>", "halfcovered", 0, 1), FULL("full",
    "<span class='coveredmark'>TF</span>", "covered", 1, 1), FUNCTION_CALLED("true", "<img src='../images/check_icon.png'>", "covered", 1, 0),
    FUNCTION_UNCALLED("true", "<img src='../images/uncheck_icon.png'>", "uncovered", 0, 1);
    public final String name;
    private final String html;
    private final String lineCss;
    public final int trueCount;
    public final int falseCount;

    private DecisionCoverType(String name, String html, String lineCss, int trueCount, int falseCount) {
      this.name = name;
      this.html = html;
      this.lineCss = lineCss;
      this.trueCount = trueCount;
      this.falseCount = falseCount;

    }

    public static DecisionCoverType getDecisionCoverType(String value) {
      for (DecisionCoverType eachType : values()) {
        if (eachType.name.equals(value)) {
          return eachType;
        }
      }
      return DecisionCoverType.NONE;
    }

    public String getHtml() {
      return this.html;
    }

    public String getLineCss() {
      return this.lineCss;
    }
  }

  public static class SrcDecisionPoint {

    public int line;
    public DecisionCoverType decisionCoverType;
    public DecisionType decisionType;
    public int column;
    public boolean sequence;
    public String name;

    public SrcDecisionPoint() {
    }

    public SrcDecisionPoint(int line, DecisionCoverType decisionCoverType, DecisionType decisionType, String name) {
      this(line, decisionCoverType, decisionType);
      this.name = name;
    }

    public SrcDecisionPoint(int line, DecisionCoverType decisionCoverType, DecisionType decisionType) {
      super();
      this.line = line;
      this.decisionCoverType = decisionCoverType;
      this.decisionType = decisionType;
    }

    public static SrcDecisionPoint createDecisionPoint(String kind, String event, Integer line) {
      SrcDecisionPoint point = null;
      DecisionType decisionType = DecisionType.getDecisionCoverType(kind);
      if (decisionType != null) {
        point = new SrcDecisionPoint();

        point.decisionType = decisionType;
        point.decisionCoverType = DecisionCoverType.getDecisionCoverType(event);
        if (point.decisionType == DecisionType.CASE) {
          point.decisionCoverType = point.decisionCoverType == DecisionCoverType.NONE ? DecisionCoverType.ONLY_FALSE
              : DecisionCoverType.ONLY_TRUE;
        }
        point.line = line;
      }
      return point;
    }

    @Override
    public String toString() {
      return String.format("%d %s %s %d %s", this.line, this.decisionCoverType.name, this.decisionType.name, this.column, this.name);
    }
  }

  public int getComplexity(List<SrcDecisionPoint> decisionPoints) {
    int count = 1;
    for (SrcDecisionPoint decisionPoint : decisionPoints) {
      count += decisionPoint.decisionType.complexity;
    }
    return count;
  }

  private void collectFunction(String fullFuncName, int methodComplexity, org.sonar.api.resources.File cxxfile) {

    String loc[] = fullFuncName.split("::");
    String className = (loc.length > 1) ? loc[0] : "GLOBAL";
    String funcName = (loc.length > 1) ? loc[1] : loc[0];

    if (cxxfile != null) {
      FileDataComplexity fileData = filesCCN.get(cxxfile.getKey());
      if (fileData == null) {
        fileData = new FileDataComplexity(cxxfile);
        filesCCN.put(cxxfile.getKey(), fileData);
      }
      fileData.addMethod(className, funcName, methodComplexity);
    }
  }

  private class FileDataComplexity {

    private org.sonar.api.resources.File name;
    private int noMethods = 0;
    private Map<String, ClassData> classes = new HashMap<String, ClassData>();
    private int complexity = 0;

    FileDataComplexity(org.sonar.api.resources.File name) {
      this.name = name;
    }

    public org.sonar.api.resources.File getName() {
      return name;
    }

    public int getNoMethods() {
      return noMethods;
    }

    public int getComplexity() {
      return complexity;
    }

    /**
     * @return data for classes contained in this file
     */
    public Collection<ClassData> getClasses() {
      return classes.values();
    }

    /**
     * Adds complexity data for a method with given name in a given class
     *
     * @param className Name of method's class
     * @param methodName The name of the method to add data for
     * @param complexity The complexity number to store
     */
    public void addMethod(String className, String methodName, int complexity) {
      noMethods++;
      this.complexity += complexity;

      ClassData classData = classes.get(className);
      if (classData == null) {
        classData = new ClassData();
        classes.put(className, classData);
      }
      classData.addMethod(methodName, complexity);
    }
  }

  private class ClassData {

    private Map<String, Integer> methodComplexities = new HashMap<String, Integer>();
    private int complexity = 0;

    /**
     * Adds complexity data for a method with given name
     *
     * @param name The name of the method to add data for
     * @param complexity The complexity number to store
     */
    public void addMethod(String name, int complexity) {
      this.complexity += complexity;
      methodComplexities.put(name, complexity);
    }

    public Integer getComplexity() {
      return complexity;
    }

    /**
     * @return complexity numbers for methods inside of this class
     */
    public Collection<Integer> getMethodComplexities() {
      return methodComplexities.values();
    }
  }
}
