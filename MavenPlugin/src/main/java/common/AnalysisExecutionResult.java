package common;

import evaluation.MinificationResult;

import java.util.ArrayList;
import java.util.Objects;

public class AnalysisExecutionResult {
    private double avgExecutionTime;
    private ArrayList<MinificationResult> executionResults;

    AnalysisExecutionResult(double avgExecutionTime, ArrayList<MinificationResult> executionResults) {
        this.avgExecutionTime = avgExecutionTime;
        this.executionResults = executionResults;
    }

    double getAvgExecutionTime() {
        return avgExecutionTime;
    }


    ArrayList<MinificationResult> getExecutionResults() {
        return executionResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisExecutionResult that = (AnalysisExecutionResult) o;
        return Double.compare(that.avgExecutionTime, avgExecutionTime) == 0 &&
                executionResults.equals(that.executionResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avgExecutionTime, executionResults);
    }
}
