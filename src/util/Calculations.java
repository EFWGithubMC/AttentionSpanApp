package util;

public class Calculations {
    private static int successfulResponses = 0;
    private static int totalResponses = 0;
    private static double accuracyPercentage = 0.0;
    private static long totalCorrectGoResponseTimeMs = 0L;
    private static int correctGoResponses = 0;
    private static double averageCorrectGoResponseTimeMs = 0.0;

    private Calculations() {
    }

    public static void recordResponse(boolean successful) {
        totalResponses++;
        if (successful) {
            successfulResponses++;
        }
        accuracyPercentage = ((double) successfulResponses / totalResponses) * 100.0;
    }

    public static void recordCorrectGoResponseTime(long responseTimeMs) {
        totalCorrectGoResponseTimeMs += responseTimeMs;
        correctGoResponses++;
        averageCorrectGoResponseTimeMs = (double) totalCorrectGoResponseTimeMs / correctGoResponses;
    }

    public static double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public static double getAverageCorrectGoResponseTimeMs() {
        return averageCorrectGoResponseTimeMs;
    }
}
