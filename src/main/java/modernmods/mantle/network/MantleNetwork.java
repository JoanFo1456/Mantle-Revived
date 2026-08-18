package modernmods.mantle.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import modernmods.mantle.Mantle;
import modernmods.mantle.fluid.transfer.FluidContainerTransferPacket;
import modernmods.mantle.network.packet.DropLecternBookPacket;
import modernmods.mantle.network.packet.OpenLecternBookPacket;
import modernmods.mantle.network.packet.OpenNamedBookPacket;
import modernmods.mantle.network.packet.SwingArmPacket;
import modernmods.mantle.network.packet.UpdateHeldPagePacket;
import modernmods.mantle.network.packet.UpdateInventoryPagePacket;
import modernmods.mantle.network.packet.UpdateLecternPagePacket;
import modernmods.mantle.network.NetworkWrapper.PacketDirection;
import modernmods.mantle.recipe.sync.RecipeSyncPacket;

public class MantleNetwork {
  /**
   * Network instance
   * 1: 1.11.101 and before
   * 2: 1.11.102 - New predicate types, enum loadable nullable field optimization
   */
  public static final NetworkWrapper INSTANCE = new NetworkWrapper(Mantle.getResource("network"), "2");
  private static boolean initialized = false;

  /**
   * Registers packets into this network
   */
  public static void init() {
    if (initialized) {
      return;
    }
    initialized = true;
    INSTANCE.registerPacket(OpenLecternBookPacket.class, OpenLecternBookPacket::new, PacketDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(UpdateHeldPagePacket.class, UpdateHeldPagePacket::new, PacketDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(UpdateInventoryPagePacket.class, UpdateInventoryPagePacket::new, PacketDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(UpdateLecternPagePacket.class, UpdateLecternPagePacket::new, PacketDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(DropLecternBookPacket.class, DropLecternBookPacket::new, PacketDirection.PLAY_TO_SERVER);
    INSTANCE.registerPacket(SwingArmPacket.class, SwingArmPacket::new, PacketDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(OpenNamedBookPacket.class, OpenNamedBookPacket::new, PacketDirection.PLAY_TO_CLIENT);
    INSTANCE.registerPacket(FluidContainerTransferPacket.class, FluidContainerTransferPacket::new, PacketDirection.PLAY_TO_CLIENT);
    // appended at the end so existing packet ids are unchanged (see NetworkWrapper auto-increment ids)
    INSTANCE.registerPacket(RecipeSyncPacket.class, RecipeSyncPacket::new, PacketDirection.PLAY_TO_CLIENT);
  }

  /** Registers packets with NeoForge's payload system. */
  public static void registerPackets(RegisterPayloadHandlersEvent event) {
    init();
    INSTANCE.registerPayloads(event);
  }
}
