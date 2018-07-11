package com.djrapitops.plan.data.store.mutators;

import com.djrapitops.plan.data.container.PlayerDeath;
import com.djrapitops.plan.data.container.PlayerKill;
import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.store.containers.DataContainer;
import com.djrapitops.plan.data.store.keys.CommonKeys;
import com.djrapitops.plan.data.store.keys.SessionKeys;
import com.djrapitops.plan.data.store.mutators.formatting.Formatters;
import com.djrapitops.plan.data.time.WorldTimes;
import com.djrapitops.plugin.api.utility.log.Log;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Mutator for a list of Sessions.
 * <p>
 * Can be used to get properties of a large number of sessions easily.
 *
 * @author Rsl1122
 */
public class SessionsMutator {

    private List<Session> sessions;

    public static SessionsMutator forContainer(DataContainer container) {
        if (!container.supports(CommonKeys.SESSIONS)) {
            Log.warn(container.getClass().getSimpleName() + " does not support SESSIONS key.");
        }
        return new SessionsMutator(container.getValue(CommonKeys.SESSIONS).orElse(new ArrayList<>()));
    }

    public static SessionsMutator copyOf(SessionsMutator mutator) {
        return new SessionsMutator(new ArrayList<>(mutator.sessions));
    }

    public SessionsMutator(List<Session> sessions) {
        this.sessions = sessions;
    }

    public List<Session> all() {
        return sessions;
    }

    public SessionsMutator filterSessionsBetween(long after, long before) {
        return new SessionsMutator(sessions.stream()
                .filter(getBetweenPredicate(after, before))
                .collect(Collectors.toList()));
    }

    public WorldTimes toTotalWorldTimes() {
        WorldTimes total = new WorldTimes(new HashMap<>());

        for (Session session : sessions) {
            session.getValue(SessionKeys.WORLD_TIMES).ifPresent(total::add);
        }

        return total;
    }

    public List<PlayerKill> toPlayerKillList() {
        return sessions.stream()
                .map(session -> session.getValue(SessionKeys.PLAYER_KILLS).orElse(new ArrayList<>()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<PlayerDeath> toPlayerDeathList() {
        return sessions.stream()
                .map(session -> session.getValue(SessionKeys.PLAYER_DEATHS).orElse(new ArrayList<>()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public int toMobKillCount() {
        return sessions.stream()
                .mapToInt(session -> session.getValue(SessionKeys.MOB_KILL_COUNT).orElse(0))
                .sum();
    }

    public int toDeathCount() {
        return sessions.stream()
                .mapToInt(session -> session.getValue(SessionKeys.DEATH_COUNT).orElse(0))
                .sum();
    }

    public long toPlaytime() {
        return sessions.stream()
                .mapToLong(Session::getLength)
                .sum();
    }

    public long toAfkTime() {
        return sessions.stream()
                .mapToLong(session -> session.getValue(SessionKeys.AFK_TIME).orElse(0L))
                .sum();
    }

    public long toActivePlaytime() {
        return sessions.stream()
                .mapToLong(session -> session.getValue(SessionKeys.ACTIVE_TIME).orElse(0L))
                .sum();
    }

    public long toLastSeen() {
        return sessions.stream()
                .mapToLong(session -> Math.max(session.getUnsafe(
                        SessionKeys.START),
                        session.getValue(SessionKeys.END).orElse(System.currentTimeMillis()))
                ).max().orElse(-1);
    }

    public long toLongestSessionLength() {
        OptionalLong longestSession = sessions.stream().mapToLong(Session::getLength).max();
        if (longestSession.isPresent()) {
            return longestSession.getAsLong();
        }
        return -1;
    }

    public long toAverageSessionLength() {
        OptionalDouble average = sessions.stream().map(Session::getLength)
                .mapToLong(i -> i)
                .average();
        if (average.isPresent()) {
            return (long) average.getAsDouble();
        }
        return 0L;
    }

    public long toMedianSessionLength() {
        List<Long> sessionLengths = sessions.stream().map(Session::getLength)
                .sorted()
                .collect(Collectors.toList());
        if (sessionLengths.isEmpty()) {
            return 0;
        }
        return sessionLengths.get(sessionLengths.size() / 2);
    }

    public int toUniqueJoinsPerDay() {
        Map<Integer, Set<UUID>> uniqueJoins = new HashMap<>();
        Function<Long, Integer> function = Formatters.dayOfYear();

        for (Session session : sessions) {
            Optional<UUID> uuidValue = session.getValue(SessionKeys.UUID);
            if (!uuidValue.isPresent()) {
                continue;
            }
            UUID uuid = uuidValue.get();
            int day = function.apply(session.getUnsafe(SessionKeys.START));

            uniqueJoins.computeIfAbsent(day, computedDay -> new HashSet<>());
            uniqueJoins.get(day).add(uuid);
        }

        int total = (int) uniqueJoins.values().stream().mapToInt(Set::size).count();
        int numberOfDays = uniqueJoins.size();

        if (numberOfDays == 0) {
            return 0;
        }

        return total / numberOfDays;
    }

    public int count() {
        return sessions.size();
    }

    public int toPlayerKillCount() {
        return toPlayerKillList().size();
    }

    public boolean playedBetween(long after, long before) {
        return sessions.stream().anyMatch(getBetweenPredicate(after, before));
    }

    private Predicate<Session> getBetweenPredicate(long after, long before) {
        return session -> {
            Long start = session.getUnsafe(SessionKeys.START);
            Long end = session.getValue(SessionKeys.END).orElse(System.currentTimeMillis());
            return (after <= start && start <= before) || (after <= end && end <= before);
        };
    }

    public int toPlayerDeathCount() {
        return toPlayerDeathList().size();
    }
}