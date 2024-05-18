package com.example.codequalityanalysis

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CodeQualityAnalysisAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        project?.let {
            val report = StringBuilder()
            val totalCount = countLinesOfCode(project.baseDir)
            val commentCount = countCommentLines(project.baseDir)
            val complexity = calculateCyclomaticComplexity(project.baseDir)
            val maxDeep = calculateMaxNestingDepth(project.baseDir)
            val duplicateLines = calculateDuplicateCode(project.baseDir)

            val commentRatio = if (totalCount > 0) commentCount.toDouble() / totalCount else 0.0

            report.appendLine("Total lines of code: $totalCount")
            report.appendLine("Comment lines: $commentCount")
            report.appendLine("Comment to code ratio: ${String.format("%.2f", commentRatio)}")
            val commentLevel = when {
                commentRatio < 0.05 -> "Few comments to the code length (5-10% of the code length is the minimum number of comments)"
                commentRatio < 0.2 -> "Moderate amount of comments on code length (10-20% of code length)"
                else -> "A large number of comments on the code length (more than 20% of the code length)"
            }
            report.appendLine("Comment level: $commentLevel")

            report.appendLine("Total Cyclomatic Complexity: $complexity")
            val complexityLevel = when {
                complexity < totalCount * 0.05 -> "Low cyclomatic complexity (5-10% of the code length is the minimum complexity)"
                complexity < totalCount * 0.2 -> "Moderate cyclomatic complexity (10-20% of the code length)"
                else -> "High cyclomatic complexity (more than 20% of the code length)"
            }
            report.appendLine("Cyclomatic complexity level: $complexityLevel")

            report.appendLine("Maximum Nesting Depth: $maxDeep")
            val nestingDepthLevel = when {
                maxDeep < totalCount * 0.05 -> "Low nesting depth in loops (5-10% of the code length is the minimum nesting depth)"
                maxDeep < totalCount * 0.2 -> "Moderate nesting depth in loops (10-20% of the code length)"
                else -> "High nesting depth in loops (more than 20% of the code length)"
            }
            report.appendLine("Nesting depth level: $nestingDepthLevel")

            report.appendLine("Total Duplicate Lines: $duplicateLines")
            val duplicateLinesLevel = when {
                duplicateLines < totalCount * 0.05 -> "Few duplicated lines in the code (5-10% of the code length is the minimum number of duplicates)"
                duplicateLines < totalCount * 0.2 -> "Moderate amount of duplicated lines in the code (10-20% of the code length)"
                else -> "A large number of duplicated lines in the code (more than 20% of the code length)"
            }
            report.appendLine("Duplicate lines level: $duplicateLinesLevel")
            saveReportToFile(project.baseDir, report.toString())
        }
    }

    private fun saveReportToFile(baseDir: VirtualFile, report: String) {
        try {
            val reportFile = File(baseDir.path, "code_quality_report.txt")
            FileWriter(reportFile).use { writer ->
                writer.write(report)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun countLinesOfCode(file: VirtualFile): Int {
        if (file.isDirectory) {
            return file.children.sumOf { countLinesOfCode(it) }
        } else if (file.extension == "kt") {
            return file.inputStream.reader().useLines { lines ->
                lines.count()
            }
        }
        return 0
    }

    private fun calculateCyclomaticComplexity(file: VirtualFile): Int {
        if (file.isDirectory) {
            return file.children.sumOf { calculateCyclomaticComplexity(it) }
        } else if (file.extension == "kt") {
            return file.inputStream.reader().useLines { lines ->
                lines.sumOf { countControlFlowStatements(it) }
            }
        }
        return 0
    }

    private fun countControlFlowStatements(line: String): Int {
        val controlStatements = listOf("if", "else", "when", "for", "while", "catch", "throw")
        return controlStatements.count { statement -> line.contains(statement) }
    }

    private fun calculateMaxNestingDepth(file: VirtualFile): Int {
        if (file.isDirectory) {
            return file.children.maxOfOrNull { calculateMaxNestingDepth(it) } ?: 0
        } else if (file.extension == "kt") {
            var currentDepth = 0
            var maxDepth = 0
            file.inputStream.reader().useLines { lines ->
                lines.forEach { line ->
                    currentDepth += line.count { it == '{' }
                    currentDepth -= line.count { it == '}' }
                    maxDepth = maxOf(maxDepth, currentDepth)
                }
            }
            return maxDepth
        }
        return 0
    }

    private fun calculateDuplicateCode(file: VirtualFile): Int {
        if (file.isDirectory) {
            return file.children.sumOf { calculateDuplicateCode(it) }
        } else if (file.extension == "kt") {
            val lineOccurrences = HashMap<String, Int>()
            var duplicateLinesCount = 0

            file.inputStream.reader().useLines { lines ->
                lines.forEach { line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty()) {
                        val count = lineOccurrences.getOrDefault(trimmedLine, 0)
                        lineOccurrences[trimmedLine] = count + 1
                        if (count == 1) { // Count this line as duplicate once it appears more than once
                            duplicateLinesCount++
                        }
                    }
                }
            }
            return duplicateLinesCount
        }
        return 0
    }

    private fun countCommentLines(file: VirtualFile): Int {
        if (file.isDirectory) {
            return file.children.sumOf { countCommentLines(it) }
        } else if (file.extension == "kt") {
            var commentLines = 0
            var isBlockComment = false
            file.inputStream.reader().use { reader ->
                val text = reader.readText()
                val lines = text.split("\n")
                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (isBlockComment) {
                        commentLines++
                        if (trimmedLine.endsWith("*/")) {
                            isBlockComment = false
                        }
                    } else if (trimmedLine.startsWith("/*")) {
                        isBlockComment = true
                        commentLines++
                    } else if (trimmedLine.startsWith("//")) {
                        commentLines++
                    }
                }
            }
            return commentLines
        }
        return 0
    }
}
