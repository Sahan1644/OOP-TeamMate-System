package com.teamMate.io;

import com.teamMate.model.Participant;
import com.teamMate.core.PersonalityClassifier;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * CSV reading/writing.
 * Supports two CSV formats (flexible):
 * 1) Sample: ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType
 * 2) Extended: ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,Q1,Q2,Q3,Q4,Q5
 */
public class CSVHandler {

    public static List<Participant> readParticipants(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) throw new FileNotFoundException("CSV not found: " + csvPath);
        List<String> lines = Files.readAllLines(csvPath);
        List<Participant> result = Collections.synchronizedList(new ArrayList<>());

        int start = 0;
        if (!lines.isEmpty() && lines.get(0).toLowerCase().contains("id")) start = 1;

        ExecutorService exec = Executors.newFixedThreadPool(Math.min(4, Math.max(1, lines.size())));
        List<Future<?>> futures = new ArrayList<>();

        for (int i = start; i < lines.size(); i++) {
            final String line = lines.get(i);
            futures.add(exec.submit(() -> {
                try {
                    Participant p = parseLine(line);
                    if (p != null) result.add(p);
                } catch (Exception ex) {
                    System.err.println("Failed to parse CSV line: " + ex.getMessage() + " -> " + line);
                }
            }));
        }

        // wait
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        exec.shutdown();
        return result;
    }

    private static Participant parseLine(String line) {
        String[] parts = line.split(",", -1);
        // Accept both 8-col and 11-col variants
        if (parts.length < 8) throw new IllegalArgumentException("Expect at least 8 columns, got " + parts.length);

        String id = parts[0].trim();
        String name = parts[1].trim();
        String email = parts.length > 2 ? parts[2].trim() : "";
        String game = parts.length > 3 ? normalizeGame(parts[3].trim()) : "Other";
        int skill = parts.length > 4 ? safeParse(parts[4].trim(), 5, 1, 10) : 5;
        String role = parts.length > 5 ? normalizeRole(parts[5].trim()) : "Other";

        int personalityScore = 0;
        String personalityType = "";

        if (parts.length >= 8) {
            // column 6 may be PersonalityScore or Q1 depending on format
            if (parts.length == 8) {
                // format: ... ,PersonalityScore,PersonalityType
                personalityScore = safeParse(parts[6].trim(), 50, 0, 100);
                personalityType = parts[7].trim().isEmpty() ? PersonalityClassifier.classify(personalityScore) : parts[7].trim();
            } else if (parts.length >= 11) {
                // format includes Q1..Q5 at positions 6..10
                int q1 = safeParse(parts[6].trim(), 3, 1, 5);
                int q2 = safeParse(parts[7].trim(), 3, 1, 5);
                int q3 = safeParse(parts[8].trim(), 3, 1, 5);
                int q4 = safeParse(parts[9].trim(), 3, 1, 5);
                int q5 = safeParse(parts[10].trim(), 3, 1, 5);
                personalityScore = PersonalityClassifier.scaledScore(q1, q2, q3, q4, q5);
                personalityType = PersonalityClassifier.classify(personalityScore);
            } else {
                // fallback
                personalityScore = safeParse(parts[6].trim(), 50, 0, 100);
                personalityType = PersonalityClassifier.classify(personalityScore);
            }
        }

        return new Participant(id, name, email, game, role, skill, personalityScore, personalityType);
    }

    private static int safeParse(String s, int defaultVal, int min, int max) {
        try {
            int v = Integer.parseInt(s);
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (Exception e) { return defaultVal; }
    }

    private static String normalizeGame(String s) {
        if (s == null || s.isEmpty()) return "Other";
        String v = s.toLowerCase();
        if (v.contains("valor")) return "Valorant";
        if (v.contains("dota")) return "DOTA 2";
        if (v.contains("fifa")) return "FIFA";
        if (v.contains("basket")) return "Basketball";
        if (v.contains("badm")) return "Badminton";
        if (v.contains("chess")) return "Chess";
        if (v.contains("cs")) return "CS:GO";
        return s;
    }

    private static String normalizeRole(String s) {
        if (s == null || s.isEmpty()) return "Other";
        String v = s.toLowerCase();
        if (v.contains("strate")) return "Strategist";
        if (v.contains("attack")) return "Attacker";
        if (v.contains("defend")) return "Defender";
        if (v.contains("support")) return "Supporter";
        if (v.contains("coord")) return "Coordinator";
        return s;
    }

    public static void writeTeams(Path outPath, List<com.teamMate.core.Team> teams) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outPath)) {
            bw.write("teamId,memberId,memberName,email,game,role,skill,personality,personalityScore\n");
            for (com.teamMate.core.Team t : teams) {
                for (Participant p : t.getMembers()) {
                    bw.write(String.format("%d,%s,%s,%s,%s,%s,%d,%s,%d\n",
                            t.getTeamId(),
                            p.getId(),
                            p.getName().replace(",", " "),
                            p.getEmail(),
                            p.getGame(),
                            p.getRole(),
                            p.getSkillRating(),
                            p.getPersonalityType(),
                            p.getPersonalityScore()
                    ));
                }
            }
        }
    }
}
