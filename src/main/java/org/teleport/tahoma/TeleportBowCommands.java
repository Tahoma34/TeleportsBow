package org.teleport.tahoma;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportBowCommands implements CommandExecutor {

    private final TeleportBow plugin;

    public TeleportBowCommands(TeleportBow plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getSafeConfigMessage("messages.not_player", "&c✖ Эта команда доступна только игрокам!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("use")) {
            if (!player.hasPermission("teleportbow.use")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав на получение лука!"));
                return true;
            }
            giveTeleportBow(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!player.hasPermission("teleportbow.use")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            removeTeleportBowAndArrow(player);
            return true;
        }

        else if (args[0].equalsIgnoreCase("removeplayer")) {
            if (!player.hasPermission("teleportbow.admin")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_target", "&c✖ Укажите имя игрока!"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.player_not_found", "&c✖ Игрок не найден или оффлайн!"));
                return true;
            }
            boolean removed = plugin.removeBowAndArrow(target);
            if (removed) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.bow_removed_from_player",
                        "&a✔ Мега-Лук убран у " + target.getName() + "!"));
                target.sendMessage(plugin.getSafeConfigMessage("messages.bow_removed",
                        "&a✔ У вас забрали Мега-Лук!"));
            } else {
                player.sendMessage(plugin.getSafeConfigMessage("messages.player_no_bow",
                        "&c✖ У игрока нет Мега-Лука!"));
            }
            return true;
        }

        else if (args[0].equalsIgnoreCase("gui")) {
            if (!player.hasPermission("teleportbow.use")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            new SoundSelectorGUI(plugin).openGUI(player);
            return true;
        }

        else if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("teleportbow.admin")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            plugin.reloadPluginConfig();
            player.sendMessage(plugin.getSafeConfigMessage("messages.config_reloaded", "&a✔ Конфигурация перезагружена!"));
            return true;
        }

        else if (args[0].equalsIgnoreCase("givebow")) {
            if (!player.hasPermission("teleportbow.admin")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_target", "&c✖ Укажите имя игрока!"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.player_not_found", "&c✖ Игрок не найден или оффлайн!"));
                return true;
            }
            plugin.givePlayerBow(target);
            player.sendMessage(plugin.getSafeConfigMessage("messages.bow_given_to_player",
                    "&a✔ Вы выдали Мега-Лук игроку " + target.getName() + "!"));
            target.sendMessage(plugin.getSafeConfigMessage("messages.bow_given",
                    "&a✔ Вам выдали Мега-Лук!"));
            return true;
        }

        else if (args[0].equalsIgnoreCase("givearrow")) {
            if (!player.hasPermission("teleportbow.admin")) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_permission", "&c✖ У вас нет прав!"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.no_target", "&c✖ Укажите имя игрока!"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getSafeConfigMessage("messages.player_not_found", "&c✖ Игрок не найден или оффлайн!"));
                return true;
            }
            target.getInventory().addItem(plugin.createPluginArrow());
            player.sendMessage(plugin.getSafeConfigMessage("messages.arrow_given_to_player",
                    "&a✔ Вы выдали Мега-Стрелу игроку " + target.getName() + "!"));
            target.sendMessage(plugin.getSafeConfigMessage("messages.arrow_received",
                    "&a✔ Вам выдали Мега-Стрелу!"));
            return true;
        }

        showHelp(player);
        return true;
    }

    private void giveTeleportBow(Player player) {
        if (plugin.playerHasBow(player)) {
            player.sendMessage(plugin.getSafeConfigMessage("messages.already_have_bow", "&c✖ У вас уже есть Мега-Лук!"));
            return;
        }
        plugin.givePlayerBow(player);
    }

    private void removeTeleportBowAndArrow(Player player) {
        boolean removed = plugin.removeBowAndArrow(player);
        if (removed) {
            player.sendMessage(plugin.getSafeConfigMessage("messages.bow_removed", "&a✔ Мега-Лук убран!"));
        } else {
            player.sendMessage(plugin.getSafeConfigMessage("messages.no_bow_to_remove", "&c✖ У вас нет Мега-Лука!"));
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_header", "&6----- TeleportBow Help -----"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_help", "&a/sbow &7- Показать справку"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_use", "&a/sbow use &7- Взять Мега-Лук и стрелу (требуется право)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_remove", "&a/sbow remove &7- Убрать Мега-Лук (требуется право)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_removeplayer", "&a/sbow removeplayer <игрок> &7- Забрать Мега-Лук (админ)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_gui", "&a/sbow gui &7- Открыть меню звуков и эффектов (требуется право)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_reload", "&a/sbow reload &7- Перезагрузить конфигурацию (админ)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_givebow", "&a/sbow givebow <игрок> &7- Выдать Мега-Лук (админ)"));
        player.sendMessage(plugin.getSafeConfigMessage("messages.help_sbow_givearrow", "&a/sbow givearrow <игрок> &7- Выдать стрелу (админ)"));
    }
}