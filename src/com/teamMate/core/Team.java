package com.teamMate.core;

import com.teamMate.model.Participant;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Team {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private final int teamId;
    private final List<Participant> members = new ArrayList<>();

    public Team() {
        this.teamId = nextId.getAndIncrement();
    }

    public int getTeamId() { return teamId; }
    public List<Participant> getMembers() { return members; }
    public void addMember(Participant p) { members.add(p); }

    public double averageSkill() {
        if (members.isEmpty()) return 0.0;
        return members.stream().mapToInt(Participant::getSkillRating).average().orElse(0.0);
    }

    public Map<String, Long> roleCounts() {
        return members.stream().collect(Collectors.groupingBy(Participant::getRole, Collectors.counting()));
    }

    public Map<String, Long> gameCounts() {
        return members.stream().collect(Collectors.groupingBy(Participant::getGame, Collectors.counting()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Team ").append(teamId).append(" - AvgSkill: ").append(String.format("%.2f", averageSkill())).append("\n");
        for (Participant p : members) {
            sb.append("\t").append(p.getName()).append(" (").append(p.getId()).append(") - ")
                    .append(p.getGame()).append(", ").append(p.getRole()).append(", Skill: ")
                    .append(p.getSkillRating()).append(", ").append(p.getPersonalityType()).append("\n");
        }
        return sb.toString();
    }
}
