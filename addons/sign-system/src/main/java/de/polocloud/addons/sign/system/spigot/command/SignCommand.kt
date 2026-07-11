package de.polocloud.addons.sign.system.spigot.command

import de.polocloud.addons.sign.system.SignSystem
import de.polocloud.addons.sign.system.SignType
import de.polocloud.addons.sign.system.spigot.BukkitSignPlatform
import de.polocloud.api.Polocloud
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SignCommand(val platform : BukkitSignPlatform) : CommandExecutor {


    override fun onCommand(
        sender: CommandSender,
        p1: Command,
        p2: String,
        args: Array<out String?>
    ): Boolean {

        val player = sender as Player

        if(args.size == 2) {
            if(args[0].equals("add")) {

                val group = args[1]!!
                val targetBlockExact = player.getTargetBlockExact(7)

                if(Polocloud.groupService.find(group) == null) {
                    player.sendMessage("§cThe group ${group} does not exist!")
                    return false;
                }

                if(targetBlockExact == null) {
                    player.sendMessage("§cYou are not looking at a block!")
                    return true;
                }

                if(platform.listSignTypes().contains(targetBlockExact.type.toString())) {
                    SignSystem.attachSign(SignType.SIGN, targetBlockExact.x, targetBlockExact.y, targetBlockExact.z, targetBlockExact.world.name, group)
                    player.sendMessage("§aSuccessfully added sign for group ${group}!")
                } else {
                    player.sendMessage("§cYou are not looking at a sign!")
                }
            }
        }

        return true;
    }
}