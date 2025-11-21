package com.teamMate.core;

import com.teamMate.model.Participant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds teams trying to satisfy:
 * - game diversity (cap per game)
 * - role diversity (aim at least 3 distinct roles)
 * - personality mix (1 Leader, 1-2 Thinkers, rest Balanced)
 * This is a heuristic algorithm — not exhaustive optimal.
 */
public class TeamBuilder {

    private final List<Participant> pool;
    private final int teamSize;
    private final int maxSameGamePerTeam;
    private final Random rnd = new Random();

    public TeamBuilder(List<Participant> pool, int teamSize, int maxSameGamePerTeam) {
        this.pool = new ArrayList<>(pool);
        this.teamSize = Math.max(2, teamSize);
        this.maxSameGamePerTeam = Math.max(1, maxSameGamePerTeam);
    }

    public List<Team> buildTeams() {
        Collections.shuffle(pool, rnd);
        int totalTeams = (int) Math.ceil((double) pool.size() / teamSize);
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < totalTeams; i++) teams.add(new Team());

        // split by personality
        Queue<Participant> leaders = new ArrayDeque<>();
        Queue<Participant> thinkers = new ArrayDeque<>();
        Queue<Participant> balanced = new ArrayDeque<>();
        Queue<Participant> unknown = new ArrayDeque<>();

        for (Participant p : pool) {
            switch (p.getPersonalityType()) {
                case "Leader": leaders.add(p); break;
                case "Thinker": thinkers.add(p); break;
                case "Balanced": balanced.add(p); break;
                default: unknown.add(p); break;
            }
        }

        // First pass: ensure at least one leader per team where possible
        for (Team t : teams) {
            if (!leaders.isEmpty()) {
                Participant p = leaders.poll();
                if (canAddToTeam(t, p)) t.addMember(p);
                else { /* if not allowed due to game cap, push to end */ leaders.add(p); }
            }
        }

        // Second pass: try to place thinkers and balanced to guarantee personality mix
        for (Team t : teams) {
            assignIfPossible(t, thinkers);
            assignIfPossible(t, balanced);
        }

        // Fill remaining spots with any available participants maintaining constraints
        List<Queue<Participant>> queues = Arrays.asList(leaders, thinkers, balanced, unknown);
        for (Team t : teams) {
            while (t.getMembers().size() < teamSize) {
                Participant candidate = pollAny(queues);
                if (candidate == null) break;
                if (canAddToTeam(t, candidate)) {
                    t.addMember(candidate);
                } else {
                    // try to place candidate later: push to fallback list
                    // we'll try other teams; so we skip candidate for now
                }
            }
        }

        // final pass: place leftovers to any teams that still have room
        List<Participant> leftovers = new ArrayList<>();
        for (Queue<Participant> q : queues) while (!q.isEmpty()) leftovers.add(q.poll());
        Collections.shuffle(leftovers, rnd);
        for (Participant p : leftovers) {
            for (Team t : teams) {
                if (t.getMembers().size() < teamSize && canAddToTeam(t, p)) {
                    t.addMember(p);
                    break;
                }
            }
        }

        // attempt to increase role diversity per team: try swapping if role count < 3
        enforceRoleDiversity(teams);

        return teams;
    }

    private void assignIfPossible(Team t, Queue<Participant> q) {
        if (t.getMembers().size() >= teamSize) return;
        Iterator<Participant> it = q.iterator();
        while (it.hasNext()) {
            Participant p = it.next();
            if (canAddToTeam(t, p)) {
                t.addMember(p);
                it.remove();
                return;
            }
        }
    }

    private Participant pollAny(List<Queue<Participant>> queues) {
        for (Queue<Participant> q : queues) {
            Participant p = q.poll();
            if (p != null) return p;
        }
        return null;
    }

    private boolean canAddToTeam(Team t, Participant p) {
        long gameCount = t.getMembers().stream().filter(m -> m.getGame().equalsIgnoreCase(p.getGame())).count();
        if (gameCount >= maxSameGamePerTeam) return false;
        return true;
    }

    // Improve role diversity by swapping participants between teams where possible
    private void enforceRoleDiversity(List<Team> teams) {
        for (Team t : teams) {
            Set<String> roles = t.getMembers().stream().map(Participant::getRole).collect(Collectors.toSet());
            if (roles.size() >= 3) continue;
            // try to find a participant in other teams with a role not present here and swap
            for (Team other : teams) {
                if (other == t) continue;
                for (Participant pOther : new ArrayList<>(other.getMembers())) {
                    if (!roles.contains(pOther.getRole())) {
                        // find candidate in this team to swap (one whose role exists elsewhere)
                        for (Participant pThis : new ArrayList<>(t.getMembers())) {
                            if (t.getMembers().contains(pThis) && other.getMembers().contains(pOther)) {
                                // ensure swap won't break game cap in either team
                                if (canSwap(t, other, pThis, pOther)) {
                                    t.getMembers().remove(pThis);
                                    other.getMembers().remove(pOther);
                                    t.getMembers().add(pOther);
                                    other.getMembers().add(pThis);
                                    roles = t.getMembers().stream().map(Participant::getRole).collect(Collectors.toSet());
                                    break;
                                }
                            }
                        }
                    }
                    if (roles.size() >= 3) break;
                }
                if (roles.size() >= 3) break;
            }
        }
    }

    private boolean canSwap(Team a, Team b, Participant pa, Participant pb) {
        long aGameCountAfter = a.getMembers().stream().filter(m -> !m.equals(pa) && m.getGame().equalsIgnoreCase(pb.getGame())).count() + 1;
        long bGameCountAfter = b.getMembers().stream().filter(m -> !m.equals(pb) && m.getGame().equalsIgnoreCase(pa.getGame())).count() + 1;
        return aGameCountAfter <= maxSameGamePerTeam && bGameCountAfter <= maxSameGamePerTeam;
    }
}
