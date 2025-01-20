package org.teleport.tahoma;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

public class SoundSelectorGUI implements Listener {

    private final TeleportBow plugin;

    public SoundSelectorGUI(TeleportBow plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("sounds");
        if (soundsSection == null) {
            plugin.getLogger().warning("Не найдена секция конфигурации 'sounds' в config.yml.");
            return;
        }

        String guiTitle = plugin.getSafeConfigMessage("messages.sound_selector_title", "&0Выбор звука и эффекта");
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', guiTitle));

        Map<String, Object> soundsMap = soundsSection.getValues(false);
        for (String key : soundsMap.keySet()) {
            String name = plugin.getConfig().getString("sounds." + key + ".name", "&e▪ Эффект");
            String iconName = plugin.getConfig().getString("sounds." + key + ".icon", "NOTE_BLOCK");
            int slot = plugin.getConfig().getInt("sounds." + key + ".slot", 0);

            InventoryClickItem item = createItem(
                    ChatColor.translateAlternateColorCodes('&', name),
                    Material.valueOf(iconName)
            );
            inventory.setItem(slot, item.stack);
        }
        player.openInventory(inventory);
    }

    private InventoryClickItem createItem(String name, Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return new InventoryClickItem(stack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Выбор звука") && event.getCurrentItem() != null) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked.getItemMeta() == null) return;

            for (int i = 0; i < event.getInventory().getSize(); i++) {
                ItemStack item = event.getInventory().getItem(i);
                if (item != null && item.isSimilar(clicked)) {
                    String soundName = getSoundValueBySlot(i);
                    String effectName = getEffectValueBySlot(i);
                    if (soundName != null && effectName != null) {
                        plugin.setSoundForPlayer(player, Sound.valueOf(soundName));
                        plugin.setEffectForPlayer(player, Particle.valueOf(effectName));

                        String message = plugin.getSafeConfigMessage("messages.sound_changed", "&a✔ Вы изменили звук!");
                        message = message.replace("%sound%", soundName);
                        player.sendMessage(message);
                        player.closeInventory();
                    }
                    break;
                }
            }
        }
    }

    private String getSoundValueBySlot(int slot) {
        ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("sounds");
        if (soundsSection == null) return null;
        for (String key : soundsSection.getKeys(false)) {
            if (plugin.getConfig().getInt("sounds." + key + ".slot") == slot) {
                return plugin.getConfig().getString("sounds." + key + ".sound");
            }
        }
        return null;
    }

    private String getEffectValueBySlot(int slot) {
        ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("sounds");
        if (soundsSection == null) return null;
        for (String key : soundsSection.getKeys(false)) {
            if (plugin.getConfig().getInt("sounds." + key + ".slot") == slot) {
                return plugin.getConfig().getString("sounds." + key + ".effect");
            }
        }
        return null;
    }

    private static class InventoryClickItem {
        private final ItemStack stack;

        public InventoryClickItem(ItemStack stack) {
            this.stack = stack;
        }
    }
}