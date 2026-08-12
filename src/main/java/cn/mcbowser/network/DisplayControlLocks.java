package cn.mcbowser.network;

import cn.mcbowser.McBowser;
import cn.mcbowser.block.entity.DisplayPanelBlockEntity;
import cn.mcbowser.screen.DisplayStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = McBowser.MOD_ID)
public final class DisplayControlLocks {
    private static final long TIMEOUT_TICKS = 120;
    private static final Map<Key, Lock> LOCKS = new ConcurrentHashMap<>();

    private DisplayControlLocks() {}

    public static void handle(DisplayActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ServerLevel level = player.level();
        DisplayStructure structure = DisplayStructure.find(level, payload.origin());
        if (structure == null || !structure.origin().equals(payload.origin())
                || player.distanceToSqr(structure.origin().getCenter()) > 256.0) return;
        BlockEntity raw = level.getBlockEntity(structure.origin());
        if (!(raw instanceof DisplayPanelBlockEntity panel)) return;
        Key key = new Key(level.dimension().identifier(), structure.origin());

        switch (payload.action()) {
            case DisplayActionPayload.TOGGLE_DISPLAY -> panel.setDisplayEnabled(!panel.isDisplayEnabled());
            case DisplayActionPayload.REQUEST_CONTROL -> request(player, key);
            case DisplayActionPayload.RELEASE_CONTROL -> release(player, key);
            case DisplayActionPayload.CONTROL_HEARTBEAT -> heartbeat(player, key);
            case DisplayActionPayload.UPDATE_URL -> {
                if (owns(player, key) && validUrl(payload.value())) panel.setBrowserUrl(payload.value());
            }
            case DisplayActionPayload.PLAY_COMPATIBLE_MEDIA -> {
                if (owns(player, key) && validUrl(payload.value())) {
                    panel.setCompatibleMedia(payload.value(), level.getServer().overworld().getGameTime());
                }
            }
            default -> { }
        }
    }

    private static void request(ServerPlayer player, Key key) {
        long now = player.level().getServer().overworld().getGameTime();
        Lock current = LOCKS.get(key);
        if (current == null || now - current.lastHeartbeat() > TIMEOUT_TICKS || current.owner().equals(player.getUUID())) {
            LOCKS.put(key, new Lock(player.getUUID(), player.getGameProfile().name(), now));
            PacketDistributor.sendToPlayer(player, new ControlResultPayload(key.origin(), true, player.getGameProfile().name()));
        } else {
            PacketDistributor.sendToPlayer(player, new ControlResultPayload(key.origin(), false, current.ownerName()));
        }
    }

    private static void release(ServerPlayer player, Key key) {
        LOCKS.computeIfPresent(key, (ignored, lock) -> lock.owner().equals(player.getUUID()) ? null : lock);
    }

    private static void heartbeat(ServerPlayer player, Key key) {
        long now = player.level().getServer().overworld().getGameTime();
        LOCKS.computeIfPresent(key, (ignored, lock) -> lock.owner().equals(player.getUUID())
                ? new Lock(lock.owner(), lock.ownerName(), now) : lock);
    }

    private static boolean owns(ServerPlayer player, Key key) {
        Lock lock = LOCKS.get(key);
        return lock != null && lock.owner().equals(player.getUUID());
    }

    private static boolean validUrl(String value) {
        if (value == null || value.length() > 2048) return false;
        try {
            URI uri = URI.create(value);
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        long now = event.getServer().overworld().getGameTime();
        LOCKS.entrySet().removeIf(entry -> now - entry.getValue().lastHeartbeat() > TIMEOUT_TICKS);
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        LOCKS.entrySet().removeIf(entry -> entry.getValue().owner().equals(playerId));
    }

    private record Key(Identifier dimension, BlockPos origin) {}
    private record Lock(UUID owner, String ownerName, long lastHeartbeat) {}
}
