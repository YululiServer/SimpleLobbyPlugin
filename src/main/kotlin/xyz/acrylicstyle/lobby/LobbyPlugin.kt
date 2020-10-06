package xyz.acrylicstyle.lobby

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import util.reflect.Ref
import xyz.acrylicstyle.tomeito_api.gui.PerPlayerInventory
import xyz.acrylicstyle.tomeito_api.utils.Log

@Suppress("unused")
class LobbyPlugin : JavaPlugin(), Listener {
    companion object {
        fun setItems(player: Player) {
            if (serverSelectorPresent) {
                player.inventory.setItem(0, getItemStack(Material.COMPASS, ChatColor.GREEN.toString() + "サーバー選択"))
            }
        }

        private fun getItemStack(material: Material, displayName: String): ItemStack {
            val item = ItemStack(material)
            val meta = item.itemMeta
            meta.displayName = displayName
            item.itemMeta = meta
            return item
        }

        var serverSelectorPresent = false
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        object: BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { player ->
                    if (player.location.y < 0 || player.location.y > 255) player.teleport(player.world.spawnLocation.clone().add(0.5, 0.0, 0.5))
                }
            }
        }.runTaskTimer(this, 0, 20)
        object: BukkitRunnable() {
            override fun run() {
                try {
                    Class.forName("xyz.acrylicstyle.serverSelector.ServerSelector")
                    serverSelectorPresent = true
                    println("ServerSelector is present!")
                } catch (e: ClassNotFoundException) {
                    Log.error("Could not find ServerSelector")
                }
            }
        }.runTaskLater(this, 1)
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        e.respawnLocation = e.player.world.spawnLocation.clone().add(0.5, 0.0, 0.5)
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (e.player.gameMode != GameMode.CREATIVE) {
            e.player.gameMode = GameMode.ADVENTURE
            e.player.inventory.clear()
        }
        e.player.teleport(e.player.world.spawnLocation.clone().add(0.5, 0.0, 0.5))
        setItems(e.player)
    }

    @EventHandler
    fun onPlayerDropItem(e: PlayerDropItemEvent) {
        if (e.player.gameMode == GameMode.CREATIVE) return
        e.isCancelled = true
    }

    @EventHandler
    fun onPlayerPickupItem(e: PlayerPickupItemEvent) {
        if (e.player.gameMode == GameMode.CREATIVE) return
        e.isCancelled = true
    }

    @EventHandler
    fun onEntityDamage(e: EntityDamageEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByEntity(e: EntityDamageByEntityEvent) {
        if (e.damager is Player && (e.damager as Player).gameMode == GameMode.CREATIVE) return
        e.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByBlock(e: EntityDamageByBlockEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action.name.startsWith("RIGHT")) {
            if (serverSelectorPresent
                && e.item != null
                && e.item.itemMeta != null
                && e.item.itemMeta.displayName != null
                && e.item.itemMeta.displayName.contains(ChatColor.GREEN.toString() + "サーバー選択")) {
                val inv = Ref.forName<Any>("xyz.acrylicstyle.serverSelector.ServerSelector")
                    .getDeclaredField("gui")
                    .accessible(true)
                    .get(null) as PerPlayerInventory<*>
                e.player.openInventory((inv.get(e.player.uniqueId) as InventoryHolder).inventory)
                e.isCancelled = true
            }
        }
    }
}