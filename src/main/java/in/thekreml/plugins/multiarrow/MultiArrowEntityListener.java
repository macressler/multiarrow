package in.thekreml.plugins.multiarrow;

import java.util.List;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

/*import com.iConomy.iConomy;
import com.iConomy.system.Holdings;*/

/**
 * Listens for entity events and raises arrow effect events
 * @author ayan4m1
 */
public class MultiArrowEntityListener implements Listener {
	private MultiArrow plugin;

	public MultiArrowEntityListener(MultiArrow instance) {
		plugin = instance;
	}

	public boolean chargeFee(Player player, String arrowType) {
		return true;
		/*Double arrowFee = plugin.config.getArrowFee(type);
		if (plugin.iconomy != null && !player.hasPermission("multiarrow.free-fees") && arrowFee > 0D) {
			try {
				if (iConomy.hasAccount(player.getName())) {
					Holdings balance = iConomy.getAccount(player.getName()).getHoldings();
					if (balance.hasEnough(arrowFee)) {
						balance.subtract(arrowFee);
						if ((Boolean)plugin.config.getOptionValue("send-balance-on-fee") == true) {
							player.sendMessage("Balance is now " + iConomy.format(balance.balance()) + "");
						}
					} else {
						player.sendMessage("You need " + iConomy.format(arrowFee) + ", but only have " + iConomy.format(balance.balance()));
						return false;
					}
				} else {
					player.sendMessage("Couldn't find your iConomy holdings, cannot pay fee of " + iConomy.format(arrowFee));
					return false;
				}
			} catch (Exception e) {
				plugin.log.warning("Exception when trying to charge " + player.getName() + " " + iConomy.format(arrowFee));
			}
			return true;
		} else return true;*/
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof Arrow)) {
			return;
		}

		Arrow arrow = (Arrow)event.getEntity();
		if (!(arrow.getShooter() instanceof Player)) {
			return;
		}

		Integer arrowIndex = plugin.activeArrow.get(((Player)arrow.getShooter()).getName());
		if (arrowIndex == -1) {
			return;
		}

		//We should ignore this event if there is a targetable entity within one block
		List<Entity> entities = arrow.getNearbyEntities(1D, 1D, 1D);
		int entCount = entities.size();
		for(Entity ent : entities) {
			if ((ent instanceof Arrow) || (ent instanceof Item) || (ent == arrow.getShooter())) {
				entCount--;
			}
		}

		//Only raise the onGroundHitEvent if there are no valid entities nearby
		if (entCount == 0) {
			String arrowType = plugin.arrowTypes.getName(arrowIndex);
			if (this.chargeFee((Player)arrow.getShooter(), arrowType)) {
				ArrowEffect arrowEffect = plugin.arrowTypes.getType(arrowType);
				arrowEffect.onGroundHitEvent(arrow);

				if (plugin.config.getArrowRemove(arrowType)) {
					arrow.remove();
				}

				if (arrowEffect instanceof TimedArrowEffect) {
					TimedArrowEffect timedArrowEffect = (TimedArrowEffect)arrowEffect;
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, timedArrowEffect.getDelayTriggerRunnable(arrow), timedArrowEffect.getDelayTicks());
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.LOW)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
			return;
		}

		if (!(event instanceof EntityDamageByEntityEvent)) {
			return;
		}

		EntityDamageByEntityEvent ebe = (EntityDamageByEntityEvent)event;
		if (!(ebe.getDamager() instanceof Arrow)) {
			return;
		}

		Arrow arrow = (Arrow)ebe.getDamager();
		if (!(arrow.getShooter() instanceof Player)) {
			return;
		}

		Integer arrowIndex = plugin.activeArrow.get(((Player)arrow.getShooter()).getName());
		if (arrowIndex == -1) {
			return;
		}

		event.setCancelled(true);
		String arrowType = plugin.arrowTypes.getName(arrowIndex);
		if (this.chargeFee((Player)arrow.getShooter(), arrowType)) {
			ArrowEffect arrowEffect = plugin.arrowTypes.getType(arrowType);
			arrowEffect.onEntityHitEvent(arrow, event.getEntity());

			if (plugin.config.getArrowRemove(arrowType)) {
				arrow.remove();
			}

			if (arrowEffect instanceof TimedArrowEffect) {
				TimedArrowEffect timedArrowEffect = (TimedArrowEffect)arrowEffect;
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, timedArrowEffect.getDelayTriggerRunnable(arrow), timedArrowEffect.getDelayTicks());
			}
		}
	}
}
