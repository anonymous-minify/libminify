package evaluation

import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import analysis.config.AnalysisConfig
import net.sf.nervalreports.core.{ReportColors, ReportFontSize, ReportTextAlignment}
import net.sf.nervalreports.generators.HTMLReportGenerator
import org.opalj.ai.PC
import org.opalj.br.{ClassFile, Method}
import slicing.{SlicingFailure, SlicingResult, SlicingSuccess}

/*
  This object analyses the results and creates a report that contains important information like the removed instructions and details about slicing failures
 */
object ResultAnalyzer {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss")

  def analyzeClassFiles(analysisConfig: AnalysisConfig, originalClassFiles: Iterable[ClassFile], modifiedClassFiles: Iterable[ClassFile], deadInstructionsPerMethod: Map[Method, Iterable[PC]], slicingResults: Iterable[SlicingResult], printDeadInstructions: Boolean = false): Unit = {
    val classComparisons = originalClassFiles.map(o => new ClassComparison(o, modifiedClassFiles.find(c => c.fqn == o.fqn).getOrElse(o)))
    createReport(analysisConfig, classComparisons, deadInstructionsPerMethod, slicingResults, printDeadInstructions)
  }

  /**
    * This method creates a report based on modified class files, dead instructions and slicing failures
    */
  def createReport(analysisConfig: AnalysisConfig, classComparisons: Iterable[ClassComparison], deadInstructionsPerMethod: Map[Method, Iterable[PC]], slicingResults: Iterable[SlicingResult], printDeadInstructions: Boolean): Unit = {
    val reportGenerator = new HTMLReportGenerator()
    val modifiedClassFiles = classComparisons.map(c => c.modifiedClass)
    val originalClassFiles = classComparisons.map(c => c.originalClass)
    reportGenerator.beginDocument()
    reportGenerator.beginDocumentHead()
    val gray = reportGenerator.addColor(192, 192, 192, "gray")
    reportGenerator.endDocumentHead()
    reportGenerator.beginDocumentBody()

    //generate footer and header
    reportGenerator.beginPageHeaderCenter()
    reportGenerator.setFontSize(ReportFontSize.LARGE)
    reportGenerator.setBold(true)
    reportGenerator.addTextLine(s"Minification Report - ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
    reportGenerator.setFontSize(ReportFontSize.NORMAL)
    reportGenerator.setBold(false)
    reportGenerator.addTextLine(s"Domain: ${analysisConfig.domainType}")
    reportGenerator.addTextLine(s"Max. callchain length: ${analysisConfig.callChainLength}")
    reportGenerator.addTextLine(s"Average reduction in method count: ${1 - modifiedClassFiles.map(c => c.methods.size).sum.toDouble / originalClassFiles.map(c => c.methods.size).sum}")
    if (classComparisons.nonEmpty) {
      val maxReductionForClass = classComparisons.maxBy(c => c.methodCountReduction())
      reportGenerator.addTextLine(s"Maximum reduction in method count is ${maxReductionForClass.methodCountReduction()} for class ${maxReductionForClass.originalClass.fqn}")
    }
    reportGenerator.addSeparatorLine()
    reportGenerator.endPageHeaderCenter()

    reportGenerator.addLineBreak()
    reportGenerator.setTableBorderStyle(.5f, ReportColors.BLACK)
    for (classComparison <- classComparisons) {
      var relevantMethods: Iterable[MethodComparison] = null
      if (!printDeadInstructions) {
        relevantMethods = classComparison.instructionReductionByMethod().filter(t => t.instructionReduction() > 0)
      } else {
        relevantMethods = classComparison.instructionReductionByMethod().filter(t => deadInstructionsPerMethod.contains(t.originalMethod) && deadInstructionsPerMethod(t.originalMethod).nonEmpty)
      }

      if (relevantMethods.nonEmpty) {
        reportGenerator.beginTable(2)
        reportGenerator.beginTableHeaderRow()
        reportGenerator.setBold(true)
        reportGenerator.addTableHeaderCell(classComparison.originalClass.thisType.toJava, 2)
        reportGenerator.endTableHeaderRow()

        reportGenerator.setBold(false)
        for (methodComparison <- relevantMethods if methodComparison.modifiedMethod.body.isDefined && methodComparison.originalMethod.body.isDefined) {
          val slicingResultOpt = slicingResults.collect { case t: SlicingSuccess => t }.find(p => p.originalMethod.eq(methodComparison.originalMethod))
          reportGenerator.setTextAlignment(ReportTextAlignment.CENTER)
          reportGenerator.setRowColor(gray)
          reportGenerator.beginTableRow()
          reportGenerator.beginTableCell(2, 1)
          reportGenerator.setItalic(true)
          reportGenerator.addText(s"${methodComparison.originalMethod.name} (Reduction: ${methodComparison.instructionReductionPercentage})")
          reportGenerator.endTableCell()
          reportGenerator.endTableRow()
          reportGenerator.setItalic(false)
          reportGenerator.setRowColor(ReportColors.WHITE)
          reportGenerator.setTextAlignment(ReportTextAlignment.LEFT)

          //add methods strings
          reportGenerator.beginTableRow()
          reportGenerator.beginTableCell()
          val modifiedCode = methodComparison.modifiedMethod.body.get
          val originalCode = methodComparison.originalMethod.body.get
          val removedInstructions = deadInstructionsPerMethod(methodComparison.originalMethod).toList
          for ((pc, instr) <- originalCode) {
            if (removedInstructions.contains(pc)) {
              reportGenerator.setTextColor(ReportColors.RED)
              reportGenerator.addTextLine(s"$pc: ${instr.toString()}")
              reportGenerator.setTextColor(ReportColors.DEFAULT_TEXT_COLOR)
            } else
              reportGenerator.addTextLine(s"$pc: ${instr.toString()}")
          }

          reportGenerator.endTableCell()
          reportGenerator.beginTableCell()
          slicingResultOpt match {
            case Some(SlicingSuccess(_, _, Some(newPCtoOldPCMap))) =>
              val modifiedInstructionPCs = modifiedCode.associateWithIndex().filter(t => !originalCode.instructions(newPCtoOldPCMap(t._1)).equals(t._2)).map(t => t._1).toList
              for ((pc, instr) <- modifiedCode) {
                if (modifiedInstructionPCs.contains(pc)) {
                  reportGenerator.setTextColor(ReportColors.GREEN)
                  reportGenerator.addTextLine(s"$pc: ${
                    instr.toString()
                  }")
                  reportGenerator.setTextColor(ReportColors.DEFAULT_TEXT_COLOR)
                } else {
                  reportGenerator.addTextLine(s"$pc: ${
                    instr.toString()
                  }")
                }
              }
            case _ =>
          }
          reportGenerator.endTableCell()
          reportGenerator.endTableRow()
        }

        reportGenerator.endTable()
        reportGenerator.addLineBreak()
      }
    }

    // include slicing failures in report
    reportGenerator.addTextLine("Slicing Failures")
    val slicingFailures = slicingResults.collect { case t: SlicingFailure => t }
    for (slicingResult <- slicingFailures) {
      reportGenerator.beginTable(2)
      reportGenerator.beginTableHeaderRow()
      reportGenerator.setBold(true)
      reportGenerator.addTableHeaderCell(slicingResult.originalMethod.toJava, 2)
      reportGenerator.endTableHeaderRow()
      reportGenerator.beginTableRow()
      reportGenerator.beginTableCell()
      reportGenerator.addText("Original method")
      reportGenerator.endTableCell()
      reportGenerator.beginTableCell()
      reportGenerator.addText("Sliced instructions")
      reportGenerator.setBold(false)
      reportGenerator.endTableCell()
      reportGenerator.endTableRow()

      val modifiedCode = slicingResult.instructionList.toIndexedSeq
      val originalCode = slicingResult.originalMethod.body.get

      reportGenerator.beginTableRow()
      reportGenerator.beginTableCell()
      for ((pc, instr) <- originalCode) {
        if (slicingResult.deadInstructions.exists(t => t._1 == pc)) {
          reportGenerator.setTextColor(ReportColors.RED)
          reportGenerator.addTextLine(s"$pc: ${instr.toString()}")
          reportGenerator.setTextColor(ReportColors.DEFAULT_TEXT_COLOR)
        } else
          reportGenerator.addTextLine(s"$pc: ${instr.toString()}")
      }
      reportGenerator.endTableCell()

      reportGenerator.beginTableCell()
      for (index <- modifiedCode.indices) {
        val instr = modifiedCode(index)
        if (instr != null) {
          reportGenerator.addTextLine(s"$index: ${instr.toString()}")
        }
      }
      reportGenerator.endTableCell()
      reportGenerator.endTableRow()

      reportGenerator.endTable()
      reportGenerator.addLineBreak()
    }

    reportGenerator.endDocumentBody()
    reportGenerator.endDocument()

    val resultPath = Paths.get(analysisConfig.resultPath, "Report-" + dateFormatter.format(LocalDateTime.now()) + ".html").toString
    reportGenerator.saveToFile(resultPath)
  }
}
