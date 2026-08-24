package nl.semmmetje.maces;

import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

public final class MythicMacesPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
 private NamespacedKey typeKey; private final Map<UUID,Long> cooldowns=new HashMap<>();
 @Override public void onEnable(){typeKey=new NamespacedKey(this,"mythic_mace");getServer().getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("mace")).setExecutor(this);getCommand("mace").setTabCompleter(this);}
 private MaceType type(ItemStack item){if(item==null||item.getType()!=Material.MACE||!item.hasItemMeta())return null;String raw=item.getItemMeta().getPersistentDataContainer().get(typeKey,PersistentDataType.STRING);try{return raw==null?null:MaceType.valueOf(raw);}catch(IllegalArgumentException e){return null;}}
 private ItemStack item(MaceType t){ItemStack i=new ItemStack(Material.MACE);ItemMeta m=i.getItemMeta();m.setDisplayName(t.color+t.title);m.setLore(List.of(ChatColor.GRAY+t.description,ChatColor.DARK_GRAY+"Unique mythic mace",ChatColor.YELLOW+"Right-click: inspect"));m.getPersistentDataContainer().set(typeKey,PersistentDataType.STRING,t.name());i.setItemMeta(m);return i;}
 @EventHandler public void hit(EntityDamageByEntityEvent e){if(!(e.getDamager() instanceof Player p)||!(e.getEntity() instanceof LivingEntity victim))return;MaceType t=type(p.getInventory().getItemInMainHand());if(t==null)return;long k=p.getUniqueId().getMostSignificantBits()^t.ordinal(),now=System.currentTimeMillis();if(cooldowns.getOrDefault(k,0L)>now){p.sendActionBar(ChatColor.RED+"Mace recharging...");return;}cooldowns.put(k,now+t.cooldown);effect(t,p,victim,e);}
 private void effect(MaceType t,Player p,LivingEntity v,EntityDamageByEntityEvent e){Location l=v.getLocation().add(0,1,0);World w=v.getWorld();switch(t){
 case FROSTBIND->{v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,50,8));v.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,50,200));w.spawnParticle(Particle.SNOWFLAKE,l,45,.7,.8,.7,.03);p.sendActionBar(ChatColor.AQUA+"Frozen — free second hit!");}
 case THUNDERCLAP->{w.strikeLightningEffect(l);for(Entity x:w.getNearbyEntities(l,4,3,4))if(x instanceof LivingEntity q&&q!=p)q.damage(4,p);w.spawnParticle(Particle.ELECTRIC_SPARK,l,70,1,1,1,.15);}
 case VOIDPULL->{for(Entity x:w.getNearbyEntities(l,6,4,6))if(x instanceof LivingEntity q&&q!=p){Vector a=l.toVector().subtract(q.getLocation().toVector()).normalize().multiply(.9);a.setY(.25);q.setVelocity(a);}w.spawnParticle(Particle.PORTAL,l,100,2,1,2,.4);}
 case INFERNO->{v.setFireTicks(120);w.spawnParticle(Particle.FLAME,l,80,.7,1,.7,.08);w.playSound(l,Sound.ITEM_FIRECHARGE_USE,1,.7f);}
 case VENOM->{v.addPotionEffect(new PotionEffect(PotionEffectType.POISON,100,1));w.spawnParticle(Particle.ITEM_SLIME,l,50,.7,.7,.7,.2);}
 case GALE->{Vector a=v.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.7);a.setY(.7);v.setVelocity(a);w.spawnParticle(Particle.CLOUD,l,70,.6,.6,.6,.12);}
 case GRAVITY->{v.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,35,2));w.spawnParticle(Particle.REVERSE_PORTAL,l,55,.6,1,.6,.1);}
 case ECHO->{e.setDamage(e.getDamage()*1.35);w.spawnParticle(Particle.SONIC_BOOM,l,2,.2,.2,.2,0);w.playSound(l,Sound.ENTITY_WARDEN_SONIC_BOOM,1,1.6f);}
 case BLOODRUSH->{p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+4));w.spawnParticle(Particle.DAMAGE_INDICATOR,l,45,.5,.7,.5,.1);}
 case SHATTER->{v.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,100,1));w.spawnParticle(Particle.BLOCK, l,55,.6,.7,.6,Material.DEEPSLATE.createBlockData());}
 case SHADOWSTEP->{Location behind=v.getLocation().subtract(v.getLocation().getDirection().normalize().multiply(1.4));behind.setYaw(v.getLocation().getYaw()+180);p.teleport(behind);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,50,1));w.spawnParticle(Particle.SMOKE,l,70,.6,.8,.6,.08);}
 case TIDEBREAKER->{v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,60,3));w.spawnParticle(Particle.SPLASH,l,90,1,1,1,.15);if(v.isInWater())e.setDamage(e.getDamage()*1.65);}
 case EMBERCHAIN->{for(Entity x:w.getNearbyEntities(l,5,3,5))if(x instanceof LivingEntity q&&q!=v&&q!=p){q.setFireTicks(80);q.damage(2,p);w.spawnParticle(Particle.FLAME,q.getLocation().add(0,1,0),16,.4,.5,.4,.04);}}
 case CRYSTAL->{v.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,160,0));w.spawnParticle(Particle.END_ROD,l,70,.6,.9,.6,.04);}
 case RIFT->{Location to=l.clone().add(p.getLocation().getDirection().normalize().multiply(5));v.teleport(to);w.spawnParticle(Particle.PORTAL,l,80,1,1,1,.25);w.spawnParticle(Particle.PORTAL,to,80,1,1,1,.25);}
 case VAMPIRE->{double drain=Math.min(5,Math.max(1,e.getFinalDamage()*.45));p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+drain));w.spawnParticle(Particle.HEART,p.getLocation().add(0,1,0),8,.4,.4,.4,.02);}
 case STASIS->{v.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,35,255));v.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,35,200));w.spawnParticle(Particle.ENCHANT,l,80,.8,1,.8,.3);}
 case NOVA->{for(Entity x:w.getNearbyEntities(l,4,3,4))if(x instanceof LivingEntity q&&q!=p)q.damage(3,p);w.spawnParticle(Particle.EXPLOSION,l,2,.1,.1,.1,0);w.playSound(l,Sound.ENTITY_GENERIC_EXPLODE,.7f,1.7f);}
 case LEECH->{p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,80,1));v.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,60,0));w.spawnParticle(Particle.SCULK_SOUL,l,65,.7,.8,.7,.05);}
 case CELESTIAL->{v.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,100,0));p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,100,1));w.spawnParticle(Particle.HAPPY_VILLAGER,l,80,.8,1,.8,.08);}
 }}
 @EventHandler public void inspect(org.bukkit.event.player.PlayerInteractEvent e){if(e.getAction().isRightClick()){MaceType t=type(e.getItem());if(t!=null)e.getPlayer().sendMessage(t.color+"✦ "+ChatColor.BOLD+t.title+ChatColor.GRAY+" — "+t.description);}}
 @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){if(!s.hasPermission("mythicmaces.admin")){s.sendMessage(ChatColor.RED+"No permission.");return true;}if(a.length<2||!a[0].equalsIgnoreCase("give")){s.sendMessage(ChatColor.YELLOW+"/mace give <player> <mace>");return true;}Player p=Bukkit.getPlayer(a[1]);if(p==null){s.sendMessage(ChatColor.RED+"Player not found.");return true;}try{MaceType t=MaceType.valueOf(a.length<3?"FROSTBIND":a[2].toUpperCase(Locale.ROOT));p.getInventory().addItem(item(t));s.sendMessage(ChatColor.GREEN+"Given "+t.title+" to "+p.getName());}catch(IllegalArgumentException x){s.sendMessage(ChatColor.RED+"Unknown mace. Use /mace list");}return true;}
 @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("give");if(a.length==2)return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();if(a.length==3)return Arrays.stream(MaceType.values()).map(x->x.name().toLowerCase()).toList();return List.of();}
 private enum MaceType{FROSTBIND("Frostbind",ChatColor.AQUA,"Freezes targets in place.",3500),THUNDERCLAP("Thunderclap",ChatColor.YELLOW,"Lightning shockwave.",4500),VOIDPULL("Voidpull",ChatColor.DARK_PURPLE,"Drags enemies inward.",5500),INFERNO("Inferno",ChatColor.RED,"Sets a target ablaze.",2500),VENOM("Venomspine",ChatColor.DARK_GREEN,"Poison strike.",3000),GALE("Gale Force",ChatColor.WHITE,"Launches your target.",3500),GRAVITY("Gravity Well",ChatColor.LIGHT_PURPLE,"Lifts targets helplessly.",4500),ECHO("Echo Breaker",ChatColor.BLUE,"Sonic damage burst.",5000),BLOODRUSH("Bloodrush",ChatColor.DARK_RED,"Heals its wielder.",4000),SHATTER("Shatter",ChatColor.GRAY,"Weakens cracked armour.",5000),SHADOWSTEP("Shadowstep",ChatColor.DARK_GRAY,"Teleports behind prey.",5500),TIDEBREAKER("Tidebreaker",ChatColor.AQUA,"Crushes wet enemies.",3500),EMBERCHAIN("Ember Chain",ChatColor.GOLD,"Spreads fire.",6000),CRYSTAL("Crystal Eye",ChatColor.LIGHT_PURPLE,"Marks targets.",4000),RIFT("Rift Ram",ChatColor.DARK_PURPLE,"Blinks targets forward.",5500),VAMPIRE("Vampire",ChatColor.DARK_RED,"Steals health.",4500),STASIS("Stasis",ChatColor.BLUE,"Absolute short freeze.",6500),NOVA("Nova",ChatColor.YELLOW,"Explosive area burst.",6500),LEECH("Leech",ChatColor.DARK_GREEN,"Withers and regenerates.",5000),CELESTIAL("Celestial",ChatColor.WHITE,"Radiant offence and defence.",5500);final String title,description;final ChatColor color;final long cooldown;MaceType(String t,ChatColor c,String d,long cd){title=t;color=c;description=d;cooldown=cd;}}
}