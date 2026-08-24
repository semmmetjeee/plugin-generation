package nl.semmmetje.lanterns;

import java.io.File;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

public final class LastLanternPlugin extends JavaPlugin implements Listener, CommandExecutor {
  private final Map<UUID, Data> players=new HashMap<>(), active=new HashMap<>();
  private NamespacedKey lanternKey; private File dataFile;
  private final String title=ChatColor.DARK_GRAY+"The Last Lantern";

  @Override public void onEnable(){
    saveDefaultConfig(); lanternKey=new NamespacedKey(this,"last_lantern"); dataFile=new File(getDataFolder(),"lanterns.yml"); load();
    getServer().getPluginManager().registerEvents(this,this); Objects.requireNonNull(getCommand("lantern")).setExecutor(this);
    getServer().getScheduler().runTaskTimer(this,this::pulse,20L,20L);
  }
  @Override public void onDisable(){ save(); }
  private Data data(UUID id){return players.computeIfAbsent(id,k->new Data());}
  private ItemStack lantern(UUID owner){
    ItemStack i=new ItemStack(Material.SOUL_LANTERN); ItemMeta m=i.getItemMeta();
    m.setDisplayName(ChatColor.LIGHT_PURPLE+"The Last Lantern");
    m.setLore(List.of(ChatColor.GRAY+"It remembers the life you live.",ChatColor.DARK_GRAY+"Right-click to awaken."));
    m.getPersistentDataContainer().set(lanternKey,PersistentDataType.STRING,owner.toString()); i.setItemMeta(m); return i;
  }
  private boolean isLantern(ItemStack i,UUID id){return i!=null&&i.hasItemMeta()&&id.toString().equals(i.getItemMeta().getPersistentDataContainer().get(lanternKey,PersistentDataType.STRING));}
  @EventHandler public void join(PlayerJoinEvent e){
    Player p=e.getPlayer(); for(ItemStack i:p.getInventory().getContents())if(isLantern(i,p.getUniqueId()))return;
    p.getInventory().addItem(lantern(p.getUniqueId())); p.sendMessage(ChatColor.LIGHT_PURPLE+"✦ A Last Lantern has appeared in your pack.");
  }
  @EventHandler(ignoreCancelled=true) public void use(PlayerInteractEvent e){
    if((e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK)&&isLantern(e.getItem(),e.getPlayer().getUniqueId())){e.setCancelled(true);open(e.getPlayer());}
  }
  @EventHandler(ignoreCancelled=true) public void mine(BlockBreakEvent e){
    Material m=e.getBlock().getType();
    if(m.name().endsWith("_ORE")||m==Material.ANCIENT_DEBRIS)add(e.getPlayer(),Memory.EARTH,m.name().contains("DIAMOND")?5:2);
    if(e.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable a&&a.getAge()==a.getMaximumAge())add(e.getPlayer(),Memory.HARVEST,2);
  }
  @EventHandler(ignoreCancelled=true) public void kill(EntityDeathEvent e){if(e.getEntity().getKiller()!=null&&e.getEntity() instanceof Monster)add(e.getEntity().getKiller(),Memory.HUNT,2);}
  @EventHandler(ignoreCancelled=true) public void fish(PlayerFishEvent e){if(e.getState()==PlayerFishEvent.State.CAUGHT_FISH)add(e.getPlayer(),Memory.TIDE,3);}
  private void add(Player p,Memory m,int amount){
    Data d=data(p.getUniqueId());d.total+=amount;d.mem.put(m,d.mem.get(m)+amount);int goal=goal(d);
    if(d.level<getConfig().getInt("max-level")&&d.total>=goal){d.total-=goal;d.level++;p.playSound(p,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.8f,1.35f);p.sendMessage(ChatColor.LIGHT_PURPLE+"✦ Your Last Lantern reached level "+d.level+".");}
  }
  private int goal(Data d){return getConfig().getInt("base-memory-goal")+(d.level-1)*getConfig().getInt("memory-goal-per-level");}
  private void open(Player p){
    Data d=data(p.getUniqueId());Inventory inv=Bukkit.createInventory(null,45,title);
    for(int i=0;i<45;i++)if(i<9||i>35||i%9==0||i%9==8)inv.setItem(i,item(Material.BLACK_STAINED_GLASS_PANE," ",List.of()));
    inv.setItem(4,item(Material.SOUL_LANTERN,ChatColor.LIGHT_PURPLE+"The Last Lantern",List.of(ChatColor.GRAY+"Level "+d.level,ChatColor.DARK_GRAY+"Memories: "+d.total+" / "+goal(d))));
    int[] memorySlots={21,22,23,24};int n=0;for(Memory m:Memory.values())inv.setItem(memorySlots[n++],item(m.icon,ChatColor.AQUA+m.label+" Memory",List.of(ChatColor.GRAY+m.detail,ChatColor.WHITE+"Stored: "+d.mem.get(m))));
    for(Ritual r:Ritual.values())inv.setItem(r.slot,item(r.icon,ChatColor.GOLD+r.label,List.of(ChatColor.GRAY+r.detail,ChatColor.DARK_GRAY+"Level "+r.level+" • "+r.cost+" "+r.memory.label,ChatColor.YELLOW+"Click to ignite")));
    p.openInventory(inv);
  }
  private ItemStack item(Material type,String name,List<String> lore){ItemStack i=new ItemStack(type);ItemMeta m=i.getItemMeta();m.setDisplayName(name);m.setLore(lore);i.setItemMeta(m);return i;}
  @EventHandler public void click(InventoryClickEvent e){
    if(!title.equals(e.getView().getTitle()))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p))return;
    for(Ritual r:Ritual.values())if(e.getRawSlot()==r.slot){ignite(p,r);return;}
  }
  @EventHandler public void drag(InventoryDragEvent e){if(title.equals(e.getView().getTitle()))e.setCancelled(true);}
  private void ignite(Player p,Ritual r){
    Data d=data(p.getUniqueId());if(active.containsKey(p.getUniqueId())){p.sendMessage(ChatColor.RED+"Your lantern is already burning a ritual.");return;}
    if(d.level<r.level){p.sendMessage(ChatColor.RED+"This ritual needs Lantern level "+r.level+".");return;}
    if(d.mem.get(r.memory)<r.cost){p.sendMessage(ChatColor.RED+"Not enough "+r.memory.label+" memories.");return;}
    d.mem.put(r.memory,d.mem.get(r.memory)-r.cost);active.put(p.getUniqueId(),new Active(r,System.currentTimeMillis()+getConfig().getLong("ritual-duration-seconds")*1000));
    p.closeInventory();p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,1f,1.4f);p.sendMessage(ChatColor.LIGHT_PURPLE+"The lantern begins "+r.label+".");
  }
  private void pulse(){
    long now=System.currentTimeMillis();int radius=getConfig().getInt("ritual-radius");
    active.entrySet().removeIf(x->{Player p=Bukkit.getPlayer(x.getKey());if(p==null||!p.isOnline()||now>=x.getValue().until){if(p!=null)p.sendMessage(ChatColor.GRAY+"Your lantern grows quiet.");return true;}run(p,x.getValue().ritual,radius);return false;});
  }
  private void run(Player p,Ritual r,int radius){
    World w=p.getWorld();Location l=p.getLocation();w.spawnParticle(Particle.WAX_ON,l.clone().add(0,1,0),8,.5,.5,.5,.02);
    if(r==Ritual.ROOTSONG)for(int i=0;i<18;i++){Block b=w.getBlockAt(l.getBlockX()+rnd(radius),l.getBlockY()+rnd(4)-2,l.getBlockZ()+rnd(radius));if(b.getBlockData() instanceof org.bukkit.block.data.Ageable a&&a.getAge()<a.getMaximumAge()){a.setAge(a.getAge()+1);b.setBlockData(a);}}
    if(r==Ritual.DEEPSIGHT)for(int i=0;i<50;i++){Block b=w.getBlockAt(l.getBlockX()+rnd(radius),l.getBlockY()+rnd(radius),l.getBlockZ()+rnd(radius));if(b.getType().name().endsWith("_ORE"))p.spawnParticle(Particle.ENCHANT,b.getLocation().add(.5,.5,.5),6,.18,.18,.18,0);}
    if(r==Ritual.MOONWARD)for(LivingEntity e:w.getLivingEntities())if(e instanceof Monster&&e.getLocation().distanceSquared(l)<radius*radius){org.bukkit.util.Vector v=e.getLocation().toVector().subtract(l.toVector());if(v.lengthSquared()>0)e.setVelocity(v.normalize().multiply(.42).setY(.14));}
    if(r==Ritual.TIDECALL&&p.isInWater()){p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE,80,0,true,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,80,0,true,false,true));}
  }
  private int rnd(int r){return java.util.concurrent.ThreadLocalRandom.current().nextInt(-r,r+1);}
  @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
    if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}
    if(a.length>0&&a[0].equalsIgnoreCase("give")){if(!p.hasPermission("lastlantern.admin")){p.sendMessage(ChatColor.RED+"No permission.");return true;}Player t=a.length>1?Bukkit.getPlayer(a[1]):p;if(t==null){p.sendMessage(ChatColor.RED+"Player not found.");return true;}t.getInventory().addItem(lantern(t.getUniqueId()));return true;}open(p);return true;
  }
  private void load(){if(!dataFile.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(dataFile);for(String k:y.getKeys(false))try{Data d=data(UUID.fromString(k));d.level=y.getInt(k+".level",1);d.total=y.getInt(k+".total");for(Memory m:Memory.values())d.mem.put(m,y.getInt(k+".memory."+m.name()));}catch(IllegalArgumentException ignored){}}
  private void save(){YamlConfiguration y=new YamlConfiguration();players.forEach((id,d)->{String k=id.toString();y.set(k+".level",d.level);y.set(k+".total",d.total);for(Memory m:Memory.values())y.set(k+".memory."+m.name(),d.mem.get(m));});try{y.save(dataFile);}catch(Exception e){getLogger().warning("Could not save lantern data.");}}
  private static final class Data{int level=1,total;EnumMap<Memory,Integer> mem=new EnumMap<>(Memory.class);Data(){for(Memory m:Memory.values())mem.put(m,0);}}
  private record Active(Ritual ritual,long until){}
  private enum Memory{EARTH("Earth",Material.MOSS_BLOCK,"Mining and exploration"),HARVEST("Harvest",Material.WHEAT,"Growing and gathering"),HUNT("Hunt",Material.BONE,"Defeating hostile creatures"),TIDE("Tide",Material.HEART_OF_THE_SEA,"Fishing and ocean life");final String label,detail;final Material icon;Memory(String l,Material i,String d){label=l;icon=i;detail=d;}}
  private enum Ritual{ROOTSONG("Rootsong",Memory.HARVEST,2,45,Material.OAK_SAPLING,"Crops mature in gentle waves.",29),DEEPSIGHT("Deepsight",Memory.EARTH,3,60,Material.DEEPSLATE,"Reveals nearby ores.",30),MOONWARD("Moonward",Memory.HUNT,4,65,Material.ENDER_EYE,"Night creatures keep away.",31),TIDECALL("Tidecall",Memory.TIDE,3,50,Material.NAUTILUS_SHELL,"Water grants strength.",32);final String label,detail;final Memory memory;final int level,cost,slot;final Material icon;Ritual(String l,Memory m,int lv,int c,Material i,String d,int s){label=l;memory=m;level=lv;cost=c;icon=i;detail=d;slot=s;}}
}