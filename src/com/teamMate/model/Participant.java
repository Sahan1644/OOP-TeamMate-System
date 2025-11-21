package com.teamMate.model;

/**
 * Mutable Participant so we can edit details in the console UI.
 */
public class Participant {
    private String id;
    private String name;
    private String email;
    private String game;
    private String role;
    private int skillRating;        // 1-10
    private int personalityScore;   // 0-100
    private String personalityType; // Leader / Balanced / Thinker / Unknown

    public Participant(String id, String name, String email,
                       String game, String role, int skillRating,
                       int personalityScore, String personalityType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.game = game;
        this.role = role;
        this.skillRating = skillRating;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }

    // Getters & setters (needed for edit)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getSkillRating() { return skillRating; }
    public void setSkillRating(int skillRating) { this.skillRating = skillRating; }

    public int getPersonalityScore() { return personalityScore; }
    public void setPersonalityScore(int personalityScore) { this.personalityScore = personalityScore; }

    public String getPersonalityType() { return personalityType; }
    public void setPersonalityType(String personalityType) { this.personalityType = personalityType; }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | Game:%s Role:%s Skill:%d Personality:%s(%d)",
                id, name, email, game, role, skillRating, personalityType, personalityScore);
    }
}
