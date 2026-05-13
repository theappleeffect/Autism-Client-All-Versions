package autismclient.mixin.accessor;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Connection.class)
public interface PackUtilClientConnectionAccessor {
    @Accessor("channel")
    Channel getChannel();
}
