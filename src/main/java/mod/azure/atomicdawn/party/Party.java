package mod.azure.atomicdawn.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Party {

    private final UUID id = UUID.randomUUID();

    private final Set<UUID> members = new LinkedHashSet<>();

    final Map<UUID, String> names = new HashMap<>();

    Party(UUID leader, String name) {
        members.add(leader);
        names.put(leader, name);
    }

    public UUID id() {
        return id;
    }

    public UUID leader() {
        return members.iterator().next();
    }

    public boolean isLeader(UUID player) {
        return leader().equals(player);
    }

    public List<UUID> members() {
        return new ArrayList<>(members);
    }

    public int size() {
        return members.size();
    }

    public boolean contains(UUID player) {
        return members.contains(player);
    }

    void add(UUID player, String name) {
        members.add(player);
        names.put(player, name);
    }

    void remove(UUID player) {
        members.remove(player);
        names.remove(player);
    }

    void promote(UUID player) {
        if (!members.contains(player)) {
            return;
        }
        var rest = new ArrayList<>(members);
        rest.remove(player);
        members.clear();
        members.add(player);
        members.addAll(rest);
    }

    public String name(UUID player) {
        return names.getOrDefault(player, "?");
    }
}
