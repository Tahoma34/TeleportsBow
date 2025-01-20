package org.teleport.tahoma;

import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class TeleportBow extends JavaPlugin implements Listener {

    private final Map<UUID, Sound> playerSounds = new HashMap<>();
    private final Map<UUID, Particle> playerEffects = new HashMap<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new SoundSelectorGUI(this), this);

        PluginCommand sbowCommand = getCommand("sbow");
        if (sbowCommand != null) {
            sbowCommand.setExecutor(new TeleportBowCommands(this));
        } else {
            getLogger().severe("Команда `sbow` не зарегистрирована! Проверьте файл plugin.yml.");
        }

        getLogger().info("TeleportBow включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TeleportBow отключён.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
    }

    public void setSoundForPlayer(Player player, Sound sound) {
        playerSounds.put(player.getUniqueId(), sound);
    }

    public void setEffectForPlayer(Player player, Particle effect) {
        playerEffects.put(player.getUniqueId(), effect);
    }

    public Sound getSoundForPlayer(Player player) {
        return playerSounds.getOrDefault(player.getUniqueId(), Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    public Particle getEffectForPlayer(Player player) {
        return playerEffects.getOrDefault(player.getUniqueId(), Particle.PORTAL);
    }

    public String getSafeConfigMessage(String path, String defaultMessage) {
        return ChatColor.translateAlternateColorCodes('&', config.getString(path, defaultMessage));
    }

    @EventHandler
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack bowItem = event.getBow();

        if (!isTeleportBow(bowItem)) return;

        String worldName = player.getWorld().getName();
        if (config.getStringList("settings.blacklist_worlds").contains(worldName)) {
            player.sendMessage(getSafeConfigMessage("messages.world_blacklisted", "&c✖ Лук не работает в этом мире!"));
            event.setCancelled(true);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long cooldown = config.getLong("settings.cooldown", 5) * 1000;
        if (playerCooldowns.containsKey(player.getUniqueId())) {
            long lastUse = playerCooldowns.get(player.getUniqueId());
            if (currentTime - lastUse < cooldown) {
                long remaining = (cooldown - (currentTime - lastUse)) / 1000;
                player.sendMessage(ChatColor.RED + "Лук на перезарядке! Подождите " + remaining + " секунд.");
                event.setCancelled(true);
                return;
            }
        }
        playerCooldowns.put(player.getUniqueId(), currentTime);

        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            Particle arrowEffect = getEffectForPlayer(player);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead()
                            || arrow.isOnGround()
                            || arrow.getLocation().getBlock().getType() != Material.AIR) {
                        this.cancel();
                        return;
                    }
                    arrow.getWorld().spawnParticle(arrowEffect, arrow.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                }
            }.runTaskTimer(this, 0L, 1L);
        }
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        if (!isTeleportBow(player.getInventory().getItemInMainHand())) return;

        Location hitLocation = event.getEntity().getLocation();
        if (hitLocation.getWorld() == null) {
            getLogger().warning("Не удалось выполнить телепортацию: мир не найден.");
            return;
        }

        int delaySeconds = config.getInt("settings.delay", 0);
        Sound sound = getSoundForPlayer(player);
        Particle effect = getEffectForPlayer(player);

        if (delaySeconds > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    teleportPlayerWithEffects(player, hitLocation, sound, effect);
                }
            }.runTaskLater(this, delaySeconds * 20L);
        } else {
            teleportPlayerWithEffects(player, hitLocation, sound, effect);
        }
    }

    private void teleportPlayerWithEffects(Player player, Location location, Sound sound, Particle effect) {
        player.teleport(location);
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, sound, 1.0f, 1.0f);
            world.spawnParticle(effect, location, 50);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (config.getBoolean("settings.give_on_join", false)) {
            givePlayerBow(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeBowAndArrow(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        drops.removeIf(item -> isTeleportBow(item) || isPluginArrow(item));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isTeleportBow(item) || isPluginArrow(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getSafeConfigMessage("messages.cannot_drop_bow", "&c✖ Вы не можете выбросить этот предмет!"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || (!isTeleportBow(current) && !isPluginArrow(current))) {
            return;
        }

        if (event.getClickedInventory() != null
                && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(
                    getSafeConfigMessage("messages.cannot_store_bow",
                            "&c✖ Вы не можете переложить этот предмет!")
            );
        }
    }

    @SuppressWarnings("unused")
    public void giveOnlyBow(Player player) {
        if (playerHasBow(player)) {
            player.sendMessage(getSafeConfigMessage("messages.already_have_bow", "&c✖ У вас уже есть Мега-Лук!"));
            return;
        }

        ItemStack bow = createTeleportBow();
        if (player.getInventory().firstEmpty() > -1) {
            player.getInventory().addItem(bow);
            player.sendMessage(getSafeConfigMessage("messages.bow_given", "&a✔ Вы получили Мега-Лук (без стрел)!"));
        } else {
            player.sendMessage(getSafeConfigMessage("messages.no_inventory_space", "&c✖ Ваш инвентарь полон!"));
        }
    }

    public boolean isTeleportBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String configName = ChatColor.translateAlternateColorCodes(
                '&', config.getString("bow.name", "§6Мега-Лук"));
        return configName.equals(meta.getDisplayName());
    }

    public boolean isPluginArrow(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return ChatColor.translateAlternateColorCodes('&', "&6Мега-Стрела")
                .equals(meta.getDisplayName());
    }

    public void givePlayerBow(Player player) {
        if (playerHasBow(player)) return;
        ItemStack bow = createTeleportBow();
        ItemStack arrow = createPluginArrow();
        if (player.getInventory().firstEmpty() > -1) {
            player.getInventory().addItem(bow, arrow);
            player.sendMessage(getSafeConfigMessage("messages.bow_given", "&a✔ Вы получили Мега-Лук!"));
        } else {
            player.sendMessage(getSafeConfigMessage("messages.no_inventory_space", "&c✖ Ваш инвентарь полон!"));
        }
    }

    private ItemStack createTeleportBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes(
                    '&', config.getString("bow.name", "§6Мега-Лук")));
            meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            meta.setUnbreakable(true);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    public ItemStack createPluginArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Мега-Стрела"));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    public boolean playerHasBow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTeleportBow(item)) return true;
        }
        return false;
    }

    public boolean removeBowAndArrow(Player player) {
        boolean removed = false;
        for (ItemStack item : player.getInventory()) {
            if (isTeleportBow(item) || isPluginArrow(item)) {
                player.getInventory().remove(item);
                removed = true;
            }
        }
        return removed;
    }
}