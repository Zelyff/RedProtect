/* Zelyff fork additions, GPL-3.0. Original RedProtect notices remain intact. */
package br.net.fabiozumbi12.RedProtect.Core.config.Category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class EnhancementCategory {
  @Setting(comment = "Enable the optional Zelyff interaction, border and Bedrock adapters.")
  public boolean enabled = false;

  @Setting public Tools tools = new Tools();
  @Setting public Border border = new Border();
  @Setting public Forms bedrock = new Forms();

  @ConfigSerializable
  public static class Tools {
    @Setting public boolean enabled = false;

    @Setting("claim-item")
    public String claimItem = "GOLDEN_SHOVEL";

    @Setting("inspect-item")
    public String inspectItem = "STICK";

    @Setting("two-corner-claim")
    public boolean claim = true;

    @Setting("resize-by-corner")
    public boolean resize = true;

    @Setting("inspect-nearby-on-sneak")
    public boolean nearby = true;

    @Setting("session-timeout-seconds")
    public int timeout = 90;

    @Setting("nearby-radius")
    public int nearbyRadius = 64;

    @Setting("nearby-limit")
    public int nearbyLimit = 3;

    @Setting("nearby-max-regions-checked")
    public int nearbySamples = 2048;
  }

  @ConfigSerializable
  public static class Border {
    @Setting public boolean enabled = false;

    @Setting(comment = "Exactly PARTICLE, BLOCK or HYBRID.")
    public String mode = "PARTICLE";

    @Setting("duration-seconds")
    public int duration = 12;

    @Setting("max-active-viewers")
    public int viewers = 20;

    @Setting("max-radius")
    public int radius = 96;

    @Setting("max-particles-per-second")
    public int particles = 160;

    @Setting("max-block-markers-per-viewer")
    public int markers = 32;

    @Setting("corner-material")
    public String cornerMaterial = "GLOWSTONE";

    @Setting("edge-material")
    public String edgeMaterial = "GOLD_BLOCK";

    @Setting public Glow glow = new Glow();
  }

  @ConfigSerializable
  public static class Glow {
    @Setting(comment = "Java only; through-wall appearance requires client testing.")
    public boolean enabled = false;

    @Setting public String color = "#FFFFFF";

    @Setting("corners-only")
    public boolean cornersOnly = true;

    @Setting("max-markers-per-viewer")
    public int markers = 8;
  }

  @ConfigSerializable
  public static class Forms {
    @Setting("forms-enabled")
    public boolean enabled = false;

    @Setting("page-size")
    public int pageSize = 10;

    @Setting("session-timeout-seconds")
    public int timeout = 60;
  }
}
