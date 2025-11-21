package com.teamMate.main;

import com.teamMate.core.TeamBuilder;
import com.teamMate.core.Team;
import com.teamMate.io.CSVHandler;
import com.teamMate.model.Participant;
import com.teamMate.util.ValidationUtil;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final List<String> GAMES = Arrays.asList("Valorant","DOTA 2","FIFA","Basketball","Badminton","CS:GO","Chess","Other");
    private static final List<String> ROLES = Arrays.asList("Strategist","Attacker","Defender","Supporter","Coordinator","Other");

    public static void main(String[] args) {
        System.out.println("=== TeamMate ===");

        List<Participant> participants = new ArrayList<>();

        // initial load prompt
        System.out.print("Load participants CSV (press Enter to skip or type path): ");
        String pth = sc.nextLine().trim();
        if (!pth.isEmpty()) {
            try {
                List<Participant> fromCsv = CSVHandler.readParticipants(Paths.get(pth));
                // deduplicate by id/email
                for (Participant p : fromCsv) {
                    if (ValidationUtil.idExists(participants, p.getId()) || ValidationUtil.emailExists(participants, p.getEmail())) {
                        System.out.println("Skipped duplicate from CSV: " + p.getId() + " / " + p.getEmail());
                    } else {
                        participants.add(p);
                    }
                }
                System.out.println("Loaded from CSV: " + participants.size());
            } catch (Exception e) {
                System.err.println("Failed to load CSV: " + e.getMessage());
            }
        }

        while (true) {
            System.out.println("\nSelect User Type:");
            System.out.println("1. Participant (Member)");
            System.out.println("2. Organizer (Admin)");
            System.out.println("3. Exit");
            System.out.print("> ");
            String opt = sc.nextLine().trim();
            if (opt.equals("1")) participantMenu(participants);
            else if (opt.equals("2")) organizerMenu(participants);
            else if (opt.equals("3")) {
                System.out.println("Goodbye.");
                break;
            } else System.out.println("Invalid option.");
        }
    }

    // ---------------- Participant menu ----------------
    private static void participantMenu(List<Participant> participants) {
        while (true) {
            System.out.println("\n===== PARTICIPANT MENU =====");
            System.out.println("1. Complete Survey");
            System.out.println("2. View My Results");
            System.out.println("3. Edit My Details");
            System.out.println("4. Submit Preferences");
            System.out.println("5. Back");
            System.out.print("> ");
            String opt = sc.nextLine().trim();
            switch (opt) {
                case "1": completeSurvey(participants); break;
                case "2": viewMyResults(participants); break;
                case "3": editDetails(participants); break;
                case "4": submitPreferences(participants); break;
                case "5": return;
                default: System.out.println("Invalid option."); break;
            }
        }
    }

    private static void completeSurvey(List<Participant> participants) {
        System.out.println("\n--- Complete Survey ---");
        String id = promptNonEmpty("Enter your ID (unique): ");
        if (ValidationUtil.idExists(participants, id)) {
            System.out.println("ID already exists. You can edit details instead.");
            return;
        }

        String name = promptNonEmpty("Enter name: ");

        String email = promptNonEmpty("Enter email: ");
        while (!ValidationUtil.isValidEmail(email) || ValidationUtil.emailExists(participants, email)) {
            if (!ValidationUtil.isValidEmail(email)) System.out.println("Invalid email format.");
            else System.out.println("Email already used.");
            email = promptNonEmpty("Enter email: ");
        }

        System.out.println("Select preferred game from list:");
        String game = chooseFromList(GAMES, "Game");

        System.out.println("Select preferred role:");
        String role = chooseFromList(ROLES, "Role");

        int skill = promptIntRange("Enter skill (1-10): ", 1, 10);

        // 5 personality questions
        String[] qs = {
                "I enjoy taking the lead and guiding others during group activities.",
                "I prefer analyzing situations and coming up with strategic solutions.",
                "I work well with others and enjoy collaborative teamwork.",
                "I am calm under pressure and can help maintain team morale.",
                "I like making quick decisions and adapting in dynamic situations."
        };
        int[] answers = new int[5];
        for (int i = 0; i < 5; i++) {
            System.out.println("Q" + (i+1) + ": " + qs[i]);
            answers[i] = promptIntRange("Rate 1 (Strongly Disagree) to 5 (Strongly Agree): ", 1, 5);
        }

        int scaled = com.teamMate.core.PersonalityClassifier.scaledScore(answers[0],answers[1],answers[2],answers[3],answers[4]);
        String pType = com.teamMate.core.PersonalityClassifier.classify(scaled);

        Participant p = new Participant(id, name, email, game, role, skill, scaled, pType);
        participants.add(p);
        System.out.println("Survey submitted. You are classified as: " + pType + " (" + scaled + ")");
    }

    private static void viewMyResults(List<Participant> participants) {
        System.out.print("Enter your ID or email: ");
        String key = sc.nextLine().trim();
        Optional<Participant> found = participants.stream().filter(p -> p.getId().equalsIgnoreCase(key) || p.getEmail().equalsIgnoreCase(key)).findFirst();
        if (found.isPresent()) {
            System.out.println("Your details:\n" + found.get());
        } else System.out.println("Participant not found.");
    }

    private static void editDetails(List<Participant> participants) {
        System.out.print("Enter your ID: ");
        String id = sc.nextLine().trim();
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id)) {
                System.out.println("Editing: " + p);
                String newName = promptOptional("Name ("+p.getName()+"): ");
                if (!newName.isEmpty()) p.setName(newName);

                String newEmail = promptOptional("Email ("+p.getEmail()+"): ");
                if (!newEmail.isEmpty()) {
                    while (!ValidationUtil.isValidEmail(newEmail) || (ValidationUtil.emailExists(participants, newEmail) && !newEmail.equalsIgnoreCase(p.getEmail()))) {
                        if (!ValidationUtil.isValidEmail(newEmail)) System.out.println("Invalid email format.");
                        else System.out.println("Email already in use.");
                        newEmail = promptOptional("Email ("+p.getEmail()+"): ");
                        if (newEmail.isEmpty()) break;
                    }
                    if (!newEmail.isEmpty()) p.setEmail(newEmail);
                }

                String newGame = promptOptional("Game ("+p.getGame()+"): ");
                if (!newGame.isEmpty()) p.setGame(newGame);

                String newRole = promptOptional("Role ("+p.getRole()+"): ");
                if (!newRole.isEmpty()) p.setRole(newRole);

                String skillIn = promptOptional("Skill ("+p.getSkillRating()+"): ");
                if (!skillIn.isEmpty()) {
                    int s = promptParseInt(skillIn, p.getSkillRating());
                    if (ValidationUtil.isValidSkill(s)) p.setSkillRating(s);
                }

                System.out.println("Updated: " + p);
                return;
            }
        }
        System.out.println("Participant not found.");
    }

    private static void submitPreferences(List<Participant> participants) {
        System.out.print("Enter your ID: ");
        String id = sc.nextLine().trim();
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id)) {
                System.out.println("Current preferences: Game=" + p.getGame() + " Role=" + p.getRole());
                String game = chooseFromList(GAMES, "Game");
                String role = chooseFromList(ROLES, "Role");
                p.setGame(game);
                p.setRole(role);
                System.out.println("Preferences updated.");
                return;
            }
        }
        System.out.println("Participant not found.");
    }

    // ---------------- Organizer menu ----------------
    private static void organizerMenu(List<Participant> participants) {
        while (true) {
            System.out.println("\n===== ORGANIZER MENU =====");
            System.out.println("1. Upload CSV");
            System.out.println("2. Validate Data");
            System.out.println("3. Run Team Formation");
            System.out.println("4. View All Teams");
            System.out.println("5. Export to CSV");
            System.out.println("6. Dashboard");
            System.out.println("7. Back");
            System.out.print("> ");
            String opt = sc.nextLine().trim();
            switch (opt) {
                case "1": uploadCsv(participants); break;
                case "2": validateData(participants); break;
                case "3": runTeamFormation(participants); break;
                case "4": viewAllTeams(); break;
                case "5": exportTeams(); break;
                case "6": dashboard(participants); break;
                case "7": return;
                default: System.out.println("Invalid option."); break;
            }
        }
    }

    // Stored teams after formation
    private static List<Team> lastFormedTeams = new ArrayList<>();
    private static int lastTeamSize = 0;
    private static int lastGameCap = 2;

    private static void uploadCsv(List<Participant> participants) {
        System.out.print("Enter CSV path: ");
        String path = sc.nextLine().trim();
        try {
            List<Participant> loaded = CSVHandler.readParticipants(Paths.get(path));
            int added = 0;
            for (Participant p : loaded) {
                if (ValidationUtil.idExists(participants, p.getId()) || ValidationUtil.emailExists(participants, p.getEmail())) {
                    System.out.println("Skipping duplicate: " + p.getId() + " / " + p.getEmail());
                } else {
                    participants.add(p);
                    added++;
                }
            }
            System.out.println("CSV import finished. Added " + added + " participants.");
        } catch (Exception e) {
            System.err.println("CSV import failed: " + e.getMessage());
        }
    }

    private static void validateData(List<Participant> participants) {
        System.out.println("\n--- Validation Report ---");
        boolean ok = true;
        Set<String> ids = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (Participant p : participants) {
            if (p.getId() == null || p.getId().isEmpty()) {
                System.out.println("Missing ID for: " + p.getName());
                ok = false;
            } else if (ids.contains(p.getId())) {
                System.out.println("Duplicate ID: " + p.getId());
                ok = false;
            } else ids.add(p.getId());

            if (!ValidationUtil.isValidEmail(p.getEmail())) {
                System.out.println("Invalid email for " + p.getId() + ": " + p.getEmail());
                ok = false;
            } else if (emails.contains(p.getEmail())) {
                System.out.println("Duplicate email: " + p.getEmail());
                ok = false;
            } else emails.add(p.getEmail());

            if (!ValidationUtil.isValidSkill(p.getSkillRating())) {
                System.out.println("Invalid skill for " + p.getId() + ": " + p.getSkillRating());
                ok = false;
            }

            if (p.getPersonalityScore() < 0 || p.getPersonalityScore() > 100) {
                System.out.println("Invalid personality score for " + p.getId() + ": " + p.getPersonalityScore());
                ok = false;
            }
        }
        System.out.println("Validation " + (ok ? "passed." : "failed. Please fix issues."));
    }

    private static void runTeamFormation(List<Participant> participants) {
        if (participants.isEmpty()) {
            System.out.println("No participants to form teams.");
            return;
        }
        System.out.print("Enter team size N (default 5): ");
        String ts = sc.nextLine().trim();
        int teamSize = ts.isEmpty() ? 5 : parseIntOr(ts, 5);
        System.out.print("Enter max same-game-per-team (cap, default 2): ");
        String capS = sc.nextLine().trim();
        int cap = capS.isEmpty() ? 2 : parseIntOr(capS,2);

        lastTeamSize = teamSize;
        lastGameCap = cap;

        ExecutorService exec = Executors.newSingleThreadExecutor();
        final List<Participant> snapshot = new ArrayList<>(participants); // effectively final for lambda
        Future<List<Team>> fut = exec.submit(() -> {
            TeamBuilder builder = new TeamBuilder(snapshot, teamSize, cap);
            return builder.buildTeams();
        });

        try {
            lastFormedTeams = fut.get();
            System.out.println("Teams formed successfully. " + lastFormedTeams.size() + " teams.");
            for (Team t : lastFormedTeams) System.out.println(t);
        } catch (Exception e) {
            System.err.println("Team formation failed: " + e.getMessage());
        } finally {
            exec.shutdown();
        }
    }

    private static void viewAllTeams() {
        if (lastFormedTeams == null || lastFormedTeams.isEmpty()) {
            System.out.println("No teams formed yet.");
            return;
        }
        for (Team t : lastFormedTeams) System.out.println(t);
    }

    private static void exportTeams() {
        if (lastFormedTeams == null || lastFormedTeams.isEmpty()) {
            System.out.println("No teams to export.");
            return;
        }
        System.out.print("Output CSV path (default formed_teams.csv): ");
        String out = sc.nextLine().trim();
        if (out.isEmpty()) out = "formed_teams.csv";
        try {
            CSVHandler.writeTeams(Paths.get(out), lastFormedTeams);
            System.out.println("Exported to " + out);
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    private static void dashboard(List<Participant> participants) {
        System.out.println("\n--- Dashboard ---");
        System.out.println("Participants: " + participants.size());
        Map<String, Long> byGame = new HashMap<>();
        Map<String, Long> byPersonality = new HashMap<>();
        for (Participant p : participants) {
            byGame.put(p.getGame(), byGame.getOrDefault(p.getGame(),0L)+1);
            byPersonality.put(p.getPersonalityType(), byPersonality.getOrDefault(p.getPersonalityType(),0L)+1);
        }
        System.out.println("By Game: " + byGame);
        System.out.println("By Personality: " + byPersonality);
        System.out.println("Last formed teams: " + (lastFormedTeams==null?0:lastFormedTeams.size()));
    }

    // ---------------- Helpers ----------------
    private static String promptNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Cannot be empty.");
        }
    }

    private static String promptOptional(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static int promptIntRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (Exception ignored) {}
            System.out.println("Enter integer between " + min + " and " + max + ".");
        }
    }

    private static String chooseFromList(List<String> opts, String label) {
        System.out.println("Options: " + String.join(", ", opts));
        System.out.print(label + ": ");
        String sel = sc.nextLine().trim();
        if (opts.stream().anyMatch(o -> o.equalsIgnoreCase(sel))) return opts.stream().filter(o -> o.equalsIgnoreCase(sel)).findFirst().get();
        System.out.println("Not found in list. Accepting entered value.");
        return sel;
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static int promptParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
