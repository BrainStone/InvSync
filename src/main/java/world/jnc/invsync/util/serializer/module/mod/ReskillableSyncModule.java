package world.jnc.invsync.util.serializer.module.mod;

import codersafterdark.reskillable.api.data.PlayerData;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import world.jnc.invsync.InventorySync;
import world.jnc.invsync.util.serializer.CapabilitySerializer;
import world.jnc.invsync.util.serializer.NativeInventorySerializer;

public class ReskillableSyncModule extends BaseModSyncModule {
  @Override
  public String getModId() {
    return "reskillable";
  }

  @Override
  public DataView serialize(Player player, DataView container) {
    return Helper.serialize(player, container);
  }

  @Override
  public void deserialize(Player player, DataView container) {
    Helper.deserialize(player, container);
  }

  @UtilityClass
  private static class Helper {
    private static DataView serialize(Player player, DataView container) {
      PlayerData playerData = new PlayerData(NativeInventorySerializer.getNativePlayer(player));
      NBTTagCompound skills = new NBTTagCompound();

      playerData.saveToNBT(skills);
      container.set(THIS, CapabilitySerializer.nbtToData(skills));

      return container;
    }

    private static void deserialize(Player player, DataView container) {
      PlayerData playerData = new PlayerData(NativeInventorySerializer.getNativePlayer(player));
      Optional<NBTTagCompound> skills =
          container
              .get(THIS)
              .map(CapabilitySerializer::dataToNbt)
              .filter(NBTTagCompound.class::isInstance)
              .map(NBTTagCompound.class::cast);

      if (skills.isPresent()) {
        playerData.loadFromNBT(skills.get());
        playerData.saveAndSync();

        Task.builder()
            .delay(100, TimeUnit.MILLISECONDS)
            .execute(
                () -> {
                  if (player.isOnline())
                    (new PlayerData(NativeInventorySerializer.getNativePlayer(player))).sync();
                })
            .submit(InventorySync.getInstance());
      }

      if (getDebug()) {
        getLogger().info("\t\tisPresent:");
        getLogger().info("\t\t\tnbt:\t" + skills.isPresent());
      }
    }
  }
}
