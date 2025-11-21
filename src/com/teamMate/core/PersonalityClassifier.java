package com.teamMate.core;

public class PersonalityClassifier {

    // Calculate scaled score (5-25 -> *4 => 20-100)
    public static int scaledScore(int q1, int q2, int q3, int q4, int q5) {
        int total = q1 + q2 + q3 + q4 + q5; // 5..25
        return total * 4;
    }

    public static String classify(int scaledScore) {
        if (scaledScore >= 90 && scaledScore <= 100) return "Leader";
        if (scaledScore >= 70 && scaledScore <= 89) return "Balanced";
        if (scaledScore >= 50 && scaledScore <= 69) return "Thinker";
        return "Unknown";
    }
}
